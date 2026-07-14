package com.psyweb.booking.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
	
	private Specialist specialist;
	private User client;
	private AvailabilitySlot slot;
	private Reservation reservation;
	private LocalDateTime now;
	
	@Mock
	BookingRepository bookingRepository;
	
	@Mock
	ReservationService reservationService;

    @Mock
    AvailabilitySlotService slotService;

    @Mock
    UserService userService;
    
    @Mock
    SpecialistService specialistService;

    @InjectMocks
    BookingService bookingService;
    
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

            specialist = new Specialist(
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
            ReflectionTestUtils.setField(specialist, "id", 2L);            
            ReflectionTestUtils.setField(slot, "id", 10L);
            ReflectionTestUtils.setField(reservation, "id", 100L);
    }
    
    @Test
    void shouldConfirmReservation() {
    	Long reservationId = 100L;
    	Long clientId = 1L;
    	
    	when(reservationService.getReservation(reservationId))
    		.thenReturn(reservation);
    	when(userService.getActiveUser(clientId))
    		.thenReturn(client);
    	when(slotService.getFreeSlot(reservation.getSlotId()))
    		.thenReturn(slot);
    	when(specialistService.getActiveSpecialist(slot.getSpecialistId()))
    		.thenReturn(specialist);
    	when(bookingRepository.save(any(Booking.class)))
    		.thenAnswer(invocation -> invocation.getArgument(0));
    	
    	Booking result = bookingService.confirmReservation(reservationId, clientId);
    	
    	assertEquals(specialist.getId(), result.getSpecialistId());
    	assertEquals(client.getId(), result.getClientId());
    	assertEquals(slot.getId(), result.getSlotId());
    	assertEquals(reservation.getId(), result.getReservationId());
    	
    	assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    	assertEquals(AvailabilityStatus.BOOKED, slot.getAvailabilityStatus());
    	
    	verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenReservationIdIsNull() {
    	Long reservationId = null;
    	Long clientId = 1L;
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.confirmReservation(reservationId, clientId));
    	
    	assertEquals("Incorrect id", exception.getMessage());
    	verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenClientIdIsNull() {
    	Long reservationId = 100L;
    	Long clientId = null;
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.confirmReservation(reservationId, clientId));
    	
    	assertEquals("Incorrect id", exception.getMessage());
    	verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenReservationBelongsToAnotherClient() {
    	Long reservationId = 100L;
    	Long clientId = 3L;
    	
    	when(reservationService.getReservation(reservationId))
    		.thenReturn(reservation);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.confirmReservation(reservationId, clientId));
    	
    	assertEquals("Reservation does not belong to this client", exception.getMessage());
    	assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldExpireReservationWhenExpired() {
    	Long reservationId = 100L;
    	Long clientId = 1L;
    	Reservation expired = new Reservation(client, slot, now.plusMinutes(15));
    	ReflectionTestUtils.setField(expired, "expiresAt", now.minusMinutes(10));
    	ReflectionTestUtils.setField(expired, "id", 100L);
    	
    	when(reservationService.getReservation(reservationId))
    		.thenReturn(expired);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.confirmReservation(reservationId, clientId));
    	
    	assertEquals("Reservation already expired", exception.getMessage());
    	assertEquals(ReservationStatus.EXPIRED, expired.getStatus());
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	verify(bookingRepository, never()).save(any());
    	
    }

    @Test
    void shouldThrowWhenReservationIsNotActive() {
    	Long reservationId = 100L;
    	Long clientId = 1L;
    	reservation.cancel();
    	
    	when(reservationService.getReservation(reservationId))
			.thenReturn(reservation);
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.confirmReservation(reservationId, clientId));
    	
    	assertEquals("Reservation must have status 'ACTIVE'", exception.getMessage());
    	assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    	assertEquals(AvailabilityStatus.FREE, slot.getAvailabilityStatus());
    	verify(bookingRepository, never()).save(any());
    }
	
    @Test
    void shouldCancelBooking() {
    	Long bookingId = 1L;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking booking = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(booking, "id", bookingId);
    	
    	when(bookingRepository.findById(bookingId))
    		.thenReturn(Optional.of(booking));
    	when(slotService.releaseBookedSlot(slot.getId()))
    		.thenReturn(slot);
    	
    	bookingService.cancelBooking(bookingId);
    	
    	assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    	assertNotEquals(null, booking.getCancelledAt());
    	verify(slotService).releaseBookedSlot(slot.getId());
    }

    @Test
    void shouldThrowWhenBookingIdIsNull() {
    	Long bookingId = null;
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.cancelBooking(bookingId));
    	
    	assertEquals("Incorrect id", exception.getMessage());
    	verify(bookingRepository, never()).save(any());
    	verifyNoInteractions(slotService); 
    }

    @Test
    void shouldThrowWhenBookingNotFound() {
    	Long bookingId = 1L;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking booking = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(booking, "id", bookingId);
    	
    	when(bookingRepository.findById(bookingId))
    		.thenReturn(Optional.empty());
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.cancelBooking(bookingId));
    	
    	assertEquals("Booking not found", exception.getMessage());
    	verify(bookingRepository, never()).save(any());
    	verifyNoInteractions(slotService); 
    }

    @Test
    void shouldNotReleaseSlotWhenCancelThrowsException() {
    	Long bookingId = 1L;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
    	Booking booking = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(booking, "id", bookingId);
    	booking.complete();
    	
    	when(bookingRepository.findById(bookingId))
			.thenReturn(Optional.of(booking));
    	
    	Exception exception = assertThrows(IllegalArgumentException.class, 
    			() -> bookingService.cancelBooking(bookingId));
    	
    	assertEquals("Cannot cancel booking", exception.getMessage());
    	verifyNoInteractions(slotService);
    }
	
    @Test
    void shouldReturnAllClientBookingsWhenStatusIsNull() {
    	Long clientId = 1L;
    	BookingStatus status = null;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking bookingF = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingF, "id", 1L);
    	bookingF.complete();
    	
    	Booking bookingS = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingS, "id", 2L);
    	bookingS.cancel(LocalDateTime.now());
    	
    	when(bookingRepository.findByClientId(clientId))
    		.thenReturn(List.of(bookingF, bookingS));
    	
    	List<Booking> result = bookingService.getClientBookings(clientId, status);
    	
    	assertEquals(2, result.size());
        assertTrue(result.contains(bookingF));
        assertTrue(result.contains(bookingS));
    }

    @Test
    void shouldFilterClientBookingsByStatus() {
    	Long clientId = 1L;
    	BookingStatus status = BookingStatus.CANCELLED;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking bookingF = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingF, "id", 1L);
    	bookingF.complete();
    	
    	Booking bookingS = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingS, "id", 2L);
    	bookingS.cancel(LocalDateTime.now());
    	
    	when(bookingRepository.findByClientId(clientId))
    		.thenReturn(List.of(bookingF, bookingS));
    	
    	List<Booking> result = bookingService.getClientBookings(clientId, status);
    	
    	assertEquals(1, result.size());
        assertFalse(result.contains(bookingF));
        assertTrue(result.contains(bookingS));
    }

    @Test
    void shouldThrowWhenGetClientBookingsClientIdIsNull() {
    	Long clientId = null;
    	BookingStatus status = BookingStatus.CANCELLED;
    	
    	Exception exception = assertThrows(IllegalArgumentException.class,
    			() -> bookingService.getClientBookings(clientId, status));
    	
    	assertEquals("Incorrect id", exception.getMessage());
    	verifyNoInteractions(bookingRepository);     
    }
    
    @Test
    void shouldReturnAllSpecialistBookingsWhenStatusIsNull() {
    	Long specialistId = 1L;
    	BookingStatus status = null;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking bookingF = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingF, "id", 1L);
    	bookingF.complete();
    	
    	Booking bookingS = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingS, "id", 2L);
    	bookingS.cancel(LocalDateTime.now());
    	
    	when(bookingRepository.findBySpecialistId(specialistId))
    		.thenReturn(List.of(bookingF, bookingS));
    	
    	List<Booking> result = bookingService.getSpecialistBookings(specialistId, status);
    	
    	assertEquals(2, result.size());
        assertTrue(result.contains(bookingF));
        assertTrue(result.contains(bookingS));
    }

    @Test
    void shouldFilterSpecialistBookingsByStatus() {
    	Long specialistId = 1L;
    	BookingStatus status = BookingStatus.CANCELLED;
    	ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

    	Booking bookingF = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingF, "id", 1L);
    	bookingF.complete();
    	
    	Booking bookingS = new Booking(client, specialist, slot, reservation);
    	ReflectionTestUtils.setField(bookingS, "id", 2L);
    	bookingS.cancel(LocalDateTime.now());
    	
    	when(bookingRepository.findBySpecialistId(specialistId))
		.thenReturn(List.of(bookingF, bookingS));
	
    	List<Booking> result = bookingService.getSpecialistBookings(specialistId, status);
    	
    	assertEquals(1, result.size());
        assertFalse(result.contains(bookingF));
        assertTrue(result.contains(bookingS));
    }

    @Test
    void shouldThrowWhenGetSpecialistBookingsSpecialistIdIsNull() {
    	Long specialistId = null;
    	BookingStatus status = BookingStatus.CANCELLED;
    	
    	Exception exception = assertThrows(IllegalArgumentException.class,
    			() -> bookingService.getSpecialistBookings(specialistId, status));
    	
    	assertEquals("Incorrect id", exception.getMessage());
    	verifyNoInteractions(bookingRepository);   
    }
}
