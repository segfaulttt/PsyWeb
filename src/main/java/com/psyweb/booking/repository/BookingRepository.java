package com.psyweb.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long>{
	List<Booking> findBySpecialist_Id(Long specialistId);
	
	List<Booking> findByClient_Id(Long clientId);
	
	List<Booking> findBySlot_Id(Long slotId);
	
	List<Booking> findByClient_IdAndStatus(Long clientId, BookingStatus status);

	List<Booking> findBySpecialist_IdAndStatus(Long specialistId, BookingStatus status);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT b
			FROM Booking b
			WHERE b.slot.id = :slotId
			AND b.status = com.psyweb.booking.domain.BookingStatus.CONFIRMED
			""")
	Optional<Booking> findConfirmedForUpdateBySlotId(@Param("slotId") Long slotId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Booking> findForUpdateById(Long bookingId);
}
