package com.psyweb.common.web.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.psyweb.common.exception.ConflictException;
import com.psyweb.common.exception.ExpiredException;
import com.psyweb.common.exception.ForbiddenException;
import com.psyweb.common.exception.InvalidStateException;
import com.psyweb.common.exception.NotFoundException;
import com.psyweb.common.exception.ValidationException;
import com.psyweb.common.web.dto.ApiError;
import com.psyweb.common.web.dto.ValidationApiError;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

	private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");
	private static final String PATH = "/api/test";

	private GlobalExceptionHandler handler;
	private HttpServletRequest request;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		handler = new GlobalExceptionHandler(clock);

		request = mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn(PATH);
	}

	@Test
	void shouldHandleNotFoundException() {
		NotFoundException exception = new TestNotFoundException("TEST_NOT_FOUND", "Test resource not found");

		ResponseEntity<ApiError> response = handler.handleNotFound(exception, request);

		assertApiError(response, HttpStatus.NOT_FOUND, "TEST_NOT_FOUND", "Test resource not found");
	}

	@Test
	void shouldHandleValidationException() {
		ValidationException exception = new TestValidationException("TEST_INVALID_DATA", "Test data is invalid");

		ResponseEntity<ApiError> response = handler.handleValidation(exception, request);

		assertApiError(response, HttpStatus.BAD_REQUEST, "TEST_INVALID_DATA", "Test data is invalid");
	}

	@Test
	void shouldHandleInvalidStateException() {
		InvalidStateException exception = new TestInvalidStateException("TEST_INVALID_STATE", "Test state is invalid");

		ResponseEntity<ApiError> response = handler.handleInvalidState(exception, request);

		assertApiError(response, HttpStatus.CONFLICT, "TEST_INVALID_STATE", "Test state is invalid");
	}

	@Test
	void shouldHandleConflictException() {
		ConflictException exception = new TestConflictException("TEST_CONFLICT", "Test conflict");

		ResponseEntity<ApiError> response = handler.handleConflict(exception, request);

		assertApiError(response, HttpStatus.CONFLICT, "TEST_CONFLICT", "Test conflict");
	}

	@Test
	void shouldHandleForbiddenException() {
		ForbiddenException exception = new TestForbiddenException("TEST_FORBIDDEN", "Test access is forbidden");

		ResponseEntity<ApiError> response = handler.handleForbidden(exception, request);

		assertApiError(response, HttpStatus.FORBIDDEN, "TEST_FORBIDDEN", "Test access is forbidden");
	}

	@Test
	void shouldHandleExpiredException() {
		ExpiredException exception = new TestExpiredException("TEST_EXPIRED", "Test resource expired");

		ResponseEntity<ApiError> response = handler.handleExpired(exception, request);

		assertApiError(response, HttpStatus.CONFLICT, "TEST_EXPIRED", "Test resource expired");
	}

	@Test
	void shouldHandleUnexpectedExceptionWithoutExposingInternalMessage() {
		Exception exception = new RuntimeException("database password is secret");

		ResponseEntity<ApiError> response = handler.handleUnexpectedException(exception, request);

		assertApiError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error");
	}

	@Test
	void shouldHandleMethodArgumentNotValidException() {
		Object target = new Object();

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");

		bindingResult.addError(new FieldError("request", "email", "must not be blank"));

		bindingResult.addError(new FieldError("request", "email", "must be a well-formed email address"));

		bindingResult.addError(new FieldError("request", "firstName", "must not be blank"));

		MethodParameter methodParameter = mock(MethodParameter.class);

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

		ResponseEntity<ValidationApiError> response = handler.handleMethodArgumentNotValid(exception, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ValidationApiError body = response.getBody();

		assertNotNull(body);
		assertEquals("VALIDATION_ERROR", body.code());
		assertEquals("Request validation failed", body.message());
		assertEquals(PATH, body.path());
		assertEquals(NOW, body.timestamp());

		assertEquals(List.of("must not be blank", "must be a well-formed email address"),
				body.fieldErrors().get("email"));

		assertEquals(List.of("must not be blank"), body.fieldErrors().get("firstName"));

		assertEquals(2, body.fieldErrors().size());
	}

	private void assertApiError(ResponseEntity<ApiError> response, HttpStatus expectedStatus, String expectedCode,
			String expectedMessage) {
		assertEquals(expectedStatus, response.getStatusCode());

		ApiError body = response.getBody();

		assertNotNull(body);
		assertEquals(expectedCode, body.code());
		assertEquals(expectedMessage, body.message());
		assertEquals(PATH, body.path());
		assertEquals(NOW, body.timestamp());
	}

	private static class TestNotFoundException extends NotFoundException {
		TestNotFoundException(String code, String message) {
			super(code, message);
		}
	}

	private static class TestValidationException extends ValidationException {
		TestValidationException(String code, String message) {
			super(code, message);
		}
	}

	private static class TestInvalidStateException extends InvalidStateException {
		TestInvalidStateException(String code, String message) {
			super(code, message);
		}
	}

	private static class TestConflictException extends ConflictException {
		TestConflictException(String code, String message) {
			super(code, message);
		}
	}

	private static class TestForbiddenException extends ForbiddenException {
		TestForbiddenException(String code, String message) {
			super(code, message);
		}
	}

	private static class TestExpiredException extends ExpiredException {
		TestExpiredException(String code, String message) {
			super(code, message);
		}
	}
}