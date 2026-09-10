package com.psyweb.specialist.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.specialist.exception.InvalidSpecialistStateException;
import com.psyweb.specialist.exception.SpecialistNotApprovedException;
import com.psyweb.specialist.exception.SpecialistNotFoundException;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
public class SpecialistServiceTest {
	private Specialist appSpec;
	private Specialist susSpec;
	private Specialist rejSpec;
	private Specialist penSpec;

	@BeforeEach
	void seUp() {
		User appUser = new User("appuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User susUser = new User("sususer@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User rejUser = new User("rejuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User penUser = new User("penuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		appSpec = new Specialist(appUser, "Ann", "Rid", Duration.ZERO, Duration.ZERO);
		ReflectionTestUtils.setField(appSpec, "id", 1L);
		susSpec = new Specialist(susUser, "Kate", "Jones", Duration.ZERO, Duration.ZERO);
		ReflectionTestUtils.setField(susSpec, "id", 2L);
		rejSpec = new Specialist(rejUser, "Hanna", "Smith", Duration.ZERO, Duration.ZERO);
		ReflectionTestUtils.setField(rejSpec, "id", 3L);
		penSpec = new Specialist(penUser, "Mary", "Williams", Duration.ZERO, Duration.ZERO);
		ReflectionTestUtils.setField(penSpec, "id", 4L);

		appSpec.approve();

		susSpec.approve();
		susSpec.suspend();

		rejSpec.reject();
	}

	@Mock
	SpecialistRepository repository;

	@InjectMocks
	SpecialistService service;

	@Test
	public void shouldReturnSpecialistById() {
		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));
		Specialist result = service.getSpecialist(appSpec.getId());

		assertEquals(appSpec, result);
	}

	@Test
	public void shouldRejectNullSpecialistIdWithoutCallingRepository() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> service.getSpecialist(null));

		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Specialist id cannot be null", exception.getMessage());
		verify(repository, never()).findById(any());
	}

	@Test
	public void shouldThrowExceptionWhenSpecialistNotFound() {
		when(repository.findById(11L)).thenReturn(Optional.empty());

		SpecialistNotFoundException exception = assertThrows(SpecialistNotFoundException.class,
				() -> service.getSpecialist(11L));

		assertEquals("SPECIALIST_NOT_FOUND", exception.code());
		assertEquals("Specialist not found", exception.getMessage());
	}

	@Test
	public void shouldReturnApprovedSpecialistWhenApprovedStatusRequired() {
		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));

		Specialist result = service.getActiveSpecialist(appSpec.getId());

		assertEquals(appSpec, result);
	}

	@Test
	public void shouldRejectNonApprovedSpecialistWhenApprovedStatusRequired() {
		when(repository.findById(penSpec.getId())).thenReturn(Optional.of(penSpec));
		SpecialistNotApprovedException exception = assertThrows(SpecialistNotApprovedException.class,
				() -> service.getActiveSpecialist(penSpec.getId()));

		assertEquals("SPECIALIST_NOT_APPROVED", exception.code());
		assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());

		when(repository.findById(susSpec.getId())).thenReturn(Optional.of(susSpec));
		exception = assertThrows(SpecialistNotApprovedException.class,
				() -> service.getActiveSpecialist(susSpec.getId()));

		assertEquals("SPECIALIST_NOT_APPROVED", exception.code());
		assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());
		
		when(repository.findById(rejSpec.getId())).thenReturn(Optional.of(rejSpec));
		exception = assertThrows(SpecialistNotApprovedException.class,
				() -> service.getActiveSpecialist(rejSpec.getId()));

		assertEquals("SPECIALIST_NOT_APPROVED", exception.code());
		assertEquals("Specialist must have status 'APPROVED'", exception.getMessage());
	}

	@Test
	public void shouldNotSaveSpecialistWhenStatusTransitionIsInvalid() {

	}

	@Test
	public void shouldRejectApproveFromInvalidStatus() {
		when(repository.findById(rejSpec.getId())).thenReturn(Optional.of(rejSpec));
		InvalidSpecialistStateException exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.approveSpecialist(rejSpec.getId()));

		assertEquals("Cannot approve specialist with status " + SpecialistStatus.REJECTED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.REJECTED, rejSpec.getApprovalStatus());
		verify(repository, never()).save(rejSpec);

		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.approveSpecialist(appSpec.getId()));

		assertEquals("Cannot approve specialist with status " + SpecialistStatus.APPROVED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.APPROVED, appSpec.getApprovalStatus());
		verify(repository, never()).save(appSpec);

		when(repository.findById(susSpec.getId())).thenReturn(Optional.of(susSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.approveSpecialist(susSpec.getId()));

		assertEquals("Cannot approve specialist with status " + SpecialistStatus.SUSPENDED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.SUSPENDED, susSpec.getApprovalStatus());
		verify(repository, never()).save(susSpec);
	}

	@Test
	public void shouldRejectRejectionFromInvalidStatus() {
		when(repository.findById(rejSpec.getId())).thenReturn(Optional.of(rejSpec));
		InvalidSpecialistStateException exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.rejectSpecialist(rejSpec.getId()));

		assertEquals("Cannot reject specialist with status " + SpecialistStatus.REJECTED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.REJECTED, rejSpec.getApprovalStatus());
		verify(repository, never()).save(rejSpec);

		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.rejectSpecialist(appSpec.getId()));

		assertEquals("Cannot reject specialist with status " + SpecialistStatus.APPROVED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.APPROVED, appSpec.getApprovalStatus());
		verify(repository, never()).save(appSpec);

		when(repository.findById(susSpec.getId())).thenReturn(Optional.of(susSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.rejectSpecialist(susSpec.getId()));

		assertEquals("Cannot reject specialist with status " + SpecialistStatus.SUSPENDED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.SUSPENDED, susSpec.getApprovalStatus());
		verify(repository, never()).save(susSpec);
	}

	@Test
	public void shouldRejectResubmitFromInvalidStatus() {
		when(repository.findById(penSpec.getId())).thenReturn(Optional.of(penSpec));
		InvalidSpecialistStateException exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.resubmitSpecialist(penSpec.getId()));

		assertEquals("Cannot resubmit specialist with status " + SpecialistStatus.PENDING, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.PENDING, penSpec.getApprovalStatus());
		verify(repository, never()).save(penSpec);

		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.resubmitSpecialist(appSpec.getId()));

		assertEquals("Cannot resubmit specialist with status " + SpecialistStatus.APPROVED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.APPROVED, appSpec.getApprovalStatus());
		verify(repository, never()).save(appSpec);

		when(repository.findById(susSpec.getId())).thenReturn(Optional.of(susSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.resubmitSpecialist(susSpec.getId()));

		assertEquals("Cannot resubmit specialist with status " + SpecialistStatus.SUSPENDED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.SUSPENDED, susSpec.getApprovalStatus());
		verify(repository, never()).save(susSpec);
	}

	@Test
	public void shouldRejectSuspensionFromInvalidStatus() {
		when(repository.findById(penSpec.getId())).thenReturn(Optional.of(penSpec));
		InvalidSpecialistStateException exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.suspendSpecialist(penSpec.getId()));

		assertEquals("Cannot suspend specialist with status " + SpecialistStatus.PENDING, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.PENDING, penSpec.getApprovalStatus());
		verify(repository, never()).save(penSpec);

		when(repository.findById(rejSpec.getId())).thenReturn(Optional.of(rejSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.suspendSpecialist(rejSpec.getId()));

		assertEquals("Cannot suspend specialist with status " + SpecialistStatus.REJECTED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.REJECTED, rejSpec.getApprovalStatus());
		verify(repository, never()).save(rejSpec);

		when(repository.findById(susSpec.getId())).thenReturn(Optional.of(susSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.suspendSpecialist(susSpec.getId()));

		assertEquals("Cannot suspend specialist with status " + SpecialistStatus.SUSPENDED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.SUSPENDED, susSpec.getApprovalStatus());
		verify(repository, never()).save(susSpec);
	}

	@Test
	public void shouldRejectReinstatementFromInvalidStatus() {
		when(repository.findById(penSpec.getId())).thenReturn(Optional.of(penSpec));
		InvalidSpecialistStateException exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.reinstateSpecialist(penSpec.getId()));

		assertEquals("Cannot reinstate specialist with status " + SpecialistStatus.PENDING, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.PENDING, penSpec.getApprovalStatus());
		verify(repository, never()).save(penSpec);

		when(repository.findById(rejSpec.getId())).thenReturn(Optional.of(rejSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.reinstateSpecialist(rejSpec.getId()));

		assertEquals("Cannot reinstate specialist with status " + SpecialistStatus.REJECTED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.REJECTED, rejSpec.getApprovalStatus());
		verify(repository, never()).save(rejSpec);

		when(repository.findById(appSpec.getId())).thenReturn(Optional.of(appSpec));
		exception = assertThrows(InvalidSpecialistStateException.class,
				() -> service.reinstateSpecialist(appSpec.getId()));

		assertEquals("Cannot reinstate specialist with status " + SpecialistStatus.APPROVED, exception.getMessage());
		assertEquals("SPECIALIST_INVALID_STATE", exception.code());
		assertEquals(SpecialistStatus.APPROVED, appSpec.getApprovalStatus());
		verify(repository, never()).save(appSpec);
	}
}