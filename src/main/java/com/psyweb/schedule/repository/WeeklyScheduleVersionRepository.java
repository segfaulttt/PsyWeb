
package com.psyweb.schedule.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.schedule.domain.WeeklyScheduleVersion;

public interface WeeklyScheduleVersionRepository extends JpaRepository<WeeklyScheduleVersion, UUID> {

	@Query("""
			SELECT v
			FROM WeeklyScheduleVersion v
			WHERE v.specialist.id = :specialistId
			  AND v.effectiveFrom <= :date
			  AND (
			      v.effectiveUntil IS NULL
			      OR v.effectiveUntil >= :date
			  )
			""")
	Optional<WeeklyScheduleVersion> findEffectiveVersion(@Param("specialistId") Long specialistId,
			@Param("date") LocalDate date);

	@Query("""
			SELECT v
			FROM WeeklyScheduleVersion v
			WHERE v.specialist.id = :specialistId
			  AND v.effectiveFrom <= :endDate
			  AND (
			      v.effectiveUntil IS NULL
			      OR v.effectiveUntil >= :startDate
			  )
			ORDER BY v.effectiveFrom
			""")
	List<WeeklyScheduleVersion> findOverlappingPeriod(@Param("specialistId") Long specialistId,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	@Query("""
			SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
			FROM WeeklyScheduleVersion v
			WHERE v.specialist.id = :specialistId
			  AND v.effectiveFrom >= :date
			""")
	boolean existsVersionStartingOnOrAfter(@Param("specialistId") Long specialistId, @Param("date") LocalDate date);
}