package com.psyweb.booking.service;

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.SlotAlreadyReservedException;
import com.psyweb.booking.repository.ReservationRepository;
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
	private AvailabilitySlot firstSlot;
	private AvailabilitySlot secondSlot;
	
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
		
		Specialist specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);
		
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
        
        firstSlot = new AvailabilitySlot(specialist, now.plusHours(1), now.plusHours(2));
		secondSlot = new AvailabilitySlot(specialist, now.plusHours(3), now.plusHours(4));

		ReflectionTestUtils.setField(firstSlot, "id", 20L);
		ReflectionTestUtils.setField(secondSlot, "id", 30L);
	}

	@Test
	void shouldCreateReservationSuccessfullyWhenSlotIsFree() {
		Long clientId = 1L;
		Long slotId = 10L;
		slot.reserve();
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(false);
		when(reservationRepository.saveAndFlush(any(Reservation.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(userService.getActiveUser(clientId))
			.thenReturn(client);
		when(slotService.reserveSlot(slotId))
			.thenReturn(slot);
		
		Reservation result = reservationService.createReservation(clientId, slotId);
		
		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());
		assertEquals(client.getId(), result.getClientId());
		assertEquals(slot.getId(), result.getSlotId());
		verify(reservationRepository).saveAndFlush(any(Reservation.class));
		verify(slotService).reserveSlot(slotId);
	}
	
	@Test
	void shouldNotCreateReservationWhenSlotReservationFails() {
		Long clientId = 1L;
		Long slotId = 10L;
		slot.cancel();
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(false);
		when(userService.getActiveUser(clientId))
			.thenReturn(client);
		when(slotService.reserveSlot(slotId))
			.thenThrow(new InvalidAvailabilitySlotStateException("Cannot reserve slot with status CANCELLED"));
		
		
		InvalidAvailabilitySlotStateException exception = assertThrows(InvalidAvailabilitySlotStateException.class,
				() -> reservationService.createReservation(clientId, slotId));
		
		assertEquals("Cannot reserve slot with status CANCELLED", exception.getMessage());
		verify(reservationRepository, never()).saveAndFlush(any(Reservation.class));
		verify(slotService).reserveSlot(slotId);
	}
	
	@Test
	void shouldThrowExceptionWhenActiveReservationAlreadyExistsForSlot() {
		Long clientId = 1L;
		Long slotId = 10L;
		
		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE))
			.thenReturn(true);
		
		SlotAlreadyReservedException exception = assertThrows(SlotAlreadyReservedException.class, 
				() -> reservationService.createReservation(clientId, slotId));
		
		assertEquals("SLOT_ALREADY_RESERVED", exception.code());
		assertEquals("Slot is already reserved", exception.getMessage());
		verify(reservationRepository, never()).saveAndFlush(any(Reservation.class));
		verifyNoInteractions(slotService);
	}
	
	@Test
	void shouldExpireActiveReservationsWhenExpiresAtIsBeforeNow() {
		Reservation expiredReservation = new Reservation(client, slot, now.plusMinutes(2));

		ReflectionTestUtils.setField(expiredReservation, "expiresAt", now.minusMinutes(10));

		when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
		        .thenReturn(List.of(expiredReservation));

		reservationService.expireExpiredReservations();

		assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());
		verify(slotService).releaseReservation(expiredReservation.getSlotId());
		
	}
	
	@Test
	void shouldKeepReservationActiveWhenExpiresAtIsAfterNow() {
		Reservation first = new Reservation(client, firstSlot, now.plusMinutes(2));
		Reservation second = new Reservation(client, secondSlot, now.plusMinutes(2));
		ReflectionTestUtils.setField(first, "expiresAt", now.minusMinutes(10));
		
		when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
			.thenReturn(List.of(first, second));
		 
		reservationService.expireExpiredReservations();
		
		assertEquals(ReservationStatus.ACTIVE, second.getStatus());
		assertEquals(ReservationStatus.EXPIRED, first.getStatus());
		verify(slotService).releaseReservation(first.getSlotId());
		verify(slotService, never()).releaseReservation(second.getSlotId());
	}
	
	@Test
	void shouldCancelReservationSuccessfullyWhenStatusIsActive() {
		Long reservationId = 100L;
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		reservationService.cancelReservation(reservationId);
		
		assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
		verify(slotService).releaseReservation(reservation.getSlotId());
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
		verify(slotService, never()).releaseReservation(anyLong());
	}
	
	@Test
	void shouldExpireSingleReservationSuccessfully() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "expiresAt", now.minusMinutes(10));
		
		when(reservationRepository.findById(reservationId))
			.thenReturn(Optional.of(reservation));
		
		reservationService.expireReservation(reservationId);
		
		assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
		verify(slotService).releaseReservation(reservation.getSlotId());
	}
	
	@Test
	void shouldThrowExceptionWhenExpireReservationIdIsNull() {
		Long reservationId = null;
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.expireReservation(reservationId));
		
		assertEquals("Reservation id cannot be null", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		verifyNoInteractions(slotService);
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
	
	@Test
	public void shouldNotReleaseSlotWhenReservationExpirationFails() {
		Reservation first = new Reservation(client, firstSlot, now.plusMinutes(2));
		ReflectionTestUtils.setField(first, "id", 111L);
		
		when(reservationRepository.findById(first.getId()))
			.thenReturn(Optional.of(first));
		
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.expireReservation(first.getId()));
		
		assertEquals("Cannot mark expired", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, first.getStatus());
		verify(slotService, never()).releaseReservation(any());
	}
	
	@Test
	public void shouldThrowExceptionWhenCancelReservationIdIsNull() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> reservationService.cancelReservation(null));
		
		assertEquals("Reservation id cannot be null", exception.getMessage());
		verify(slotService, never()).releaseReservation(any());
		verify(reservationRepository, never()).findById(any());
	}
}
