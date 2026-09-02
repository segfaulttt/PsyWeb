package com.psyweb.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BaseDomainExceptionTest {
	
	@Test
	public void shouldExposeCodeAndMessage() {
		TestDomainException exception = new TestDomainException("TEST_ERROR", "Test message");
		
		assertEquals("TEST_ERROR", exception.code());
		assertEquals("Test message", exception.getMessage());
		assertNull(exception.getCause());
	}
	
	@Test
	public void shouldExposeCause() {
		Throwable cause = new RuntimeException("Original error");
		TestDomainException exception = new TestDomainException("TEST_ERROR", "Test_message", cause);
		assertSame(cause, exception.getCause());
	}
	
	@Test
	public void shouldImplementDomainExceptionContract() {
		TestDomainException exception = new TestDomainException("TEST_ERROR", "Test_message");
		assertInstanceOf(DomainException.class, exception);
	}
	
	
	
	public static final class TestDomainException extends BaseDomainException{
		protected TestDomainException(String code, String message) {
			super(code, message);
		}
		
		protected TestDomainException(String code, String message, Throwable cause) {
			super(code, message, cause);
		}
	}
	
}