package com.psyweb.specialist.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

public class SpecialistRepositoryIntegrationTest extends PostgreSQLIntegrationTest {
	
	@Autowired
    private UserRepository userRepository;
	
	@Autowired
    private SpecialistRepository specialistRepository;
	
	@Autowired
	private EntityManager entityManager;
	
	
	
	
	@Test
	public void shouldPersistAndRestoreSpecialistBookingNotices() {
		User specialistUser = userRepository
				.saveAndFlush(new User("userspecialist@example.com", "password-hash", UserRole.SPECIALIST, UserStatus.ACTIVE));
		Long userId = specialistUser.getId();
		
		Specialist specialist = new Specialist(specialistUser, "Mary", "Brown", Duration.ofMinutes(90), Duration.ofHours(24));	
		
		specialistRepository.saveAndFlush(specialist);
		
		entityManager.clear();
		
		Specialist restored = specialistRepository
				.findById(userId)
				.orElseThrow();
		
		assertEquals(Duration.ofMinutes(90), restored.getMinimumBookingNotice());
		assertEquals(Duration.ofHours(24), restored.getClientCancellationNotice());
		assertEquals(userId, restored.getId());
	}
}
