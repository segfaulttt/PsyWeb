package com.psyweb.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
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
	
	@Test
	public void shouldConfirmReservationAndCreateBookingAtomically() {
		User specialistUser = userRepository.saveAndFlush(
		        new User("specialist-confirm-success@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Success", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
		        new User("client-confirm-success@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(1);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository.saveAndFlush(
		        new Reservation(client, slot, LocalDateTime.now().plusMinutes(10)));
		
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
		User specialistUser = userRepository.saveAndFlush(
		        new User("specialist-confirm-rollback@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Rollback", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
		        new User("client-confirm-rollback@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = LocalDateTime.now().plusDays(2);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = reservationRepository.saveAndFlush(
		        new Reservation(client, slot, LocalDateTime.now().plusMinutes(10)));
		
		long bookingsBefore = bookingRepository.count();
		
		doThrow(new DataIntegrityViolationException("Booking save failed"))
			.when(bookingRepository)
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
		LocalDateTime now = LocalDateTime.now();

		User specialistUser = userRepository
				.saveAndFlush(new User("specialist-expired-confirmation@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

		Specialist specialist = new Specialist(specialistUser, "Anna", "Expired", Duration.ZERO, Duration.ZERO);
		specialist.approve();
		specialistRepository.saveAndFlush(specialist);

		User client = userRepository.saveAndFlush(
		        new User("client-expired-confirmation@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

		LocalDateTime startTime = now.plusDays(1);

		AvailabilitySlot slot = new AvailabilitySlot(specialist, startTime, startTime.plusHours(1));
		slot.reserve();
		slotRepository.saveAndFlush(slot);

		Reservation reservation = new Reservation(client, slot, now.plusMinutes(10));

		ReflectionTestUtils.setField(reservation, "expiresAt", now.minusMinutes(1));
		
		reservationRepository.saveAndFlush(reservation);

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
}
