package com.psyweb.user.web.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.psyweb.user.service.CurrentUserProfileService;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.web.dto.response.ClientMeResponse;
import com.psyweb.user.web.dto.response.MeResponse;
import com.psyweb.user.web.dto.response.SpecialistMeResponse;
import com.psyweb.user.web.mapper.MeWebMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;

@Tag(name = "Profile endpoints")
@RestController
public class MeController {
	private final CurrentUserProfileService currentUserProfileService;
	private final MeWebMapper meWebMapper;

	public MeController(CurrentUserProfileService currentUserProfileService, MeWebMapper meWebMapper) {
		this.currentUserProfileService = currentUserProfileService;
		this.meWebMapper = meWebMapper;
	}

	@Operation(summary = "Get current user profile", description = "Returns the account and profile information of the authenticated user.", security = @SecurityRequirement(name = "Bearer auth"))
	@ApiResponse(responseCode = "200", description = "Current user profile retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {
			ClientMeResponse.class, SpecialistMeResponse.class })))
	@ApiResponse(responseCode = "401", description = "Authentication required or invalid access token")
	@ApiResponse(responseCode = "403", description = "User account is not active")
	@GetMapping("/api/me")
	public ResponseEntity<MeResponse> profile() {
		CurrentUserProfile profile = currentUserProfileService.getCurrentUserProfile();
		MeResponse response = meWebMapper.toResponse(profile);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
	}

	@Operation(summary = "Update current user profile", description = "Partially updates the authenticated user's first and/or last name.", security = @SecurityRequirement(name = "Bearer auth"))
	@ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {
			ClientMeResponse.class, SpecialistMeResponse.class })))
	@ApiResponse(responseCode = "400", description = "Invalid profile update request")
	@ApiResponse(responseCode = "401", description = "Authentication required or invalid access token")
	@ApiResponse(responseCode = "403", description = "User account is not active")
	@PatchMapping(value = "/api/me/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MeResponse> updateProfile(@RequestBody JsonNode json) {

		CurrentUserProfile profile = currentUserProfileService.updateCurrentProfile(meWebMapper.toUpdateCommand(json));
		MeResponse response = meWebMapper.toResponse(profile);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
	}
}
