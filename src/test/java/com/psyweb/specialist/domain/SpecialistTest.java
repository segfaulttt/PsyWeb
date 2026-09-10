package com.psyweb.specialist.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.specialist.exception.InvalidSpecialistDataException;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;

public class SpecialistTest {
	private final User user = new User("userfirst@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
	private final User client = new User("client@example.ru", "passwordsec", UserRole.CLIENT, UserStatus.ACTIVE);
	private final User admin = new User("admin@example.ru", "passwordthird", UserRole.ADMIN, UserStatus.ACTIVE);
	Specialist appSpec;
	Specialist susSpec;
	Specialist rejSpec;
	Specialist penSpec;
	
	@BeforeEach
	void seUp() {
		User appUser = new User("appuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User susUser = new User("sususer@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User rejUser = new User("rejuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		User penUser = new User("penuser@example.ru", "password", UserRole.SPECIALIST, UserStatus.ACTIVE);
		
		appSpec = new Specialist(appUser, "Ann", "Rid", Duration.ZERO, Duration.ZERO);
		susSpec = new Specialist(susUser, "Kate", "Jones", Duration.ZERO, Duration.ZERO);
		rejSpec = new Specialist(rejUser, "Hanna", "Smith", Duration.ZERO, Duration.ZERO);
		penSpec = new Specialist(penUser, "Mary", "Williams", Duration.ZERO, Duration.ZERO);
		
		appSpec.approve();
		
		susSpec.approve();
		susSpec.suspend();
		
		rejSpec.reject();
	}
	
	@Test
	public void shouldRejectNullUserOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(null, "First", "Last", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("User cannot be null", exception.getMessage());
	}
	
	@Test
	public void shouldCreateSpecialistWithZeroBookingNotices() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		
		assertEquals("First", specialist.getFirstName());
		assertEquals("Last", specialist.getLastName());
		assertEquals(Duration.ZERO, specialist.getMinimumBookingNotice());
		assertEquals(Duration.ZERO, specialist.getClientCancellationNotice());
		assertEquals(SpecialistStatus.PENDING, specialist.getApprovalStatus());
	}
	
	@Test
	public void shouldCreateSpecialistWithPositiveBookingNotices() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ofHours(2), Duration.ofHours(24));
		
		assertEquals(Duration.ofHours(2), specialist.getMinimumBookingNotice());
		assertEquals(Duration.ofHours(24), specialist.getClientCancellationNotice());
	}
	
	@Test
	public void shouldRejectUserWithoutSpecialistRoleOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class, 
				() -> new Specialist(client, "Mary", "Poppins", Duration.ZERO, Duration.ZERO));
		assertEquals("User must have SPECIALIST role", exception.getMessage());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class, 
				() -> new Specialist(admin, "Tony", "Wills", Duration.ZERO, Duration.ZERO));
		assertEquals("User must have SPECIALIST role", excep.getMessage());
	}
	
	@Test
	public void shouldRejectInvalidFirstNameOnCreation() {
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "   ", "Last", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("First name cannot be blank", except.getMessage());
		
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, null, "Last", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("First name cannot be blank", exception.getMessage());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "", "Last", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("First name cannot be blank", excep.getMessage());
	}
	
	@Test
	public void shouldRejectInvalidLastNameOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "    ", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Last name cannot be blank", exception.getMessage());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "", Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Last name cannot be blank", except.getMessage());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", null, Duration.ZERO, Duration.ZERO));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("Last name cannot be blank", excep.getMessage());
	}
	
	@Test
	public void shouldRejectInvalidMinimumBookingNoticeOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", null, Duration.ofHours(24)));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Minimum booking notice cannot be null", exception.getMessage());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofMinutes(-1), Duration.ofHours(24)));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Minimum booking notice cannot be negative", except.getMessage());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofMinutes(5).plusNanos(1), Duration.ofHours(24)));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("Minimum booking notice must contain whole minutes", excep.getMessage());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofMinutes(Integer.MAX_VALUE + 1L), Duration.ofHours(24)));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("Minimum booking notice exceeds supported range", ex.getMessage());
	}
	
	@Test
	public void shouldRejectInvalidClientCancellationNoticeOnCreation() {
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofHours(2), null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Client cancellation notice cannot be null", exception.getMessage());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofHours(2), Duration.ofMinutes(-1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Client cancellation notice cannot be negative", except.getMessage());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofHours(2), Duration.ofMinutes(5).plusSeconds(1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("Client cancellation notice must contain whole minutes", excep.getMessage());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> new Specialist(user, "First", "Last", Duration.ofHours(2), Duration.ofMinutes(Integer.MAX_VALUE + 1L)));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("Client cancellation notice exceeds supported range", ex.getMessage());
	}
	
	@Test
	public void shouldChangeFirstName() {
		String newName = "Lili";
		
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		specialist.changeFirstName(newName);
		
		assertEquals(newName, specialist.getFirstName());
	}
	
	@Test
	public void shouldRejectInvalidFirstNameOnChange() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeFirstName(null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("First name cannot be blank", exception.getMessage());
		assertEquals("First", specialist.getFirstName());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeFirstName(""));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("First name cannot be blank", except.getMessage());
		assertEquals("First", specialist.getFirstName());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeFirstName("   "));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("First name cannot be blank", ex.getMessage());
		assertEquals("First", specialist.getFirstName());
	}
	
	@Test
	public void shouldChangeLastName() {
		String newName = "Brown";
		
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		specialist.changeLastName(newName);
		
		assertEquals(newName, specialist.getLastName());
	}
	
	@Test
	public void shouldRejectInvalidLastNameOnChange() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeLastName(null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Last name cannot be blank", exception.getMessage());
		assertEquals("Last", specialist.getLastName());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeLastName(""));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Last name cannot be blank", except.getMessage());
		assertEquals("Last", specialist.getLastName());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeLastName("   "));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("Last name cannot be blank", ex.getMessage());
		assertEquals("Last", specialist.getLastName());
	}
	
	@Test
	public void shouldChangeMinimumBookingNotice() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		Duration newDuration = Duration.ofMinutes(5);
		
		specialist.changeMinimumBookingNotice(newDuration);
		
		assertEquals(newDuration, specialist.getMinimumBookingNotice());
	}
	
	@Test
	public void shouldRejectInvalidMinimumBookingNoticeOnChange() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeMinimumBookingNotice(null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Minimum booking notice cannot be null", exception.getMessage());
		assertEquals(Duration.ZERO, specialist.getMinimumBookingNotice());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeMinimumBookingNotice(Duration.ofMinutes(-1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Minimum booking notice cannot be negative", except.getMessage());
		assertEquals(Duration.ZERO, specialist.getMinimumBookingNotice());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeMinimumBookingNotice(Duration.ofMinutes(5).plusSeconds(1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("Minimum booking notice must contain whole minutes", excep.getMessage());
		assertEquals(Duration.ZERO, specialist.getMinimumBookingNotice());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeMinimumBookingNotice(Duration.ofMinutes(Integer.MAX_VALUE + 1L)));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("Minimum booking notice exceeds supported range", ex.getMessage());
		assertEquals(Duration.ZERO, specialist.getMinimumBookingNotice());
		
	}
	
	@Test
	public void shouldChangeClientCancellationNotice() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		Duration newDuration = Duration.ofMinutes(5);
		
		specialist.changeClientCancellationNotice(newDuration);
		
		assertEquals(newDuration, specialist.getClientCancellationNotice());
	}
	
	@Test
	public void shouldRejectInvalidClientCancellationNoticeOnChange() {
		Specialist specialist = new Specialist(user, "First", "Last", Duration.ZERO, Duration.ZERO);
		
		InvalidSpecialistDataException exception = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeClientCancellationNotice(null));
		
		assertEquals("SPECIALIST_INVALID_DATA", exception.code());
		assertEquals("Client cancellation notice cannot be null", exception.getMessage());
		assertEquals(Duration.ZERO, specialist.getClientCancellationNotice());
		
		InvalidSpecialistDataException except = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeClientCancellationNotice(Duration.ofMinutes(-1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", except.code());
		assertEquals("Client cancellation notice cannot be negative", except.getMessage());
		assertEquals(Duration.ZERO, specialist.getClientCancellationNotice());
		
		InvalidSpecialistDataException excep = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeClientCancellationNotice(Duration.ofMinutes(5).plusNanos(1)));
		
		assertEquals("SPECIALIST_INVALID_DATA", excep.code());
		assertEquals("Client cancellation notice must contain whole minutes", excep.getMessage());
		assertEquals(Duration.ZERO, specialist.getClientCancellationNotice());
		
		InvalidSpecialistDataException ex = assertThrows(InvalidSpecialistDataException.class,
				() -> specialist.changeClientCancellationNotice(Duration.ofMinutes(Integer.MAX_VALUE + 1L)));
		
		assertEquals("SPECIALIST_INVALID_DATA", ex.code());
		assertEquals("Client cancellation notice exceeds supported range", ex.getMessage());
		assertEquals(Duration.ZERO, specialist.getClientCancellationNotice());
	}
	
	@Test
	public void shouldApproveSpecialist() {		
		penSpec.approve();
		
		assertEquals(SpecialistStatus.APPROVED, penSpec.getApprovalStatus());
	}
	
	@Test
	public void shouldRejectSpecialist() {
		penSpec.reject();
		
		assertEquals(SpecialistStatus.REJECTED, penSpec.getApprovalStatus());
	}
	
	@Test
	public void shouldResubmitSpecialist() {
		rejSpec.resubmit();
		
		assertEquals(SpecialistStatus.PENDING, rejSpec.getApprovalStatus());
	}
	
	@Test
	public void shouldSuspendSpecialist() {
		appSpec.suspend();
		
		assertEquals(SpecialistStatus.SUSPENDED, appSpec.getApprovalStatus());
	}
	
	@Test
	public void shouldReinstateSpecialist() {
		susSpec.reinstate();
		
		assertEquals(SpecialistStatus.APPROVED, susSpec.getApprovalStatus());
	}
}