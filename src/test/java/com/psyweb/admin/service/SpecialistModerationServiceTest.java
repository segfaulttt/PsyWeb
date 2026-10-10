package com.psyweb.admin.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.booking.service.SchedulingService;
import com.psyweb.specialist.service.SpecialistService;

@ExtendWith(MockitoExtension.class)
class SpecialistModerationServiceTest {

	private static final Long SPECIALIST_ID = 1L;

	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));

	@Mock
	private SpecialistService specialistService;

	@Mock
	private SchedulingService schedulingService;

	private SpecialistModerationService moderationService;

	@BeforeEach
	void setUp() {
		moderationService = new SpecialistModerationService(specialistService, schedulingService, clock);
	}

	@Test
	void shouldSuspendSpecialistAndCancelFutureSlots() {
		Instant suspendedAt = clock.instant();

		moderationService.suspendSpecialist(SPECIALIST_ID);

		InOrder inOrder = inOrder(specialistService, schedulingService);

		inOrder.verify(specialistService).suspendSpecialist(SPECIALIST_ID);

		inOrder.verify(schedulingService).cancelFutureSlotsForSpecialistSuspension(SPECIALIST_ID, suspendedAt);
	}

	@Test
	void shouldReinstateSpecialistWithoutRestoringCancelledScheduling() {
		moderationService.reinstateSpecialist(SPECIALIST_ID);

		verify(specialistService).reinstateSpecialist(SPECIALIST_ID);

		verifyNoInteractions(schedulingService);
	}
}