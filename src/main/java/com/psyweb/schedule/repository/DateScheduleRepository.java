
package com.psyweb.schedule.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.schedule.domain.DateSchedule;
import com.psyweb.schedule.domain.DateSchedulePurpose;

public interface DateScheduleRepository extends JpaRepository<DateSchedule, UUID> {

	@Query("""
			SELECT d
			FROM DateSchedule d
			WHERE d.specialist.id = :specialistId
			  AND d.date = :date
			  AND d.purpose = :purpose
			""")
	Optional<DateSchedule> findBySpecialistAndDateAndPurpose(@Param("specialistId") Long specialistId,
			@Param("date") LocalDate date, @Param("purpose") DateSchedulePurpose purpose);

	@Query("""
			SELECT d
			FROM DateSchedule d
			WHERE d.specialist.id = :specialistId
			  AND d.date BETWEEN :startDate AND :endDate
			ORDER BY d.date
			""")
	List<DateSchedule> findBySpecialistAndDateRange(@Param("specialistId") Long specialistId,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}