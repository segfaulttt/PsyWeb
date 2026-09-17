package com.psyweb.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReservationPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ReservationConfiguration.class);

	@Test
	void shouldBindReservationProperties() {
		contextRunner.withPropertyValues("psyweb.reservation.ttl=10m", "psyweb.reservation.expiration-scan-interval=1m",
				"psyweb.reservation.expiration-batch-size=100").run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(ReservationProperties.class);

					ReservationProperties properties = context.getBean(ReservationProperties.class);

					assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(10));

					assertThat(properties.expirationScanInterval()).isEqualTo(Duration.ofMinutes(1));

					assertThat(properties.expirationBatchSize()).isEqualTo(100);
				});
	}

	@Test
	void shouldRejectConfigurationWhenTtlIsMissing() {
		contextRunner.withPropertyValues("psyweb.reservation.expiration-scan-interval=1m",
				"psyweb.reservation.expiration-batch-size=100").run(context -> assertThat(context).hasFailed());
	}

	@Test
	void shouldRejectConfigurationWhenExpirationScanIntervalIsMissing() {
		contextRunner.withPropertyValues("psyweb.reservation.ttl=10m", "psyweb.reservation.expiration-batch-size=100")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void shouldRejectConfigurationWhenExpirationBatchSizeIsZero() {
		contextRunner.withPropertyValues("psyweb.reservation.ttl=10m", "psyweb.reservation.expiration-scan-interval=1m",
				"psyweb.reservation.expiration-batch-size=0").run(context -> assertThat(context).hasFailed());
	}

}