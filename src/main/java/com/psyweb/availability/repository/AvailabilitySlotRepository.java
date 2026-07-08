package com.psyweb.availability.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.availability.domain.AvailabilitySlot;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long>{
	
	public List<AvailabilitySlot> findBySpecialistId(Long specialistId);

	@Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
			"FROM AvailabilitySlot s " +
			"WHERE s.specialist.id = :specialistId " +
			"AND s.startTime < :newEnd " +
			"AND s.endTime > :newStart")
	public boolean existsOverlappingSlot(
			@Param("specialistId")Long specialistId, 
			@Param("newStart") LocalDateTime newStart, 
			@Param("newEnd")LocalDateTime newEnd
			);
	
}
