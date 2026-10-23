package com.psyweb.auth.service.dto;

public record Success(IssuedRefreshToken issuedRefreshToken) implements RefreshRotationResult {

}
