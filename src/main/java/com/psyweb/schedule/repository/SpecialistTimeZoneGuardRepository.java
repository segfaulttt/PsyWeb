
package com.psyweb.schedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.schedule.domain.SpecialistTimeZoneGuard;

import jakarta.persistence.LockModeType;

public interface SpecialistTimeZoneGuardRepository extends JpaRepository<SpecialistTimeZoneGuard, Long> {

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("""
			SELECT g
			FROM SpecialistTimeZoneGuard g
			WHERE g.specialistId = :specialistId
			""")
	Optional<SpecialistTimeZoneGuard> findForShareBySpecialistId(@Param("specialistId") Long specialistId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT g
			FROM SpecialistTimeZoneGuard g
			WHERE g.specialistId = :specialistId
			""")
	Optional<SpecialistTimeZoneGuard> findForUpdateBySpecialistId(@Param("specialistId") Long specialistId);
}