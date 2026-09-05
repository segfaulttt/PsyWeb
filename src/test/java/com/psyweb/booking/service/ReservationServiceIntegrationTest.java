package com.psyweb.booking.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.doReturn;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.SlotAlreadyReservedException;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

public class ReservationServiceIntegrationTest extends PostgreSQLIntegrationTest {
		
	@Autowired
    private UserRepository userRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private AvailabilitySlotRepository slotRepository;

    @MockitoSpyBean
    private ReservationRepository reservationRepository;
    
    @Autowired
    private ReservationService reservationService;
	
	@Test
    public void shouldTranslateDatabaseConflictWhenConcurrentReservationsTargetSameSlot() throws InterruptedException, TimeoutException {
    	User specialistUser = userRepository
        		.saveAndFlush(new User("specialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));

        Specialist specialist = specialistRepository
        		.saveAndFlush(new Specialist(specialistUser, "Anna", "Smith", "Specialist bio"));

        User client = userRepository
        		.saveAndFlush(new User("client@example.com", "password-hash", UserRole.CLIENT, UserStatus.ACTIVE));

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);

        AvailabilitySlot slot = slotRepository
        		.saveAndFlush(new AvailabilitySlot(specialist, startTime, startTime.plusHours(1)));
        
        doReturn(false)
        	.when(reservationRepository)
        	.existsBySlotIdAndStatus(slot.getId(), ReservationStatus.ACTIVE);
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
        	List<Future<Reservation>> futures = new ArrayList<>();
        
        	for (int i = 0; i < 2; i++) {
        		Future<Reservation> future = executor.submit(
        				() -> reservationService.createReservation(client.getId(), slot.getId()));
        		futures.add(future);
        	}
        
        	int successfulAttempts = 0;
        	int failedAttempts = 0;
        
        	for(Future<Reservation> future : futures) {
        		try {
        			future.get(10, TimeUnit.SECONDS);
        			successfulAttempts++;
        		} catch(ExecutionException e) {
        			Throwable cause = e.getCause();
        			SlotAlreadyReservedException conflictException = assertInstanceOf(SlotAlreadyReservedException.class, cause);

        			assertEquals("SLOT_ALREADY_RESERVED", conflictException.code());
        			assertEquals("Slot is already reserved", conflictException.getMessage());
        			assertInstanceOf(DataIntegrityViolationException.class, conflictException.getCause());
        			failedAttempts++;
        		} 
        	}
        	assertEquals(1, successfulAttempts);
            assertEquals(1, failedAttempts);
        } finally {
       		executor.shutdownNow();
       	}
    }
}
