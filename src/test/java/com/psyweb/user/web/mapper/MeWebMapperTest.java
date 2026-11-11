package com.psyweb.user.web.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.dto.UpdateCurrentProfileCommand;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MeWebMapperTest {

	private MeWebMapper mapper;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mapper = new MeWebMapper();
		objectMapper = new ObjectMapper();
	}

	private JsonNode parse(String json) {
		return objectMapper.readTree(json);
	}

	@Test
	void shouldMapOnlyFirstName() {
		JsonNode json = parse("""
				{"firstName": "Anna"}
				""");

		UpdateCurrentProfileCommand command = mapper.toUpdateCommand(json);

		assertEquals("Anna", command.firstName());
		assertNull(command.lastName());
	}

	@Test
	void shouldMapOnlyLastName() {
		JsonNode json = parse("""
				{"lastName": "Smith"}
				""");

		UpdateCurrentProfileCommand command = mapper.toUpdateCommand(json);

		assertNull(command.firstName());
		assertEquals("Smith", command.lastName());
	}

	@Test
	void shouldMapBothFields() {
		JsonNode json = parse("""
				{
				    "firstName": "Anna",
				    "lastName": "Smith"
				}
				""");

		UpdateCurrentProfileCommand command = mapper.toUpdateCommand(json);

		assertEquals("Anna", command.firstName());
		assertEquals("Smith", command.lastName());
	}

	@Test
	void shouldRejectNullBody() {
		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(null));
	}

	@Test
	void shouldRejectEmptyObject() {
		JsonNode json = parse("{}");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldRejectArrayAsRoot() {
		JsonNode json = parse("""
				[{"firstName": "Anna"}]
				""");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldRejectUnknownField() {
		JsonNode json = parse("""
				{
				    "firstName": "Anna",
				    "role": "ADMIN"
				}
				""");

		InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
				() -> mapper.toUpdateCommand(json));

		assertEquals("Unknown profile field: role", exception.getMessage());
	}

	@Test
	void shouldRejectExplicitNullField() {
		JsonNode json = parse("""
				{"firstName": null}
				""");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldRejectNumericField() {
		JsonNode json = parse("""
				{"firstName": 123}
				""");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldRejectBooleanField() {
		JsonNode json = parse("""
				{"firstName": true}
				""");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldRejectNestedObject() {
		JsonNode json = parse("""
				{"firstName": {"value": "Anna"}}
				""");

		assertThrows(InvalidUserDataException.class, () -> mapper.toUpdateCommand(json));
	}

	@Test
	void shouldPreserveEmptyStringForDomainValidation() {
		JsonNode json = parse("""
				{"firstName": ""}
				""");

		UpdateCurrentProfileCommand command = mapper.toUpdateCommand(json);

		assertEquals("", command.firstName());
		assertNull(command.lastName());
	}
}