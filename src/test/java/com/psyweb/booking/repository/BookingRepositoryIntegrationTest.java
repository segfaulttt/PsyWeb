package com.psyweb.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class BookingRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Autowired
	UserRepository userRepository;

	@Autowired
	SpecialistRepository specialistRepository;

	@Autowired
	BookingRepository bookingRepository;

	@Autowired
	AvailabilitySlotRepository slotRepository;

	@Autowired
	ReservationRepository reservationRepository;

	@Test
	public void shouldFindClientBookingsByStatus() {
		LocalDateTime now = LocalDateTime.now(clock);
		User targetClient = userRepository.saveAndFlush(
				new User("targetclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		User specialistUser = userRepository.saveAndFlush(
				new User("specialistuser@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Ann", "Jobs", Duration.ZERO, Duration.ZERO));
		LocalDateTime startTime = LocalDateTime.now().plusDays(2);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

		Reservation reservation = new Reservation(targetClient, slot, now, now.plusMinutes(5));
		reservation.confirm(now);
		reservationRepository.saveAndFlush(reservation);

		Booking targetBooking = new Booking(targetClient, specialist, slot, reservation,
				LocalDateTime.now().plusMinutes(5));
		targetBooking.cancel(startTime.plusDays(3), CancellationInitiator.CLIENT, CancellationReason.CLIENT_REQUEST);
		bookingRepository.saveAndFlush(targetBooking);

		AvailabilitySlot differentStatusSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime.plusHours(2), startTime.plusHours(3)));

		Reservation differentStatusReservation = new Reservation(targetClient, differentStatusSlot, now,
				now.plusMinutes(10));
		differentStatusReservation.confirm(now);
		reservationRepository.saveAndFlush(differentStatusReservation);

		Booking differentStatusBooking = new Booking(targetClient, specialist, differentStatusSlot,
				differentStatusReservation, LocalDateTime.now().plusMinutes(10));
		differentStatusBooking.complete();
		bookingRepository.saveAndFlush(differentStatusBooking);

		User otherClient = userRepository
				.saveAndFlush(new User("difclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		AvailabilitySlot otherClientSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime.plusHours(4), startTime.plusHours(5)));

		Reservation otherClientReservation = new Reservation(otherClient, otherClientSlot, now, now.plusMinutes(10));
		otherClientReservation.confirm(now);
		reservationRepository.saveAndFlush(otherClientReservation);

		Booking otherClientBooking = new Booking(otherClient, specialist, otherClientSlot, otherClientReservation,
				LocalDateTime.now().plusMinutes(10));
		otherClientBooking.cancel(startTime.plusDays(4), CancellationInitiator.CLIENT,
				CancellationReason.CLIENT_REQUEST);
		bookingRepository.saveAndFlush(otherClientBooking);

		List<Booking> result = bookingRepository.findByClient_IdAndStatus(targetClient.getId(),
				BookingStatus.CANCELLED);

		assertEquals(1, result.size());
		assertEquals(targetBooking.getId(), result.get(0).getId());
		assertEquals(targetClient.getId(), result.get(0).getClientId());
		assertEquals(BookingStatus.CANCELLED, result.get(0).getStatus());
	}

	@Test
	public void shouldFindSpecialistBookingsByStatus() {
		LocalDateTime now = LocalDateTime.now(clock);
		User targetClient = userRepository.saveAndFlush(
				new User("targetclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		User specialistUser = userRepository.saveAndFlush(
				new User("specialistuser@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist targetSpecialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Ann", "Jobs", Duration.ZERO, Duration.ZERO));
		LocalDateTime startTime = LocalDateTime.now().plusDays(2);

		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(targetSpecialist, startTime, startTime.plusHours(1)));

		Reservation reservation = new Reservation(targetClient, slot, now, now.plusMinutes(5));
		reservation.confirm(now);
		reservationRepository.saveAndFlush(reservation);

		Booking targetBooking = new Booking(targetClient, targetSpecialist, slot, reservation,
				LocalDateTime.now().plusMinutes(5));
		targetBooking.cancel(startTime.plusDays(3), CancellationInitiator.CLIENT, CancellationReason.CLIENT_REQUEST);
		bookingRepository.saveAndFlush(targetBooking);

		AvailabilitySlot differentStatusSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(targetSpecialist, startTime.plusHours(2), startTime.plusHours(3)));

		Reservation differentStatusReservation = new Reservation(targetClient, differentStatusSlot, now,
				now.plusMinutes(10));
		differentStatusReservation.confirm(now);
		reservationRepository.saveAndFlush(differentStatusReservation);

		Booking differentStatusBooking = new Booking(targetClient, targetSpecialist, differentStatusSlot,
				differentStatusReservation, LocalDateTime.now().plusMinutes(10));
		differentStatusBooking.complete();
		bookingRepository.saveAndFlush(differentStatusBooking);

		User otherSpecialistUser = userRepository.saveAndFlush(
				new User("otherspecialistclient@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist otherSpecialist = specialistRepository
				.saveAndFlush(new Specialist(otherSpecialistUser, "John", "Smith", Duration.ZERO, Duration.ZERO));

		AvailabilitySlot otherSpecialistSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(otherSpecialist, startTime.plusHours(4), startTime.plusHours(5)));

		Reservation otherSpecialistReservation = new Reservation(targetClient, otherSpecialistSlot, now,
				now.plusMinutes(10));
		otherSpecialistReservation.confirm(now);
		reservationRepository.saveAndFlush(otherSpecialistReservation);

		Booking otherSpecialistBooking = new Booking(targetClient, otherSpecialist, otherSpecialistSlot,
				otherSpecialistReservation, LocalDateTime.now().plusMinutes(10));
		otherSpecialistBooking.cancel(startTime.plusDays(4), CancellationInitiator.CLIENT,
				CancellationReason.CLIENT_REQUEST);
		bookingRepository.saveAndFlush(otherSpecialistBooking);

		List<Booking> result = bookingRepository.findBySpecialist_IdAndStatus(targetSpecialist.getId(),
				BookingStatus.CANCELLED);

		assertEquals(1, result.size());
		assertEquals(targetBooking.getId(), result.get(0).getId());
		assertEquals(targetSpecialist.getId(), result.get(0).getSpecialistId());
		assertEquals(BookingStatus.CANCELLED, result.get(0).getStatus());
	}

	@Test
	public void shouldPersistBookingCancellationMetadata() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);

		LocalDateTime startTime = now.plusDays(1);
		LocalDateTime reservationCreatedAt = now.minusMinutes(10);
		LocalDateTime reservationExpiresAt = now.plusMinutes(10);
		LocalDateTime confirmationTime = now.minusMinutes(1);
		LocalDateTime bookingCreatedAt = now;
		LocalDateTime cancelledAt = now.plusMinutes(1);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-booking-metadata@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "BookingMetadata", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-booking-metadata@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));

		slot.reserve();
		slot.confirmBooking();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, reservationCreatedAt, reservationExpiresAt);

		reservation.confirm(confirmationTime);
		reservation = reservationRepository.saveAndFlush(reservation);

		Booking booking = new Booking(client, specialist, slot, reservation, bookingCreatedAt);

		booking.cancel(cancelledAt, CancellationInitiator.CLIENT, CancellationReason.CLIENT_REQUEST);

		booking = bookingRepository.saveAndFlush(booking);

		Long bookingId = booking.getId();

		entityManager.clear();

		Booking result = bookingRepository.findById(bookingId).orElseThrow();

		assertEquals(bookingId, result.getId());
		assertEquals(BookingStatus.CANCELLED, result.getStatus());
		assertEquals(cancelledAt, result.getCancelledAt());
		assertEquals(CancellationInitiator.CLIENT, result.getCancellationInitiator());
		assertEquals(CancellationReason.CLIENT_REQUEST, result.getCancellationReason());
	}

	@Test
	public void shouldRejectPartialBookingCancellationMetadata() {
		LocalDateTime now = LocalDateTime.now(clock).withNano(0);

		LocalDateTime startTime = now.plusDays(1);
		LocalDateTime reservationCreatedAt = now.minusMinutes(10);
		LocalDateTime reservationExpiresAt = now.plusMinutes(10);
		LocalDateTime confirmationTime = now.minusMinutes(1);
		LocalDateTime bookingCreatedAt = now;
		LocalDateTime cancelledAt = now.plusMinutes(1);

		User specialistUser = userRepository.saveAndFlush(new User("specialist-booking-partial@example.com",
				"password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = specialistRepository
				.saveAndFlush(new Specialist(specialistUser, "Anna", "BookingPartial", Duration.ZERO, Duration.ZERO));

		User client = userRepository.saveAndFlush(
				new User("client-booking-partial@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));

		slot.reserve();
		slot.confirmBooking();
		slot = slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, reservationCreatedAt, reservationExpiresAt);

		reservation.confirm(confirmationTime);
		reservation = reservationRepository.saveAndFlush(reservation);

		Booking booking = bookingRepository
				.saveAndFlush(new Booking(client, specialist, slot, reservation, bookingCreatedAt));

		Long bookingId = booking.getId();
		
		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				UPDATE bookings
				SET cancelled_at = ?
				WHERE id = ?
				""", cancelledAt, bookingId));
	}
}
