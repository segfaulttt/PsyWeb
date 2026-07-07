package com.psyweb.availability.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psyweb.availability.domain.AvailabilitySlot;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long>{
	
	public List<AvailabilitySlot> findBySpecialistId(Long SpecialistId);

}
