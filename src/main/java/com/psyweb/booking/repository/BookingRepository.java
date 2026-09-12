package com.psyweb.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long>{
	List<Booking> findBySpecialist_Id(Long specialistId);
	
	List<Booking> findByClient_Id(Long clientId);
	
	List<Booking> findBySlot_Id(Long slotId);
	
	List<Booking> findByClient_IdAndStatus(Long clientId, BookingStatus status);

	List<Booking> findBySpecialist_IdAndStatus(Long specialistId, BookingStatus status);
}
