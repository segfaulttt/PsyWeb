package com.psyweb.auth.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.List;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.psyweb.auth.security.AccessTokenClaimsValidator;

import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {

	private final JwtProperties jwtProperties;
	private final Clock clock;

	public JwtConfiguration(JwtProperties jwtProperties, Clock clock) {
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	@Bean
	public JwtKeyPair jwtKeyPair() {
		RSAPrivateKey privateKey = loadPrivateKey();
		RSAPublicKey publicKey = loadPublicKey();
		validateKeys(privateKey, publicKey);
		return new JwtKeyPair(privateKey, publicKey);
	}

	@Bean
	public JwtEncoder jwtEncoder(JwtKeyPair keyPair) {
		RSAKey key = new RSAKey.Builder(keyPair.publicKey()).privateKey(keyPair.privateKey())
				.keyID(jwtProperties.keyId()).algorithm(JWSAlgorithm.RS256).build();
		JWKSet jwkSet = new JWKSet(List.of(key));
		ImmutableJWKSet<SecurityContext> immutableJwkSet = new ImmutableJWKSet<>(jwkSet);
		NimbusJwtEncoder encoder = new NimbusJwtEncoder(immutableJwkSet);
		return encoder;
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtKeyPair keyPair, AccessTokenClaimsValidator validatorClaims) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyPair.publicKey())
				.signatureAlgorithm(SignatureAlgorithm.RS256).build();
		JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));
		timestampValidator.setClock(clock);
		
		JwtIssuerValidator issuerValidator = new JwtIssuerValidator(jwtProperties.issuer());
		OAuth2TokenValidator<Jwt> validatorAud = jwt -> {
			if (jwt.getAudience().contains(jwtProperties.audience())) {
				return OAuth2TokenValidatorResult.success();
			}

			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid JWT audience", null);
			return OAuth2TokenValidatorResult.failure(error);
		};
		DelegatingOAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
		        timestampValidator,
		        issuerValidator,
		        validatorAud,
		        validatorClaims
		);
		decoder.setJwtValidator(validators);
		return decoder;
	}

	@Bean
	public AccessTokenClaimsValidator accessTokenClaimsValidator() {
		return new AccessTokenClaimsValidator(clock, jwtProperties);
	}

	public void validateKeys(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
		if (!privateKey.getModulus().equals(publicKey.getModulus())) {
			throw new IllegalStateException("RSA key pair mismatch");
		}

		try {
			String message = "psyweb-key-validation";
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(privateKey);
			signature.update(message.getBytes(StandardCharsets.UTF_8));

			byte[] signedBytes = signature.sign();

			Signature verifier = Signature.getInstance("SHA256withRSA");
			verifier.initVerify(publicKey);
			verifier.update(message.getBytes(StandardCharsets.UTF_8));

			if (!verifier.verify(signedBytes)) {
				throw new IllegalStateException("Invalid keys pair");
			}
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to validate RSA key pair", e);
		}
	}

	public RSAPrivateKey loadPrivateKey() {
		try {
			Path path = jwtProperties.privateKeyPath();
			String content = Files.readString(path);

			if (!content.contains("-----BEGIN PRIVATE KEY-----") || !content.contains("-----END PRIVATE KEY-----")) {
				throw new IllegalStateException("Invalid private key PEM format");
			}

			String base64Key = content.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "").replaceAll("\\s+", "");

			byte[] decodedBytes = Base64.getDecoder().decode(base64Key);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedBytes);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

			if (privateKey instanceof RSAPrivateKey rsaKey) {
				if (rsaKey.getModulus().bitLength() == 3072) {
					return rsaKey;
				}
			}
			throw new IllegalStateException("Failed to load JWT private key");
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load JWT private key", e);
		}
	}

	public RSAPublicKey loadPublicKey() {
		try {
			Path path = jwtProperties.publicKeyPath();
			String content = Files.readString(path);

			if (!content.contains("-----BEGIN PUBLIC KEY-----") || !content.contains("-----END PUBLIC KEY-----")) {
				throw new IllegalStateException("Invalid public key PEM format");
			}

			String base64Key = content.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
					.replaceAll("\\s+", "");

			byte[] decodedBytes = Base64.getDecoder().decode(base64Key);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedBytes);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey key = keyFactory.generatePublic(keySpec);

			if (key instanceof RSAPublicKey rsaKey) {
				if (rsaKey.getModulus().bitLength() == 3072) {
					return rsaKey;
				}
			}
			throw new IllegalStateException("Failed to load JWT public key");
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load JWT public key", e);
		}
	}
}