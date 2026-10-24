
package com.psyweb.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtConfigurationTest {

	@TempDir
	Path tempDir;

	private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void shouldLoadValidRsa3072KeyPair() throws Exception {
		KeyPair pair = generateRsaKeyPair(3072);

		Path privatePath = tempDir.resolve("private.pem");
		Path publicPath = tempDir.resolve("public.pem");

		writePrivateKey(privatePath, pair);
		writePublicKey(publicPath, pair);

		JwtConfiguration configuration = configuration(privatePath, publicPath);

		JwtKeyPair loaded = configuration.jwtKeyPair();

		assertEquals(pair.getPrivate(), loaded.privateKey());
		assertEquals(pair.getPublic(), loaded.publicKey());
		assertEquals(3072, loaded.publicKey().getModulus().bitLength());
	}

	@Test
	void shouldRejectMismatchedRsaKeys() throws Exception {
		KeyPair firstPair = generateRsaKeyPair(3072);
		KeyPair secondPair = generateRsaKeyPair(3072);

		Path privatePath = tempDir.resolve("private.pem");
		Path publicPath = tempDir.resolve("public.pem");

		writePrivateKey(privatePath, firstPair);
		writePublicKey(publicPath, secondPair);

		JwtConfiguration configuration = configuration(privatePath, publicPath);

		IllegalStateException exception = assertThrows(IllegalStateException.class, configuration::jwtKeyPair);

		assertEquals("RSA key pair mismatch", exception.getMessage());
	}

	@Test
	void shouldRejectRsaKeysWithInsufficientLength() throws Exception {
		KeyPair pair = generateRsaKeyPair(2048);

		Path privatePath = tempDir.resolve("private.pem");
		Path publicPath = tempDir.resolve("public.pem");

		writePrivateKey(privatePath, pair);
		writePublicKey(publicPath, pair);

		JwtConfiguration configuration = configuration(privatePath, publicPath);

		assertThrows(IllegalStateException.class, configuration::jwtKeyPair);
	}

	@Test
	void shouldRejectMalformedPrivateKeyPem() throws Exception {
		KeyPair pair = generateRsaKeyPair(3072);

		Path privatePath = tempDir.resolve("private.pem");
		Path publicPath = tempDir.resolve("public.pem");

		Files.writeString(privatePath, "invalid private key", StandardCharsets.UTF_8);

		writePublicKey(publicPath, pair);

		JwtConfiguration configuration = configuration(privatePath, publicPath);

		IllegalStateException exception = assertThrows(IllegalStateException.class, configuration::jwtKeyPair);

		assertEquals("Invalid private key PEM format", exception.getMessage());
	}

	@Test
	void shouldRejectMissingPrivateKeyFile() throws Exception {
		KeyPair pair = generateRsaKeyPair(3072);

		Path privatePath = tempDir.resolve("missing-private.pem");
		Path publicPath = tempDir.resolve("public.pem");

		writePublicKey(publicPath, pair);

		JwtConfiguration configuration = configuration(privatePath, publicPath);

		IllegalStateException exception = assertThrows(IllegalStateException.class, configuration::jwtKeyPair);

		assertEquals("Failed to load JWT private key", exception.getMessage());
		assertNotNull(exception.getCause());
	}

	private JwtConfiguration configuration(Path privatePath, Path publicPath) {
		JwtProperties properties = new JwtProperties(privatePath, publicPath, "test-rsa-key", "psyweb", "psyweb-api",
				Duration.ofMinutes(15));

		return new JwtConfiguration(properties, clock);
	}

	private KeyPair generateRsaKeyPair(int bits) throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(bits);
		return generator.generateKeyPair();
	}

	private void writePrivateKey(Path path, KeyPair pair) throws Exception {
		writePem(path, "PRIVATE KEY", pair.getPrivate().getEncoded());
	}

	private void writePublicKey(Path path, KeyPair pair) throws Exception {
		writePem(path, "PUBLIC KEY", pair.getPublic().getEncoded());
	}

	private void writePem(Path path, String type, byte[] encoded) throws Exception {
		String base64 = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encoded);

		String pem = "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";

		Files.writeString(path, pem, StandardCharsets.UTF_8);
	}
}