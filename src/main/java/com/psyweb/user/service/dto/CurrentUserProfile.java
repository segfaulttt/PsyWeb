package com.psyweb.user.service.dto;

public sealed interface CurrentUserProfile permits CurrentClientProfile, CurrentSpecialistProfile {

}
