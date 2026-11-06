package com.psyweb.schedule.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.booking.service.BookingObligationsQueryService;
import com.psyweb.booking.service.model.BookingObligation;
import com.psyweb.booking.service.model.ObligationType;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.schedule.service.model.TimeZoneChangePreview;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
class SpecialistTimeZoneIntegrationTest extends PostgreSQLIntegrationTest {

	private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

	private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

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

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private SpecialistTimeZoneGuardRepository guardRepository;

	@Autowired
	private BookingObligationsQueryService obligationsQueryService;

	@Autowired
	private SpecialistTimeZoneService timeZoneService;

	@PersistenceContext
	private EntityManager entityManager;

	private Specialist createSpecialist() {
		User user = userRepository.saveAndFlush(new User("specialist-" + UUID.randomUUID() + "@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(user, "Anna", "Test", Duration.ZERO, Duration.ZERO);

		specialist.approve();

		return specialistRepository.saveAndFlush(specialist);
	}

	private User createClient() {
		return userRepository.saveAndFlush(new User("client-" + UUID.randomUUID() + "@example.com", "password-hash",
				UserRole.CLIENT, UserStatus.ACTIVE));
	}

	private SpecialistTimeZoneGuard createGuard(Specialist specialist, ZoneId zone) {
		return guardRepository
				.saveAndFlush(new SpecialistTimeZoneGuard(specialist, zone, clock.instant().minusSeconds(3600)));
	}

	private Reservation createActiveReservation(Specialist specialist, User client, Instant start, Instant createdAt,
			Instant expiresAt) {
		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)));

		slot.reserve();

		slot = slotRepository.saveAndFlush(slot);

		return reservationRepository.saveAndFlush(new Reservation(client, slot, createdAt, expiresAt));
	}

	private Booking createConfirmedBooking(Specialist specialist, User client, Instant start) {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Instant createdAt = start.isBefore(now) ? start.minus(Duration.ofHours(2)) : now.minus(Duration.ofMinutes(1));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, start, start.plus(Duration.ofHours(1)));

		slot.reserve();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, createdAt.minus(Duration.ofMinutes(3)),
				createdAt.plus(Duration.ofMinutes(10)));

		reservation.confirm(createdAt.minusSeconds(10));
		reservation = reservationRepository.saveAndFlush(reservation);

		slot.confirmBooking();
		slotRepository.saveAndFlush(slot);

		return bookingRepository.saveAndFlush(new Booking(client, specialist, slot, reservation, createdAt));
	}

	@Test
	void shouldFindOnlyFutureConfirmedBookings() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Specialist target = createSpecialist();
		Specialist other = createSpecialist();
		User client = createClient();

		Booking expected = createConfirmedBooking(target, client, now.plus(Duration.ofDays(2)));

		createConfirmedBooking(target, client, now.minus(Duration.ofDays(1)));

		Booking cancelled = createConfirmedBooking(target, client, now.plus(Duration.ofDays(3)));

		cancelled.cancel(now, CancellationInitiator.CLIENT, CancellationReason.CLIENT_REQUEST);

		bookingRepository.saveAndFlush(cancelled);

		createConfirmedBooking(other, client, now.plus(Duration.ofDays(4)));

		entityManager.flush();
		entityManager.clear();

		List<BookingObligation> result = obligationsQueryService.getFutureConfirmedBookings(target.getId(), now);

		assertEquals(1, result.size());

		BookingObligation obligation = result.get(0);

		assertEquals(expected.getId(), obligation.id());
		assertEquals(ObligationType.BOOKING, obligation.type());
		assertEquals(expected.getStartTime(), obligation.startTime());
		assertEquals(expected.getEndTime(), obligation.endTime());
	}

	@Test
	void shouldFindOnlyActiveNonExpiredFutureReservations() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Specialist target = createSpecialist();
		Specialist other = createSpecialist();
		User client = createClient();

		Reservation expected = createActiveReservation(target, client, now.plus(Duration.ofDays(2)),
				now.minusSeconds(60), now.plusSeconds(300));

		createActiveReservation(target, client, now.plus(Duration.ofDays(3)), now.minusSeconds(3600),
				now.minusSeconds(60));

		createActiveReservation(target, client, now.minus(Duration.ofDays(1)), now.minusSeconds(60),
				now.plusSeconds(300));

		Reservation confirmed = createActiveReservation(target, client, now.plus(Duration.ofDays(4)),
				now.minusSeconds(60), now.plusSeconds(300));

		confirmed.confirm(now);
		reservationRepository.saveAndFlush(confirmed);

		createActiveReservation(other, client, now.plus(Duration.ofDays(5)), now.minusSeconds(60),
				now.plusSeconds(300));

		entityManager.flush();
		entityManager.clear();

		List<BookingObligation> result = obligationsQueryService.getActiveFutureReservations(target.getId(), now);

		assertEquals(1, result.size());

		BookingObligation obligation = result.get(0);

		assertEquals(expected.getId(), obligation.id());
		assertEquals(ObligationType.RESERVATION, obligation.type());
		assertEquals(expected.getStartTime(), obligation.startTime());
		assertEquals(expected.getEndTime(), obligation.endTime());
	}

	@Test
	void shouldPreviewAffectedBookingsAndReservations() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Specialist specialist = createSpecialist();
		User client = createClient();

		createGuard(specialist, MOSCOW);

		createConfirmedBooking(specialist, client, now.plus(Duration.ofDays(2)));

		createActiveReservation(specialist, client, now.plus(Duration.ofDays(3)), now.minusSeconds(60),
				now.plusSeconds(300));

		entityManager.flush();
		entityManager.clear();

		TimeZoneChangePreview preview = timeZoneService.previewTimeZoneChange(specialist.getId(), AMSTERDAM);

		assertEquals(MOSCOW, preview.currentZoneId());
		assertEquals(AMSTERDAM, preview.newZoneId());
		assertEquals(1, preview.affectedBookingsCount());
		assertEquals(1, preview.affectedReservationsCount());
		assertTrue(preview.requiresAcknowledgement());

		TimeZoneChangePreview sameZonePreview = timeZoneService.previewTimeZoneChange(specialist.getId(), MOSCOW);

		assertEquals(0, sameZonePreview.affectedBookingsCount());
		assertEquals(0, sameZonePreview.affectedReservationsCount());
		assertFalse(sameZonePreview.requiresAcknowledgement());
	}

	@Test
	void shouldRejectTimeZoneChangeWithoutAcknowledgement() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Specialist specialist = createSpecialist();
		User client = createClient();

		createGuard(specialist, MOSCOW);

		createConfirmedBooking(specialist, client, now.plus(Duration.ofDays(2)));

		entityManager.flush();
		entityManager.clear();

		assertThrows(IllegalStateException.class,
				() -> timeZoneService.changeTimeZone(specialist.getId(), AMSTERDAM, false));

		assertEquals(MOSCOW, timeZoneService.getTimeZone(specialist.getId()));
	}

	@Test
	void shouldPersistAcknowledgedTimeZoneChange() {
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);

		Specialist specialist = createSpecialist();
		User client = createClient();

		createGuard(specialist, MOSCOW);

		Booking booking = createConfirmedBooking(specialist, client, now.plus(Duration.ofDays(2)));

		Instant originalStart = booking.getStartTime();
		Instant originalEnd = booking.getEndTime();

		entityManager.flush();
		entityManager.clear();

		timeZoneService.changeTimeZone(specialist.getId(), AMSTERDAM, true);

		entityManager.flush();
		entityManager.clear();

		SpecialistTimeZoneGuard reloadedGuard = guardRepository.findById(specialist.getId()).orElseThrow();

		Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

		assertEquals(AMSTERDAM, reloadedGuard.getTimeZone());
		assertEquals(originalStart, reloadedBooking.getStartTime());
		assertEquals(originalEnd, reloadedBooking.getEndTime());
	}
}