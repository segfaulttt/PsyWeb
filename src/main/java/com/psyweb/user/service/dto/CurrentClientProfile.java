package com.psyweb.user.service.dto;

import com.psyweb.client.domain.Client;
import com.psyweb.user.domain.User;

public record CurrentClientProfile(User user, Client client) implements CurrentUserProfile {

}
