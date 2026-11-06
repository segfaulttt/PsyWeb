package com.psyweb.schedule.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psyweb.booking.service.BookingObligationsQueryService;
import com.psyweb.booking.service.model.BookingObligation;
import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;
import com.psyweb.schedule.repository.SpecialistTimeZoneGuardRepository;
import com.psyweb.schedule.service.model.TimeZoneChangePreview;

@Service
public class SpecialistTimeZoneService {
	private final SpecialistTimeZoneGuardRepository specialistTimeZoneGuardRepository;
	private final BookingObligationsQueryService bookingObligationsQueryService;
	private final Clock clock;

	public SpecialistTimeZoneService(SpecialistTimeZoneGuardRepository specialistTimeZoneGuardRepository,
			BookingObligationsQueryService bookingObligationsQueryService, Clock clock) {
		this.specialistTimeZoneGuardRepository = specialistTimeZoneGuardRepository;
		this.bookingObligationsQueryService = bookingObligationsQueryService;
		this.clock = clock;
	}

	public ZoneId getTimeZone(Long specialistId) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}

		return specialistTimeZoneGuardRepository.findById(specialistId)
				.orElseThrow(() -> new IllegalStateException("Time zone guard not found")).getTimeZone();
	}

	@Transactional(readOnly = true)
	public TimeZoneChangePreview previewTimeZoneChange(Long specialistId, ZoneId newZoneId) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (newZoneId == null) {
			throw new IllegalArgumentException("New zone ID cannot be null");
		}
		ZoneId zoneId = getTimeZone(specialistId);
		Instant now = Instant.now(clock);
		List<BookingObligation> bookingsObl = bookingObligationsQueryService.getFutureConfirmedBookings(specialistId,
				now);
		List<BookingObligation> reservationsObl = bookingObligationsQueryService
				.getActiveFutureReservations(specialistId, now);

		long affectedBookingsCount = countAffectedObligations(bookingsObl, zoneId, newZoneId);

		long affectedReservationsCount = countAffectedObligations(reservationsObl, zoneId, newZoneId);

		return new TimeZoneChangePreview(zoneId, newZoneId, Math.toIntExact(affectedBookingsCount),
				Math.toIntExact(affectedReservationsCount));
	}

	@Transactional
	public void changeTimeZone(Long specialistId, ZoneId newZoneId, boolean acknowledged) {
		if (specialistId == null) {
			throw new IllegalArgumentException("Specialist ID cannot be null");
		}
		if (newZoneId == null) {
			throw new IllegalArgumentException("New zone ID cannot be null");
		}
		SpecialistTimeZoneGuard timeZoneGuard = specialistTimeZoneGuardRepository
				.findForUpdateBySpecialistId(specialistId)
				.orElseThrow(() -> new IllegalStateException("Time zone guard not found"));
		if (timeZoneGuard.getTimeZone().equals(newZoneId)) {
			return;
		}
		Instant now = Instant.now(clock);
		List<BookingObligation> bookingsObl = bookingObligationsQueryService.getFutureConfirmedBookings(specialistId,
				now);
		List<BookingObligation> reservationsObl = bookingObligationsQueryService
				.getActiveFutureReservations(specialistId, now);

		long affectedBookingsCount = countAffectedObligations(bookingsObl, timeZoneGuard.getTimeZone(), newZoneId);

		long affectedReservationsCount = countAffectedObligations(reservationsObl, timeZoneGuard.getTimeZone(), newZoneId);

		if (acknowledged == false && (affectedBookingsCount > 0 || affectedReservationsCount > 0)) {
			throw new IllegalStateException(
					"Time zone change requires acknowledgement of affected bookings and reservations");
		}
		timeZoneGuard.changeTimeZone(newZoneId, now);
	}

	private long countAffectedObligations(List<BookingObligation> obligations, ZoneId currentZoneId, ZoneId newZoneId) {
		return obligations.stream().filter(obligation -> {
			LocalDateTime oldTime = obligation.startTime().atZone(currentZoneId).toLocalDateTime();

			LocalDateTime newTime = obligation.startTime().atZone(newZoneId).toLocalDateTime();

			return !oldTime.equals(newTime);
		}).count();
	}
}
