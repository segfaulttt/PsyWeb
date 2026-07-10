package com.psyweb.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psyweb.booking.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long>{
	List<Booking> findBySpecialistId(Long specialistId);
	
	List<Booking> findByClientId(Long clientId);
	
	List<Booking> findBySlotId(Long slotId);
}
