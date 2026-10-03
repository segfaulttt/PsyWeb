package com.psyweb.specialist.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;


import com.psyweb.specialist.domain.Specialist;

public interface SpecialistRepository extends JpaRepository<Specialist, Long>{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Specialist> findForUpdateById(Long specialistId);
}
