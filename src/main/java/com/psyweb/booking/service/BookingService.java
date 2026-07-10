package com.psyweb.booking.service;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.availability.service.AvailabilitySlotService;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.domain.SpecialistStatus;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@Service
public class BookingService {
	private final BookingRepository bookingRepository;
	private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;
	
	public BookingService(BookingRepository bookingRepository, 
			AvailabilitySlotRepository slotRepository, 
			UserRepository userRepository) {
		this.bookingRepository = bookingRepository;
		this.slotRepository = slotRepository;
		this.userRepository = userRepository;
	}
	
	public Booking createBooking(Long clientId, Long slotId) {
		User client = userRepository.findById(clientId)
		        .orElseThrow("");
		// pupupu... later...
		if (client == null || client.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalArgumentException("Client cannot be blank");
		}
		if (specialist ==  null) {
			throw new IllegalArgumentException("Specialist cannot be blank");
		}
		if (specialist.getApprovalStatus() != SpecialistStatus.APPROVED) {
			throw new IllegalArgumentException("Specialist must have status 'APPROVED'");
		}
		if (slot == null) {
			throw new IllegalArgumentException("Slot cannot be blank");
		}
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new IllegalArgumentException("Slot must have status 'FREE'");
		}
		
		AvailabilitySlot bookedSlot = slotService.bookingSlot(slot.getId());
		
		Booking newBooking = new Booking(client, specialist, bookedSlot);
		
		return bookingRepository.save(newBooking);
	}
}
