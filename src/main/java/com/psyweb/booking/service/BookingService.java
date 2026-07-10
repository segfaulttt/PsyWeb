package com.psyweb.booking.service;

import org.springframework.stereotype.Service;

import com.psyweb.availability.domain.AvailabilitySlot;
import com.psyweb.availability.domain.AvailabilityStatus;
import com.psyweb.availability.repository.AvailabilitySlotRepository;
import com.psyweb.booking.domain.Booking;
import com.psyweb.booking.repository.BookingRepository;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.service.SpecialistService;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.UserService;

@Service
public class BookingService {
	private final BookingRepository bookingRepository;
	private final AvailabilitySlotRepository slotRepository;
    private final UserService userService;
    private final SpecialistService specialistService;
	
	public BookingService(BookingRepository bookingRepository, 
			AvailabilitySlotRepository slotRepository, 
			UserService userService, 
			SpecialistService specialistService) {
		this.bookingRepository = bookingRepository;
		this.slotRepository = slotRepository;
		this.userService = userService;
		this.specialistService = specialistService;
	}
	
	public Booking createBooking(Long clientId, Long slotId) {
		User client = userService.getActiveUser(clientId);
		AvailabilitySlot slot = slotRepository.findById(slotId)
				.orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));
		if (slot.getAvailabilityStatus() != AvailabilityStatus.FREE) {
			throw new IllegalArgumentException("Slot must have status 'FREE'");
		}
		Specialist specialist = specialistService.getActiveSpecialist(slot.getSpecialistId());
		
		Booking newBooking = new Booking(client, specialist, slot);
		
		return bookingRepository.save(newBooking);
	}
}
