package com.psyweb.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.booking.exception.ReservationExpiredException;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

public class BookingServiceIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private Clock clock;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SpecialistRepository specialistRepository;

	@Autowired
	private AvailabilitySlotRepository slotRepository;

	@Autowired
	private ReservationRepository reservationRepository;

	@MockitoSpyBean
	private BookingRepository bookingRepository;

	@Autowired
	private BookingService bookingService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	public void shouldConfirmReservationAndCreateBookingAtomically() {
		LocalDateTime now = LocalDateTime.now(clock);
		User specialistUser = userRepository.saveAndFlush(new User("specialist-confirm-success@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Success", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
				new User("client-confirm-success@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, now, now.plusMinutes(10)));

		Booking result = bookingService.confirmReservation(reservation.getId(), client.getId());

		Reservation savedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();
		Booking savedBooking = bookingRepository.findById(result.getId()).orElseThrow();

		assertEquals(result.getId(), savedBooking.getId());

		assertEquals(ReservationStatus.CONFIRMED, savedReservation.getStatus());
		assertEquals(AvailabilityStatus.BOOKED, savedSlot.getAvailabilityStatus());
		assertEquals(BookingStatus.CONFIRMED, savedBooking.getStatus());

		assertEquals(savedReservation.getId(), savedBooking.getReservationId());
		assertEquals(savedSlot.getId(), savedBooking.getSlotId());
		assertEquals(client.getId(), savedBooking.getClientId());
		assertEquals(specialist.getId(), savedBooking.getSpecialistId());
	}

	@Test
	public void shouldRollbackConfirmationWhenBookingCreationFails() {
		LocalDateTime now = LocalDateTime.now(clock);
		User specialistUser = userRepository.saveAndFlush(new User("specialist-confirm-rollback@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Rollback", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
				new User("client-confirm-rollback@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(2);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, now, now.plusMinutes(10)));

		long bookingsBefore = bookingRepository.count();

		doThrow(new DataIntegrityViolationException("Booking save failed")).when(bookingRepository)
				.save(any(Booking.class));

		assertThrows(DataIntegrityViolationException.class,
				() -> bookingService.confirmReservation(reservation.getId(), client.getId()));

		Reservation savedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals(ReservationStatus.ACTIVE, savedReservation.getStatus());
		assertEquals(AvailabilityStatus.RESERVED, savedSlot.getAvailabilityStatus());
		assertEquals(bookingsBefore, bookingRepository.count());
	}

	@Test
	public void shouldPersistExpirationAndReleaseSlotWhenConfirmationIsRejected() {
		LocalDateTime now = LocalDateTime.now(clock);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-expired-confirmation@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Expired", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(new User("client-expired-confirmation@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = now.plusDays(1);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));

		reservationRepository.saveAndFlush(reservation);

		long bookingsBefore = bookingRepository.count();

		ReservationExpiredException exception = assertThrows(ReservationExpiredException.class,
				() -> bookingService.confirmReservation(reservation.getId(), client.getId()));

		Reservation savedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
		AvailabilitySlot savedSlot = slotRepository.findById(slot.getId()).orElseThrow();

		assertEquals("RESERVATION_EXPIRED", exception.code());
		assertEquals("Reservation already expired", exception.getMessage());

		assertEquals(ReservationStatus.EXPIRED, savedReservation.getStatus());
		assertEquals(AvailabilityStatus.FREE, savedSlot.getAvailabilityStatus());
		assertEquals(bookingsBefore, bookingRepository.count());
	}

	private static void awaitLatch(CountDownLatch latch) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError("Latch timeout");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	@Test
	void shouldRejectConfirmationWhenExpirationWinsRace() throws Exception {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-expiration-race@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Ann", "Race", Duration.ZERO, Duration.ZERO);

		specialist.approve();
		specialist = specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
				new User("client-expiration-race@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime slotStart = now.plusDays(1);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, slotStart, slotStart.plusHours(1));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository
				.saveAndFlush(new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10)));

		Long reservationId = reservation.getId();
		Long slotId = slot.getId();
		Long clientId = client.getId();

		long bookingsBefore = bookingRepository.count();

		CountDownLatch expirationHasLock = new CountDownLatch(1);
		CountDownLatch confirmationStarted = new CountDownLatch(1);
		CountDownLatch allowExpirationCommit = new CountDownLatch(1);

		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<Void> expirationFuture = executor.submit(() -> {
				TransactionTemplate transaction = new TransactionTemplate(transactionManager);

				transaction.executeWithoutResult(status -> {
					Reservation lockedReservation = reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 1)
							.getFirst();

					expirationHasLock.countDown();
					awaitLatch(allowExpirationCommit);

					lockedReservation.expire(now);

					AvailabilitySlot lockedSlot = slotRepository.findById(slotId).orElseThrow();

					lockedSlot.releaseReservation();
				});

				return null;
			});

			assertTrue(expirationHasLock.await(5, TimeUnit.SECONDS));

			Future<Throwable> confirmationFuture = executor.submit(() -> {
				confirmationStarted.countDown();

				try {
					bookingService.confirmReservation(reservationId, clientId);
					return null;
				} catch (Throwable throwable) {
					return throwable;
				}
			});

			assertTrue(confirmationStarted.await(5, TimeUnit.SECONDS));

			allowExpirationCommit.countDown();

			expirationFuture.get(5, TimeUnit.SECONDS);
			Throwable confirmationFailure = confirmationFuture.get(5, TimeUnit.SECONDS);

			assertNotNull(confirmationFailure);
			assertInstanceOf(InvalidReservationStateException.class, confirmationFailure);

			Reservation actualReservation = reservationRepository.findById(reservationId).orElseThrow();

			AvailabilitySlot actualSlot = slotRepository.findById(slotId).orElseThrow();

			assertEquals(ReservationStatus.EXPIRED, actualReservation.getStatus());

			assertEquals(AvailabilityStatus.FREE, actualSlot.getAvailabilityStatus());

			assertEquals(bookingsBefore, bookingRepository.count());
		} finally {
			allowExpirationCommit.countDown();
			executor.shutdownNow();
		}
	}
}
