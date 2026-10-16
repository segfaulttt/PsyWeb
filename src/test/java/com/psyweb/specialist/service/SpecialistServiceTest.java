
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
import com.psyweb.specialist.exception.SpecialistNotEligibleException;
import com.psyweb.specialist.exception.SpecialistNotFoundException;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
class SpecialistServiceTest {

    @Mock
    private SpecialistRepository repository;

    @InjectMocks
    private SpecialistService service;

    private Specialist approved;
    private Specialist suspended;
    private Specialist rejected;
    private Specialist pending;

    @BeforeEach
    void setUp() {
        approved = specialist(1L);
        suspended = specialist(2L);
        rejected = specialist(3L);
        pending = specialist(4L);

        approved.approve();

        suspended.approve();
        suspended.suspend();

        rejected.reject();
    }

    @Test
    void shouldCreateSpecialistWithPendingStatusAndProvidedNotices() {
        User user = user(UserRole.SPECIALIST, UserStatus.ACTIVE);

        Duration bookingNotice = Duration.ofHours(2);
        Duration cancellationNotice = Duration.ofHours(24);

        when(repository.save(any(Specialist.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Specialist result = service.createSpecialist(
                user,
                "Anna",
                "Smith",
                bookingNotice,
                cancellationNotice
        );

        assertEquals("Anna", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(SpecialistStatus.PENDING, result.getApprovalStatus());
        assertEquals(bookingNotice, result.getMinimumBookingNotice());
        assertEquals(cancellationNotice, result.getClientCancellationNotice());

        verify(repository).save(same(result));
    }

    @Test
    void shouldRejectNullUserOnCreation() {
        assertThrows(
                InvalidSpecialistDataException.class,
                () -> service.createSpecialist(
                        null, "Anna", "Smith",
                        Duration.ZERO, Duration.ZERO
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectClientUserOnSpecialistCreation() {
        assertThrows(
                InvalidSpecialistDataException.class,
                () -> service.createSpecialist(
                        user(UserRole.CLIENT, UserStatus.ACTIVE),
                        "Anna",
                        "Smith",
                        Duration.ZERO,
                        Duration.ZERO
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectBlockedUserOnSpecialistCreation() {
        assertThrows(
                InvalidSpecialistDataException.class,
                () -> service.createSpecialist(
                        user(UserRole.SPECIALIST, UserStatus.BLOCKED),
                        "Anna",
                        "Smith",
                        Duration.ZERO,
                        Duration.ZERO
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectInvalidNoticeOnCreation() {
        assertThrows(
                InvalidSpecialistDataException.class,
                () -> service.createSpecialist(
                        user(UserRole.SPECIALIST, UserStatus.ACTIVE),
                        "Anna",
                        "Smith",
                        Duration.ofMinutes(-1),
                        Duration.ZERO
                )
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnSpecialistById() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(approved));

        assertSame(approved, service.getSpecialist(1L));
    }

    @Test
    void shouldRejectNullSpecialistIdWithoutCallingRepository() {
        InvalidSpecialistDataException ex = assertThrows(
                InvalidSpecialistDataException.class,
                () -> service.getSpecialist(null)
        );

        assertEquals("SPECIALIST_INVALID_DATA", ex.code());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldThrowExceptionWhenSpecialistNotFound() {
        when(repository.findById(11L))
                .thenReturn(Optional.empty());

        assertThrows(
                SpecialistNotFoundException.class,
                () -> service.getSpecialist(11L)
        );
    }

    @Test
    void shouldReturnEligibleSpecialist() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(approved));

        assertSame(approved, service.getEligibleSpecialist(1L));
    }

    @Test
    void shouldRejectIneligibleSpecialist() {
        when(repository.findById(4L))
                .thenReturn(Optional.of(pending));

        assertThrows(
                SpecialistNotEligibleException.class,
                () -> service.getEligibleSpecialist(4L)
        );

        when(repository.findById(2L))
                .thenReturn(Optional.of(suspended));

        assertThrows(
                SpecialistNotEligibleException.class,
                () -> service.getEligibleSpecialist(2L)
        );

        when(repository.findById(3L))
                .thenReturn(Optional.of(rejected));

        assertThrows(
                SpecialistNotEligibleException.class,
                () -> service.getEligibleSpecialist(3L)
        );
    }

    @Test
    void shouldApproveSpecialist() {
        when(repository.findForUpdateById(4L))
                .thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        service.approveSpecialist(4L);

        assertEquals(SpecialistStatus.APPROVED, pending.getApprovalStatus());
        verify(repository).save(pending);
    }

    @Test
    void shouldRejectSpecialist() {
        when(repository.findForUpdateById(4L))
                .thenReturn(Optional.of(pending));
        when(repository.save(pending)).thenReturn(pending);

        service.rejectSpecialist(4L);

        assertEquals(SpecialistStatus.REJECTED, pending.getApprovalStatus());
        verify(repository).save(pending);
    }

    @Test
    void shouldResubmitSpecialist() {
        when(repository.findForUpdateById(3L))
                .thenReturn(Optional.of(rejected));
        when(repository.save(rejected)).thenReturn(rejected);

        service.resubmitSpecialist(3L);

        assertEquals(SpecialistStatus.PENDING, rejected.getApprovalStatus());
    }

    @Test
    void shouldSuspendSpecialist() {
        when(repository.findForUpdateById(1L))
                .thenReturn(Optional.of(approved));
        when(repository.save(approved)).thenReturn(approved);

        service.suspendSpecialist(1L);

        assertEquals(SpecialistStatus.SUSPENDED, approved.getApprovalStatus());
    }

    @Test
    void shouldReinstateSpecialist() {
        when(repository.findForUpdateById(2L))
                .thenReturn(Optional.of(suspended));
        when(repository.save(suspended)).thenReturn(suspended);

        service.reinstateSpecialist(2L);

        assertEquals(SpecialistStatus.APPROVED, suspended.getApprovalStatus());
    }

    @Test
    void shouldRejectInvalidModerationTransitions() {
        when(repository.findForUpdateById(3L))
                .thenReturn(Optional.of(rejected));

        assertThrows(
                InvalidSpecialistStateException.class,
                () -> service.approveSpecialist(3L)
        );

        when(repository.findForUpdateById(1L))
                .thenReturn(Optional.of(approved));

        assertThrows(
                InvalidSpecialistStateException.class,
                () -> service.rejectSpecialist(1L)
        );

        when(repository.findForUpdateById(4L))
                .thenReturn(Optional.of(pending));

        assertThrows(
                InvalidSpecialistStateException.class,
                () -> service.resubmitSpecialist(4L)
        );

        assertThrows(
                InvalidSpecialistStateException.class,
                () -> service.suspendSpecialist(4L)
        );

        assertThrows(
                InvalidSpecialistStateException.class,
                () -> service.reinstateSpecialist(4L)
        );

        verify(repository, never()).save(any());
    }

    private static Specialist specialist(long id) {
        Specialist specialist = new Specialist(
                user(UserRole.SPECIALIST, UserStatus.ACTIVE),
                "Anna",
                "Smith",
                Duration.ZERO,
                Duration.ZERO
        );

        ReflectionTestUtils.setField(specialist, "id", id);
        return specialist;
    }

    private static User user(UserRole role, UserStatus status) {
        return new User(
                "user@example.com", "hash", role, status
        );
    }
}
