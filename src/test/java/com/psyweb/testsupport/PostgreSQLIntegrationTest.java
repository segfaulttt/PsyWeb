
package com.psyweb.testsupport;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgreSQLIntegrationTest {

	protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("psyweb").withUsername("psyweb").withPassword("psyweb");

	private static final JwtTestKeys jwtKeys = createJwtTestKeys();

	static {
		postgres.start();
	}

	@DynamicPropertySource
	static void dbProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);

		registry.add("psyweb.auth.jwt.private-key-path", () -> jwtKeys.privateKeyPath().toString());

		registry.add("psyweb.auth.jwt.public-key-path", () -> jwtKeys.publicKeyPath().toString());
	}

	private static JwtTestKeys createJwtTestKeys() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(3072);

			KeyPair keyPair = generator.generateKeyPair();

			Path directory = Files.createTempDirectory("psyweb-jwt-test-");

			Path privateKeyPath = directory.resolve("private.pem");
			Path publicKeyPath = directory.resolve("public.pem");

			writePem(privateKeyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());

			writePem(publicKeyPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());

			privateKeyPath.toFile().deleteOnExit();
			publicKeyPath.toFile().deleteOnExit();
			directory.toFile().deleteOnExit();

			return new JwtTestKeys(privateKeyPath, publicKeyPath);

		} catch (Exception e) {
			throw new IllegalStateException("Failed to initialize JWT test keys", e);
		}
	}

	private static void writePem(Path path, String type, byte[] encoded) throws Exception {

		String base64 = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encoded);

		String pem = "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";

		Files.writeString(path, pem, StandardCharsets.UTF_8);
	}

	private record JwtTestKeys(Path privateKeyPath, Path publicKeyPath) {
	}
}
