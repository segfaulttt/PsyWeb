package com.psyweb.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.booking.domain.Reservation;
import com.psyweb.booking.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long>{
	List<Reservation> findBySlotId(Long slotId);
	
	List<Reservation> findByClientId(Long clientId);
	
	List<Reservation> findByStatus(ReservationStatus status);
	
	Optional<Reservation> findBySlotIdAndStatus(Long slotId, ReservationStatus status);
	
	@Query(
			"SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
			"FROM Reservation r " +
			"WHERE r.slot.id = :slotId " +
			"AND r.status = :status"
			)
	boolean existsBySlotIdAndStatus(
			@Param("slotId")Long slotId,
			@Param("status") ReservationStatus status
			);
}
