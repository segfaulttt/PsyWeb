package com.psyweb.common.web.exception;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.psyweb.common.exception.BaseDomainException;
import com.psyweb.common.exception.ConflictException;
import com.psyweb.common.exception.ExpiredException;
import com.psyweb.common.exception.ForbiddenException;
import com.psyweb.common.exception.InvalidStateException;
import com.psyweb.common.exception.NotFoundException;
import com.psyweb.common.exception.ValidationException;
import com.psyweb.common.web.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private final Clock clock;
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public GlobalExceptionHandler(Clock clock) {
		this.clock = clock;
	}
	
	private ApiError createApiError(BaseDomainException exception, HttpServletRequest request) {
		return new ApiError(exception.code(), exception.getMessage(), request.getRequestURI(),
				Instant.now(clock));
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiError> handleValidation(ValidationException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(InvalidStateException.class)
	public ResponseEntity<ApiError> handleInvalidState(InvalidStateException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiError> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(ExpiredException.class)
	public ResponseEntity<ApiError> handleExpired(ExpiredException exception, HttpServletRequest request) {
		ApiError error = createApiError(exception, request);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unexpected exception while processing request {}", request.getRequestURI(), exception);

		ApiError error = new ApiError("INTERNAL_SERVER_ERROR", "Internal server error", request.getRequestURI(),
				Instant.now(clock));
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
