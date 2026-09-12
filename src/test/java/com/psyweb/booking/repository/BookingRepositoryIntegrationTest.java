package com.psyweb.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class BookingRepositoryIntegrationTest extends PostgreSQLIntegrationTest {
	
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
		User targetClient = userRepository
				.saveAndFlush(new User("targetclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		User specialistUser = userRepository
        		.saveAndFlush(new User("specialistuser@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));
		
		Specialist specialist = specialistRepository
        		.saveAndFlush(new Specialist(specialistUser, "Ann", "Jobs", Duration.ZERO, Duration.ZERO));
		LocalDateTime startTime = LocalDateTime.now().plusDays(2);
		
		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));
			
		Reservation reservation = new Reservation(targetClient, slot, LocalDateTime.now().plusMinutes(5));
		reservation.confirm();
		reservationRepository.saveAndFlush(reservation);
		
		Booking targetBooking = new Booking(targetClient, specialist, slot, reservation, LocalDateTime.now().plusMinutes(5));
		targetBooking.cancel(startTime.plusDays(3));
		bookingRepository.saveAndFlush(targetBooking);
		
		AvailabilitySlot differentStatusSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime.plusHours(2), startTime.plusHours(3)));
		
		Reservation differentStatusReservation = new Reservation(targetClient, differentStatusSlot, LocalDateTime.now().plusMinutes(10));
		differentStatusReservation.confirm();
		reservationRepository.saveAndFlush(differentStatusReservation);
		
		Booking differentStatusBooking = new Booking(targetClient, specialist, differentStatusSlot, differentStatusReservation, LocalDateTime.now().plusMinutes(10));
		differentStatusBooking.complete();
		bookingRepository.saveAndFlush(differentStatusBooking);
		
		User otherClient = userRepository
				.saveAndFlush(new User("difclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		
		AvailabilitySlot otherClientSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(specialist, startTime.plusHours(4), startTime.plusHours(5)));
		
		Reservation otherClientReservation = new Reservation(otherClient, otherClientSlot, LocalDateTime.now().plusMinutes(10));
		otherClientReservation.confirm();
		reservationRepository.saveAndFlush(otherClientReservation);
		
		Booking otherClientBooking = new Booking(otherClient, specialist, otherClientSlot, otherClientReservation, LocalDateTime.now().plusMinutes(10));
		otherClientBooking.cancel(startTime.plusDays(4));
		bookingRepository.saveAndFlush(otherClientBooking);
		
		List<Booking> result = bookingRepository
				.findByClient_IdAndStatus(targetClient.getId(), BookingStatus.CANCELLED);
		
		assertEquals(1, result.size());
		assertEquals(targetBooking.getId(), result.get(0).getId());
		assertEquals(targetClient.getId(), result.get(0).getClientId());
		assertEquals(BookingStatus.CANCELLED, result.get(0).getStatus());
	}
	
	@Test
	public void shouldFindSpecialistBookingsByStatus() {
		User targetClient = userRepository
				.saveAndFlush(new User("targetclient@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));
		User specialistUser = userRepository
        		.saveAndFlush(new User("specialistuser@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));
		
		Specialist targetSpecialist = specialistRepository
        		.saveAndFlush(new Specialist(specialistUser, "Ann", "Jobs", Duration.ZERO, Duration.ZERO));
		LocalDateTime startTime = LocalDateTime.now().plusDays(2);
		
		AvailabilitySlot slot = slotRepository
				.saveAndFlush(new AvailabilitySlot(targetSpecialist, startTime, startTime.plusHours(1)));
			
		Reservation reservation = new Reservation(targetClient, slot, LocalDateTime.now().plusMinutes(5));
		reservation.confirm();
		reservationRepository.saveAndFlush(reservation);
		
		Booking targetBooking = new Booking(targetClient, targetSpecialist, slot, reservation, LocalDateTime.now().plusMinutes(5));
		targetBooking.cancel(startTime.plusDays(3));
		bookingRepository.saveAndFlush(targetBooking);
		
		AvailabilitySlot differentStatusSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(targetSpecialist, startTime.plusHours(2), startTime.plusHours(3)));
		
		Reservation differentStatusReservation = new Reservation(targetClient, differentStatusSlot, LocalDateTime.now().plusMinutes(10));
		differentStatusReservation.confirm();
		reservationRepository.saveAndFlush(differentStatusReservation);
		
		Booking differentStatusBooking = new Booking(targetClient, targetSpecialist, differentStatusSlot, differentStatusReservation, LocalDateTime.now().plusMinutes(10));
		differentStatusBooking.complete();
		bookingRepository.saveAndFlush(differentStatusBooking);

		User otherSpecialistUser = userRepository
				.saveAndFlush(new User("otherspecialistclient@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));
		
		Specialist otherSpecialist = specialistRepository
        		.saveAndFlush(new Specialist(otherSpecialistUser, "John", "Smith", Duration.ZERO, Duration.ZERO));
		
		AvailabilitySlot otherSpecialistSlot = slotRepository
				.saveAndFlush(new AvailabilitySlot(otherSpecialist, startTime.plusHours(4), startTime.plusHours(5)));
		
		Reservation otherSpecialistReservation = new Reservation(targetClient, otherSpecialistSlot, LocalDateTime.now().plusMinutes(10));
		otherSpecialistReservation.confirm();
		reservationRepository.saveAndFlush(otherSpecialistReservation);
		
		Booking otherSpecialistBooking = new Booking(targetClient, otherSpecialist, otherSpecialistSlot, otherSpecialistReservation, LocalDateTime.now().plusMinutes(10));
		otherSpecialistBooking.cancel(startTime.plusDays(4));
		bookingRepository.saveAndFlush(otherSpecialistBooking);
		
		List<Booking> result = bookingRepository
				.findBySpecialist_IdAndStatus(targetSpecialist.getId(), BookingStatus.CANCELLED);
		
		assertEquals(1, result.size());
		assertEquals(targetBooking.getId(), result.get(0).getId());
		assertEquals(targetSpecialist.getId(), result.get(0).getSpecialistId());
		assertEquals(BookingStatus.CANCELLED, result.get(0).getStatus());
	}
}
