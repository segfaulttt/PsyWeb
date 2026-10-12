package com.psyweb.user.web.dto.response;

import com.psyweb.user.domain.UserRole;

public record UserAccountResponse(Long id, String email, UserRole role) {
}