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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.exception.InvalidAvailabilitySlotStateException;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.config.ReservationProperties;
import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;
import com.psyweb.booking.exception.ActiveReservationAlreadyExistsException;
import com.psyweb.booking.exception.InvalidReservationDataException;
import com.psyweb.booking.exception.InvalidReservationStateException;
import com.psyweb.booking.repository.ReservationRepository;
import com.psyweb.cancellation.domain.CancellationInitiator;
import com.psyweb.cancellation.domain.CancellationReason;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

	private User client;
	private AvailabilitySlot slot;
	private Reservation reservation;
	private AvailabilitySlot firstSlot;
	private AvailabilitySlot secondSlot;
	private final Clock clock = Clock.fixed(Instant.parse("2099-01-01T10:00:00Z"), ZoneId.of("UTC"));

	private final LocalDateTime now = LocalDateTime.now(clock);

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
		client = new User("example@email.ru", "password", UserRole.CLIENT, UserStatus.ACTIVE);

		User user = new User("email@gmail.com", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);

		Specialist specialist = new Specialist(user, "firstName", "lastName", Duration.ZERO, Duration.ZERO);

		slot = new AvailabilitySlot(specialist, now, now.plusHours(1));

		reservation = new Reservation(client, slot, now, now.plusMinutes(2));

		ReflectionTestUtils.setField(client, "id", 1L);
		ReflectionTestUtils.setField(slot, "id", 10L);
		ReflectionTestUtils.setField(reservation, "id", 100L);

		firstSlot = new AvailabilitySlot(specialist, now.plusHours(1), now.plusHours(2));
		secondSlot = new AvailabilitySlot(specialist, now.plusHours(3), now.plusHours(4));

		ReflectionTestUtils.setField(firstSlot, "id", 20L);
		ReflectionTestUtils.setField(secondSlot, "id", 30L);

		ReservationProperties reservationProperties = new ReservationProperties(Duration.ofMinutes(10),
				Duration.ofMinutes(1), 100);

		reservationService = new ReservationService(reservationRepository, userService, slotService, clock,
				reservationProperties);
	}

	@Test
	void shouldCreateReservationSuccessfullyWhenSlotIsFree() {
		Long clientId = 1L;
		Long slotId = 10L;
		slot.reserve();

		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE)).thenReturn(false);
		when(reservationRepository.saveAndFlush(any(Reservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(userService.getActiveUser(clientId)).thenReturn(client);
		when(slotService.reserveSlot(slotId)).thenReturn(slot);

		Reservation result = reservationService.createReservation(clientId, slotId);

		assertEquals(AvailabilityStatus.RESERVED, slot.getAvailabilityStatus());
		assertEquals(client.getId(), result.getClientId());
		assertEquals(slot.getId(), result.getSlotId());
		assertEquals(now, result.getCreatedAt());
		assertEquals(now.plusMinutes(10), result.getExpiresAt());
		verify(reservationRepository).saveAndFlush(any(Reservation.class));
		verify(slotService).reserveSlot(slotId);
	}

	@Test
	void shouldNotCreateReservationWhenSlotReservationFails() {
		Long clientId = 1L;
		Long slotId = 10L;
		slot.cancel(now, CancellationInitiator.SPECIALIST, CancellationReason.SPECIALIST_REMOVED_AVAILABILITY);
		assertEquals(AvailabilityStatus.CANCELLED, slot.getAvailabilityStatus());

		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE)).thenReturn(false);
		when(userService.getActiveUser(clientId)).thenReturn(client);
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

		when(reservationRepository.existsBySlotIdAndStatus(slotId, ReservationStatus.ACTIVE)).thenReturn(true);

		ActiveReservationAlreadyExistsException exception = assertThrows(ActiveReservationAlreadyExistsException.class,
				() -> reservationService.createReservation(clientId, slotId));

		assertEquals("ACTIVE_RESERVATION_ALREADY_EXISTS", exception.code());
		assertEquals("Slot is already reserved", exception.getMessage());
		verify(reservationRepository, never()).saveAndFlush(any(Reservation.class));
		verifyNoInteractions(slotService);
	}

	@Test
	void shouldExpireActiveReservationsWhenExpiresAtIsBeforeNow() {
		Reservation expiredReservation = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));

		when(reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100))
				.thenReturn(List.of(expiredReservation));

		reservationService.expireExpiredReservations();

		assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());
		verify(slotService).releaseReservation(expiredReservation.getSlotId());

	}

	@Test
	void shouldKeepReservationActiveWhenExpiresAtIsAfterNow() {
		Reservation first = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));
		Reservation second = new Reservation(client, secondSlot, now, now.plusMinutes(2));

		when(reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100)).thenReturn(List.of(first));

		reservationService.expireExpiredReservations();

		assertEquals(ReservationStatus.ACTIVE, second.getStatus());
		assertEquals(ReservationStatus.EXPIRED, first.getStatus());
		verify(slotService).releaseReservation(first.getSlotId());
		verify(slotService, never()).releaseReservation(second.getSlotId());
		verify(reservationRepository).findExpiredBatchForUpdateSkipLocked(now, 100);
	}

	@Test
	void shouldDoNothingWhenExpiredReservationBatchIsEmpty() {
		when(reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100)).thenReturn(List.of());

		reservationService.expireExpiredReservations();

		verify(reservationRepository).findExpiredBatchForUpdateSkipLocked(now, 100);

		verifyNoInteractions(slotService);
	}

	@Test
	void shouldCancelReservationSuccessfullyWhenStatusIsActive() {
		Long reservationId = 100L;

		when(reservationRepository.findForUpdateById(reservationId)).thenReturn(Optional.of(reservation));

		reservationService.cancelReservation(reservationId, now, CancellationInitiator.CLIENT,
				CancellationReason.CLIENT_REQUEST);

		assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
		assertEquals(now, reservation.getCancelledAt());
		assertEquals(CancellationInitiator.CLIENT, reservation.getCancellationInitiator());
		assertEquals(CancellationReason.CLIENT_REQUEST, reservation.getCancellationReason());
		verify(slotService).releaseReservation(reservation.getSlotId());
	}

	@Test
	void shouldThrowExceptionWhenCancellingAlreadyExpiredOrCancelledReservation() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);

		when(reservationRepository.findForUpdateById(reservationId)).thenReturn(Optional.of(reservation));
		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> reservationService.cancelReservation(reservationId, now, CancellationInitiator.CLIENT,
						CancellationReason.CLIENT_REQUEST));

		assertEquals("RESERVATION_INVALID_STATE", exception.code());
		assertEquals("Cannot mark cancelled", exception.getMessage());
		assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
		verify(slotService, never()).releaseReservation(anyLong());
	}

	@Test
	void shouldExpireSingleReservationSuccessfully() {
		Reservation expiredReservation = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));
		ReflectionTestUtils.setField(expiredReservation, "id", 60L);

		when(reservationRepository.findForUpdateById(expiredReservation.getId()))
				.thenReturn(Optional.of(expiredReservation));

		reservationService.expireReservation(expiredReservation.getId());

		assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());
		verify(slotService).releaseReservation(expiredReservation.getSlotId());
	}

	@Test
	void shouldThrowExceptionWhenExpireReservationIdIsNull() {
		Long reservationId = null;

		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservationService.expireReservation(reservationId));

		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Reservation id cannot be null", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
		verifyNoInteractions(slotService);
	}

	@Test
	void shouldReturnReservationWhenStatusIsActive() {
		Long reservationId = 100L;

		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

		Reservation result = reservationService.getActiveReservationById(reservationId);

		assertEquals(ReservationStatus.ACTIVE, result.getStatus());
	}

	@Test
	void shouldThrowExceptionWhenReservationIsNotActive() {
		Long reservationId = 100L;
		ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CANCELLED);

		when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> reservationService.getActiveReservationById(reservationId));

		assertEquals("RESERVATION_INVALID_STATE", exception.code());
		assertEquals("Reservation must have status 'ACTIVE'", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenGetReservationIdIsNull() {
		Long reservationId = null;

		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservationService.getActiveReservationById(reservationId));

		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Incorrect Id", exception.getMessage());
	}

	@Test
	public void shouldNotReleaseSlotWhenReservationExpirationFails() {
		Reservation first = new Reservation(client, firstSlot, now, now.plusMinutes(2));
		ReflectionTestUtils.setField(first, "id", 111L);

		when(reservationRepository.findForUpdateById(first.getId())).thenReturn(Optional.of(first));

		InvalidReservationStateException exception = assertThrows(InvalidReservationStateException.class,
				() -> reservationService.expireReservation(first.getId()));

		assertEquals("RESERVATION_INVALID_STATE", exception.code());
		assertEquals("Cannot mark expired", exception.getMessage());
		assertEquals(ReservationStatus.ACTIVE, first.getStatus());
		verify(slotService, never()).releaseReservation(any());
	}

	@Test
	public void shouldThrowExceptionWhenCancelReservationIdIsNull() {
		InvalidReservationDataException exception = assertThrows(InvalidReservationDataException.class,
				() -> reservationService.cancelReservation(null, now, CancellationInitiator.CLIENT,
						CancellationReason.CLIENT_REQUEST));

		assertEquals("RESERVATION_INVALID_DATA", exception.code());
		assertEquals("Reservation id cannot be null", exception.getMessage());
		verify(slotService, never()).releaseReservation(any());
		verify(reservationRepository, never()).findForUpdateById(any());
	}

	@Test
	void shouldNotProcessReservationAgainOnRepeatedExpirationRun() {
		Reservation expiredReservation = new Reservation(client, slot, now.minusMinutes(20), now.minusMinutes(10));

		when(reservationRepository.findExpiredBatchForUpdateSkipLocked(now, 100))
				.thenReturn(List.of(expiredReservation)).thenReturn(List.of());

		reservationService.expireExpiredReservations();
		reservationService.expireExpiredReservations();

		assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());

		verify(slotService, times(1)).releaseReservation(expiredReservation.getSlotId());

		verify(reservationRepository, times(2)).findExpiredBatchForUpdateSkipLocked(now, 100);
	}
}
