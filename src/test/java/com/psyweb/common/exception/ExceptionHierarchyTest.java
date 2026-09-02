package com.psyweb.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;


public class ExceptionHierarchyTest {
	@Test
	public void shouldExtendBaseDomainException() {
		assertEquals(BaseDomainException.class, ValidationException.class.getSuperclass());
		assertEquals(BaseDomainException.class, NotFoundException.class.getSuperclass());
		assertEquals(BaseDomainException.class, ConflictException.class.getSuperclass());
		assertEquals(BaseDomainException.class, ForbiddenException.class.getSuperclass());
		assertEquals(BaseDomainException.class, InvalidStateException.class.getSuperclass());
		assertEquals(BaseDomainException.class, ExpiredException.class.getSuperclass());
	}
	
	@Test
	public void shouldDefineCategoryExceptionsAsAbstract() {
		assertTrue(Modifier.isAbstract(ValidationException.class.getModifiers()));
		assertTrue(Modifier.isAbstract(NotFoundException.class.getModifiers()));
		assertTrue(Modifier.isAbstract(ConflictException.class.getModifiers()));
		assertTrue(Modifier.isAbstract(ForbiddenException.class.getModifiers()));
		assertTrue(Modifier.isAbstract(InvalidStateException.class.getModifiers()));
		assertTrue(Modifier.isAbstract(ExpiredException.class.getModifiers()));
	}
}
