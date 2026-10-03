package com.psyweb.availability.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.availability.domain.AvailabilitySlot;

import jakarta.persistence.LockModeType;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

	public List<AvailabilitySlot> findBySpecialistId(Long specialistId);

	@Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END "
			+ "FROM AvailabilitySlot s "
			+ "WHERE s.specialist.id = :specialistId "
			+ "AND s.availabilityStatus <> com.psyweb.availability.domain.AvailabilityStatus.CANCELLED "
			+ "AND s.startTime < :newEnd " + "AND s.endTime > :newStart")
	boolean existsOverlappingSlot(@Param("specialistId") Long specialistId, @Param("newStart") LocalDateTime newStart,
			@Param("newEnd") LocalDateTime newEnd);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AvailabilitySlot> findForUpdateById(Long slotId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s "
			+ "FROM AvailabilitySlot s "
			+ "WHERE s.specialist.id = :specialistId "
			+ "AND s.startTime > :suspendedAt "
			+ "AND s.availabilityStatus <> com.psyweb.availability.domain.AvailabilityStatus.CANCELLED "
			+ "ORDER BY s.id")
	List<AvailabilitySlot> findFutureForUpdateBySpecialistId(
			@Param("specialistId") Long specialistId,
			@Param("suspendedAt")LocalDateTime suspendedAt);
}
