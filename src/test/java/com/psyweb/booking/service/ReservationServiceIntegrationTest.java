package com.psyweb.booking.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.doReturn;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.ActiveReservationAlreadyExistsException;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

public class ReservationServiceIntegrationTest extends PostgreSQLIntegrationTest {
	@Autowired
	private Clock clock;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpecialistRepository specialistRepository;

	@Autowired
	private AvailabilitySlotRepository slotRepository;

	@MockitoSpyBean
	private ReservationRepository reservationRepository;

	@Autowired
	private ReservationService reservationService;

	@Test
	public void shouldTranslateDatabaseConflictWhenConcurrentReservationsTargetSameSlot()
			throws InterruptedException, TimeoutException {
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Smith", Duration.ZERO, Duration.ZERO));

		User client = userRepository
				.saveAndFlush(new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		doReturn(false).when(reservationRepository).existsBySlotIdAndStatus(slot.getId(), ReservationStatus.ACTIVE);

		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			List<Future<Reservation>> futures = new ArrayList<>();

			for (int i = 0; i < 2; i++) {
				Future<Reservation> future = executor
						.submit(() -> reservationService.createReservation(client.getId(), slot.getId()));
				futures.add(future);
			}

			int successfulAttempts = 0;
			int failedAttempts = 0;

			for (Future<Reservation> future : futures) {
				try {
					future.get(10, TimeUnit.SECONDS);
					successfulAttempts++;
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					ActiveReservationAlreadyExistsException conflictException = assertInstanceOf(
							ActiveReservationAlreadyExistsException.class, cause);

					assertEquals("ACTIVE_RESERVATION_ALREADY_EXISTS", conflictException.code());
					assertEquals("Slot is already reserved", conflictException.getMessage());
					assertInstanceOf(DataIntegrityViolationException.class, conflictException.getCause());
					failedAttempts++;
				}
			}
			assertEquals(1, successfulAttempts);
			assertEquals(1, failedAttempts);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	public void shouldCreateActiveReservationAndReserveSlot() {
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist-create@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Create", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-create@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		Reservation result = reservationService.createReservation(client.getId(), slot.getId());

		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals(ReservationStatus.ACTIVE, result.getStatus());
		assertEquals(AvailabilityStatus.RESERVED, savedSlot.getAvailabilityStatus());
	}

	@Test
	public void shouldCancelReservationAndReleaseSlot() {
		LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist-cancel@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Cancel", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-cancel@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(2);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));

		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, now, now.plusMinutes(5)));

		reservationService.cancelReservation(reservation.getId(), now, CancellationInitiator.CLIENT,
				CancellationReason.CLIENT_REQUEST);

		Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals(ReservationStatus.CANCELLED, result.getStatus());
		assertEquals(AvailabilityStatus.FREE, savedSlot.getAvailabilityStatus());
		assertEquals(ReservationStatus.CANCELLED, result.getStatus());
		assertEquals(now, result.getCancelledAt());
		assertEquals(CancellationInitiator.CLIENT, result.getCancellationInitiator());
		assertEquals(CancellationReason.CLIENT_REQUEST, result.getCancellationReason());
	}

	@Test
	public void shouldExpireReservationAndReleaseSlot() {
		LocalDateTime now = LocalDateTime.now(clock);
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist-expire@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Expire", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-expire@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(3);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));

		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));

		reservationRepository.saveAndFlush(reservation);

		reservationService.expireReservation(reservation.getId());

		Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals(ReservationStatus.EXPIRED, result.getStatus());
		assertEquals(AvailabilityStatus.FREE, savedSlot.getAvailabilityStatus());
	}

	@Test
	public void shouldRollbackReservationCancellationWhenSlotReleaseFails() {
		LocalDateTime now = LocalDateTime.now(clock);
		User specialistUser = userRepository.saveAndFlush(
				new User("specialist-rollback@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "Rollback", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-rollback@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(4);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, now, now.plusMinutes(5)));

		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> reservationService.cancelReservation(reservation.getId(), now, CancellationInitiator.CLIENT,
						CancellationReason.CLIENT_REQUEST));

		Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals("Cannot release reservation from slot with status FREE", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, result.getStatus());
		assertEquals(AvailabilityStatus.FREE, savedSlot.getAvailabilityStatus());
		assertNull(result.getCancelledAt());
		assertNull(result.getCancellationInitiator());
		assertNull(result.getCancellationReason());
	}
}
