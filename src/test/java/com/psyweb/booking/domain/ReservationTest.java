package com.psyweb.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class ReservationTest {
	private final Clock clock = Clock.fixed(
	        Instant.parse("2099-01-01T10:00:00Z"),
	        ZoneId.of("UTC"));
	private final LocalDateTime now = LocalDateTime.now(clock);
	private Specialist specialist;
	private User client;
	private AvailabilitySlot slot;
	private Reservation reservation;
	
	@BeforeEach
    void setUp() {
    	client = new User("example@email.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);
    	
    	User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

    	specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);
            
    	slot = new AvailabilitySlot(specialist, now, now.plusHours(1));

    	reservation = new Reservation(client, slot, now.minusMinutes(10), now);
            
    	ReflectionTestUtils.setField(client, "id", 1L);
    	ReflectionTestUtils.setField(specialist, "id", 2L);            
    	ReflectionTestUtils.setField(slot, "id", 10L);
    	ReflectionTestUtils.setField(reservation, "id", 100L);
    }
	
	@Mock
	ReservationRepository reservationRepository;
	
	@Test
	public void shouldBeExpiredWhenNowEqualsExpiresAt() {		
		assertTrue(reservation.isExpired(now));
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}
	
	@Test
	public void shouldNotBeExpiredWhenNowIsBeforeExpiresAt() {
		ReflectionTestUtils.setField(reservation, "expiresAt", now.plusMinutes(1));

		assertFalse(reservation.isExpired(now));
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}
	
	@Test
	public void shouldRejectConfirmationWhenNowEqualsExpiresAt() {
		
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> reservation.confirm(now));
		
		assertEquals("Cannot mark confirm", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
	}
}
