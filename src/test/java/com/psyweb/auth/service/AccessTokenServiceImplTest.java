
package com.psyweb.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.psyweb.auth.config.JwtConfiguration;
import com.psyweb.auth.config.JwtKeyPair;
import com.psyweb.auth.config.JwtProperties;
import com.psyweb.auth.security.AccessTokenClaimsValidator;
import com.psyweb.auth.service.impl.AccessTokenServiceImpl;
import com.psyweb.user.domain.UserRole;

import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AccessTokenServiceImplTest {

	private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

	private static final Duration TTL = Duration.ofMinutes(15);
	private static final String ISSUER = "psyweb";
	private static final String AUDIENCE = "psyweb-api";
	private static final String KID = "test-rsa-key";

	private static KeyPair keyPair;

	private Clock clock;
	private JwtProperties properties;
	private JwtEncoder encoder;
	private JwtDecoder decoder;
	private AccessTokenService service;

	@BeforeAll
	static void generateTestKeys() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(3072);
		keyPair = generator.generateKeyPair();
	}

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(NOW, ZoneOffset.UTC);

		properties = new JwtProperties(Path.of("unused-private.pem"), Path.of("unused-public.pem"), KID, ISSUER,
				AUDIENCE, TTL);

		JwtConfiguration configuration = new JwtConfiguration(properties, clock);

		JwtKeyPair rsaKeys = new JwtKeyPair((RSAPrivateKey) keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());

		encoder = configuration.jwtEncoder(rsaKeys);

		AccessTokenClaimsValidator claimsValidator = new AccessTokenClaimsValidator(clock, properties);

		decoder = configuration.jwtDecoder(rsaKeys, claimsValidator);

		service = new AccessTokenServiceImpl(properties, clock, encoder, decoder);
	}

	@Test
	void shouldGenerateAndVerifyClientAccessToken() {
		String token = service.generateAccessToken(42L, UserRole.CLIENT);

		assertNotNull(token);
		assertEquals(3, token.split("\\.").length);

		AuthenticatedUser actual = service.verifyAccessToken(token);

		assertEquals(42L, actual.userId());
		assertEquals(UserRole.CLIENT, actual.role());
	}

	@Test
	void shouldGenerateAndVerifyAllUserRoles() {
		for (UserRole role : UserRole.values()) {
			String token = service.generateAccessToken(100L, role);

			AuthenticatedUser actual = service.verifyAccessToken(token);

			assertEquals(100L, actual.userId());
			assertEquals(role, actual.role());
		}
	}

	@Test
	void shouldGenerateCorrectClaimsAndHeaders() {
		String token = service.generateAccessToken(42L, UserRole.SPECIALIST);

		Jwt jwt = decoder.decode(token);

		assertEquals(ISSUER, jwt.getClaimAsString("iss"));
		assertEquals("42", jwt.getSubject());
		assertEquals(List.of(AUDIENCE), jwt.getAudience());
		assertEquals("SPECIALIST", jwt.getClaimAsString("role"));
		assertEquals(NOW, jwt.getIssuedAt());
		assertEquals(NOW.plus(TTL), jwt.getExpiresAt());
		assertEquals(KID, jwt.getHeaders().get("kid"));
		assertEquals("RS256", jwt.getHeaders().get("alg"));
	}

	@Test
	void shouldRejectInvalidGenerationArguments() {
		assertThrows(IllegalArgumentException.class, () -> service.generateAccessToken(null, UserRole.CLIENT));

		assertThrows(IllegalArgumentException.class, () -> service.generateAccessToken(0L, UserRole.CLIENT));

		assertThrows(IllegalArgumentException.class, () -> service.generateAccessToken(-1L, UserRole.CLIENT));

		assertThrows(IllegalArgumentException.class, () -> service.generateAccessToken(1L, null));
	}

	@Test
	void shouldRejectBlankToken() {
		assertThrows(BadJwtException.class, () -> service.verifyAccessToken(null));

		assertThrows(BadJwtException.class, () -> service.verifyAccessToken(""));

		assertThrows(BadJwtException.class, () -> service.verifyAccessToken("   "));
	}

	@Test
	void shouldRejectMalformedToken() {
		assertThrows(JwtException.class, () -> service.verifyAccessToken("not-a-jwt"));
	}

	@Test
	void shouldRejectExpiredToken() {
		JwtClaimsSet claims = validClaims("42", "CLIENT", ISSUER, AUDIENCE, NOW.minus(Duration.ofMinutes(20)),
				NOW.minus(Duration.ofMinutes(5)));

		String token = sign(claims, KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectTamperedPayload() {
		String original = service.generateAccessToken(42L, UserRole.CLIENT);

		String[] parts = original.split("\\.");

		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

		assertTrue(payload.contains("\"CLIENT\""));

		String modifiedPayload = payload.replace("\"CLIENT\"", "\"ADMIN\"");

		parts[1] = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(modifiedPayload.getBytes(StandardCharsets.UTF_8));

		String tampered = String.join(".", parts);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(tampered));
	}

	@Test
	void shouldRejectSignatureFromAnotherKey() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(3072);
		KeyPair otherKeyPair = generator.generateKeyPair();

		JwtEncoder otherEncoder = createEncoder(otherKeyPair, KID);

		String token = signWithEncoder(otherEncoder, validClaims("42", "CLIENT", ISSUER, AUDIENCE, NOW, NOW.plus(TTL)),
				KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectWrongIssuer() {
		String token = sign(validClaims("42", "CLIENT", "other-service", AUDIENCE, NOW, NOW.plus(TTL)), KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectWrongAudience() {
		String token = sign(validClaims("42", "CLIENT", ISSUER, "other-api", NOW, NOW.plus(TTL)), KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectInvalidSubject() {
		for (String subject : List.of("abc", "0", "-10")) {
			String token = sign(validClaims(subject, "CLIENT", ISSUER, AUDIENCE, NOW, NOW.plus(TTL)), KID);

			assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
		}
	}

	@Test
	void shouldRejectMissingSubject() {
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).audience(List.of(AUDIENCE)).claim("role", "CLIENT")
				.issuedAt(NOW).expiresAt(NOW.plus(TTL)).build();

		assertThrows(JwtException.class, () -> service.verifyAccessToken(sign(claims, KID)));
	}

	@Test
	void shouldRejectInvalidRole() {
		String token = sign(validClaims("42", "SUPER_ADMIN", ISSUER, AUDIENCE, NOW, NOW.plus(TTL)), KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectMissingRole() {
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject("42").audience(List.of(AUDIENCE))
				.issuedAt(NOW).expiresAt(NOW.plus(TTL)).build();

		assertThrows(JwtException.class, () -> service.verifyAccessToken(sign(claims, KID)));
	}

	@Test
	void shouldRejectMissingIssuedAt() {
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject("42").audience(List.of(AUDIENCE))
				.claim("role", "CLIENT").expiresAt(NOW.plus(TTL)).build();

		assertThrows(JwtException.class, () -> service.verifyAccessToken(sign(claims, KID)));
	}

	@Test
	void shouldRejectMissingExpiresAt() {
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject("42").audience(List.of(AUDIENCE))
				.claim("role", "CLIENT").issuedAt(NOW).build();

		assertThrows(JwtException.class, () -> service.verifyAccessToken(sign(claims, KID)));
	}

	@Test
	void shouldRejectExcessiveTokenLifetime() {
		String token = sign(validClaims("42", "CLIENT", ISSUER, AUDIENCE, NOW, NOW.plus(Duration.ofMinutes(16))), KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectIssuedAtTooFarInFuture() {
		String token = sign(validClaims("42", "CLIENT", ISSUER, AUDIENCE, NOW.plus(Duration.ofMinutes(2)),
				NOW.plus(Duration.ofMinutes(17))), KID);

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectWrongKeyId() {
		JwtClaimsSet claims = validClaims("42", "CLIENT", ISSUER, AUDIENCE, NOW, NOW.plus(TTL));

		JwtEncoder otherKidEncoder = createEncoder(keyPair, "other-key");

		String token = signWithEncoder(otherKidEncoder, claims, "other-key");

		assertThrows(JwtException.class, () -> service.verifyAccessToken(token));
	}

	@Test
	void shouldRejectMissingKeyId() throws Exception {
		SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
				new JWTClaimsSet.Builder().issuer(ISSUER).subject("42").audience(AUDIENCE).claim("role", "CLIENT")
						.issueTime(java.util.Date.from(NOW)).expirationTime(java.util.Date.from(NOW.plus(TTL)))
						.build());

		signedJwt.sign(new com.nimbusds.jose.crypto.RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));

		assertThrows(JwtException.class, () -> service.verifyAccessToken(signedJwt.serialize()));
	}

	@Test
	void shouldRejectUnsupportedAlgorithm() throws Exception {
		byte[] secret = new byte[32];
		java.util.Arrays.fill(secret, (byte) 7);

		SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).build(),
				new JWTClaimsSet.Builder().issuer(ISSUER).subject("42").audience(AUDIENCE).claim("role", "CLIENT")
						.build());

		signedJwt.sign(new MACSigner(secret));

		assertThrows(JwtException.class, () -> service.verifyAccessToken(signedJwt.serialize()));
	}

	private JwtClaimsSet validClaims(String subject, String role, String issuer, String audience, Instant issuedAt,
			Instant expiresAt) {
		return JwtClaimsSet.builder().issuer(issuer).subject(subject).audience(List.of(audience)).claim("role", role)
				.issuedAt(issuedAt).expiresAt(expiresAt).build();
	}

	private String sign(JwtClaimsSet claims, String kid) {
		return signWithEncoder(encoder, claims, kid);
	}

	private String signWithEncoder(JwtEncoder selectedEncoder, JwtClaimsSet claims, String kid) {
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(kid).build();

		return selectedEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private JwtEncoder createEncoder(KeyPair pair, String kid) {
		RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
				.privateKey((RSAPrivateKey) pair.getPrivate()).keyID(kid).algorithm(JWSAlgorithm.RS256).build();

		JWKSet jwkSet = new JWKSet(List.of(rsaKey));

		ImmutableJWKSet<SecurityContext> source = new ImmutableJWKSet<>(jwkSet);

		return new NimbusJwtEncoder(source);
	}
}