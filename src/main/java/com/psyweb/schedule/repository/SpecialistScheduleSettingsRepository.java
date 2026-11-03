
package com.psyweb.schedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.schedule.domain.SpecialistScheduleSettings;

import jakarta.persistence.LockModeType;

public interface SpecialistScheduleSettingsRepository extends JpaRepository<SpecialistScheduleSettings, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT s
			FROM SpecialistScheduleSettings s
			WHERE s.specialistId = :specialistId
			""")
	Optional<SpecialistScheduleSettings> findForUpdateBySpecialistId(@Param("specialistId") Long specialistId);
}