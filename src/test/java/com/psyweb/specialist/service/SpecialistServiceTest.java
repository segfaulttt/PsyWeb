package com.psyweb.specialist.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.exception.SpecialistNotApprovedException;
import com.psyweb.specialist.exception.SpecialistNotFoundException;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
public class SpecialistServiceTest {
	User userFirst = new User("userfirst@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
	User userSec = new User("usersec@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
	private Specialist approvedSpec = new Specialist(userFirst, "First", "Last", "bio");
	private Specialist pendingSpec = new Specialist(userSec, "Second", "End", "bio");
	
	@Mock
	SpecialistRepository repository;
	
	@InjectMocks
	SpecialistService service;
	
	@Test
	public void shouldReturnSpecialistById() {
		Long id = 1L;
		
		when(repository.findById(id))
			.thenReturn(Optional.of(approvedSpec));
		Specialist result = service.getSpecialist(id);
		
		assertEquals(approvedSpec, result);
	}
	
	@Test
	public void shouldRejectNullSpecialistIdWithoutCallingRepository() {		
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class, 
				() -> service.getSpecialist(null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Specialist id cannot be blank", exception.getMessage());
		verify(repository, never()).findById(any());
	}
	
	@Test
	public void shouldThrowExceptionWhenSpecialistNotFound() {
		when(repository.findById(11L))
			.thenReturn(Optional.empty());
		
		SpecialistNotFoundException exception = assertThrows(SpecialistNotFoundException.class, 
				() -> service.getSpecialist(11L));
		
		assertEquals("SPECIALIST_NOT_FOUND", exception.code());
		assertEquals("Specialist not found", exception.getMessage());
	}
	
	@Test
	public void shouldReturnApprovedSpecialistWhenApprovedStatusRequired() {
		approvedSpec = mock(Specialist.class);
		Long id = 1L;
		
		when(repository.findById(id))
			.thenReturn(Optional.of(approvedSpec));
		when(approvedSpec.getApprovalStatus())
			.thenReturn(SpecialistStatus.APPROVED);
		
		Specialist result = service.getActiveSpecialist(id);
		
		assertEquals(approvedSpec, result);
	}
	
	@Test
	public void shouldRejectNonApprovedSpecialistWhenApprovedStatusRequired() {
		Long id = 2L;
		
		when(repository.findById(id))
			.thenReturn(Optional.of(pendingSpec));

		SpecialistNotApprovedException exception = assertThrows(SpecialistNotApprovedException.class, 
				() -> service.getActiveSpecialist(id));
	
		assertEquals("SPECIALIST_NOT_APPROVED", exception.code());
		assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());
	}
}