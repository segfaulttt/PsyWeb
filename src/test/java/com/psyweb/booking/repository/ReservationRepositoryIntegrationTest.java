package com.psyweb.booking.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@SpringBootTest
@Testcontainers
@Transactional
public class ReservationRepositoryIntegrationTest {
	
	@Container
	static final PostgreSQLContainer<?> POSTGRES = 
		new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("psyweb")
			.withUsername("psyweb")
			.withPassword("psyweb");
	
	@DynamicPropertySource
	static void configureDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
	    registry.add("spring.datasource.username", POSTGRES::getUsername);
	    registry.add("spring.datasource.password", POSTGRES::getPassword);
	}
		
	@Autowired
    private UserRepository userRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private AvailabilitySlotRepository slotRepository;

    @Autowired
    private ReservationRepository reservationRepository;	
		
    @Test
    void shouldRejectSecondActiveReservationForSameSlot() {
        User specialistUser = userRepository
        		.saveAndFlush(new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

        Specialist specialist = specialistRepository
        		.saveAndFlush(new Specialist(specialistUser, "Anna", "Smith", "Specialist bio"));

        User client = userRepository
        		.saveAndFlush(new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);

        AvailabilitySlot slot = slotRepository
        		.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));

        Reservation firstReservation = new Reservation(client, slot, LocalDateTime.now().plusMinutes(5));

        reservationRepository.saveAndFlush(firstReservation);

        Reservation secondReservation = new Reservation(client, slot, LocalDateTime.now().plusMinutes(5));

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> reservationRepository.saveAndFlush(secondReservation));

        assertTrue(containsConstraintViolation(exception, "unique_active_reservation_slot"));
    }
    
    private boolean containsConstraintViolation(Throwable exception, String expectedConstraintName) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintException 
            		&& expectedConstraintName.equals(constraintException.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}