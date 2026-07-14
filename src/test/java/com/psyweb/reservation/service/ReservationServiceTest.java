package com.psyweb.reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.booking.service.ReservationService;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {
	
	private User client;
	private AvailabilitySlot slot;
	private LocalDateTime now;
	private Reservation reservation;
	
	@Mock
	ReservationRepository reservationRepository;
	
	@Mock
	UserService userService;
	
	@Mock
	AvailabilitySlotService slotService;
	
	@InjectMocks
	ReservationService reservationService;
	
	@BeforeEach
	void setUp() {
		now = LocalDateTime.now();
		client = new User(
				"example@email.ru",
    			"password",
    			UserRole.CLIENT,
    			UserStatus.ACTIVE
		);
		
		User user = new User(
				"email@gmail.com",
                "password",
                UserRole.SPECIALIST,
                UserStatus.ACTIVE
		);
		
		Specialist specialist = new Specialist(
                user,
                "firstName",
                "lastName",
                "bio"
        );
		
		slot = new AvailabilitySlot(
				specialist,
        	    now,
        	    now.plusHours(1)
		);
		
		reservation = new Reservation(
           	    client,
           	    slot,
           	    now.plusMinutes(2)
		);
		
		ReflectionTestUtils.setField(client, "id", 1L);           
        ReflectionTestUtils.setField(slot, "id", 10L);
        ReflectionTestUtils.setField(reservation, "id", 100L);
	}

	@Test
	void shouldCreateReservationSuccessfullyWhenSlotIsFree() {
		Long clientId = 1L;
		Long slotId = 10L;
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(false);
		when(reservationRepository.save(any(Reservation.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(userService.getActiveUser(clientId))
			.thenReturn(client);
		when(slotService.getFreeSlot(slotId))
			.thenReturn(slot);
		
		Reservation result = reservationService.createReservation(clientId, slotId);
		
		assertEquals(client.getId(), result.getClientId());
		assertEquals(slot.getId(), result.getSlotId());
		verify(reservationRepository).save(any(Reservation.class));
	}
	
	@Test
	void shouldThrowExceptionWhenSlotIsAlreadyBooked() {
		Long clientId = 1L;
		Long slotId = 10L;
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(false);
		when(userService.getActiveUser(clientId))
			.thenReturn(client);
		when(slotService.getFreeSlot(slotId))
			.thenThrow(new IllegalArgumentException("Slot must have status 'FREE'"));
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.createReservation(clientId, slotId));
		
		assertEquals("Slot must have status 'FREE'", exception.getMessage());
		verify(reservationRepository, never()).save(any(Reservation.class));
	}
	
	@Test
	void shouldThrowExceptionWhenActiveReservationAlreadyExistsForSlot() {
		Long clientId = 1L;
		Long slotId = 10L;
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(true);
		
		Exception exception = assertThrows(IllegalArgumentException.class, 
				() -> reservationService.createReservation(clientId, slotId));
		
		assertEquals("Reservation already exists", exception.getMessage());
		verify(reservationRepository, never()).save(any(Reservation.class));
	}
	
	@Test
	void shouldExpireActiveReservationsWhenExpiresAtIsBeforeNow() {
		Reservation first = new Reservation(
				client,
           	    slot,
           	    now.plusMinutes(2)
		);
		Reservation second = new Reservation(
				client,
           	    slot,
           	    now.plusMinutes(2)
		);
		ReflectionTestUtils.setField(first, "expiresAt", now.minusMinutes(10));
		
		
		when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
			.thenReturn(List.of(first, second));
		 
		reservationService.expireExpiredReservations();
		
		assertEquals(ReservationStatus.EXPIRED, first.getStatus());
	}
	
	@Test
	void shouldKeepReservationActiveWhenExpiresAtIsAfterNow() {
		Reservation first = new Reservation(
				client,
           	    slot,
           	    now.plusMinutes(2)
		);
		Reservation second = new Reservation(
				client,
           	    slot,
           	    now.plusMinutes(2)
		);
		ReflectionTestUtils.setField(first, "expiresAt", now.minusMinutes(10));
		
		
		when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
			.thenReturn(List.of(first, second));
		 
		reservationService.expireExpiredReservations();
		
		assertEquals(ReservationStatus.ACTIVE, second.getStatus());
	}
	
	@Test
	void shouldCancelReservationSuccessfullyWhenStatusIsActive() {
		Long reservationId = 100L;
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		reservationService.cancelReservation(reservationId);
		
		assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
	}
	
	@Test
	void shouldThrowExceptionWhenCancellingAlreadyExpiredOrCancelledReservation() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.cancelReservation(reservationId));
		
		assertEquals("Cannot mark cancelled", exception.getMessage());
		assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
	}
	
	@Test
	void shouldExpireSingleReservationSuccessfully() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "expiresAt", now.minusMinutes(10));
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		reservationService.expireReservation(reservationId);
		
		assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
	}
	
	@Test
	void shouldThrowExceptionWhenExpireReservationIdIsNull() {
		Long reservationId = null;
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.expireReservation(reservationId));
		
		assertEquals("Incorrect id", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}
	
	@Test
	void shouldReturnReservationWhenStatusIsActive() {
		Long reservationId = 100L;
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		Reservation result = reservationService.getActiveReservationById(reservationId);
		
		assertEquals(ReservationStatus.ACTIVE, result.getStatus());
	}
	
	@Test
	void shouldThrowExceptionWhenReservationIsNotActive() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CANCELLED);
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.getActiveReservationById(reservationId));
		
		assertEquals("Reservation must have status 'ACTIVE'", exception.getMessage());
	}
	
	@Test
	void shouldThrowExceptionWhenGetReservationIdIsNull() {
		Long reservationId = null;
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.getActiveReservationById(reservationId));
		
		assertEquals("Incorrect Id", exception.getMessage());
	}
}
