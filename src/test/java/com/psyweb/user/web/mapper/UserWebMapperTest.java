package com.psyweb.user.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.web.dto.response.UserAccountResponse;

class UserWebMapperTest {

    private final UserWebMapper mapper = new UserWebMapper();
    
    @Test
    void shouldMapUserToAccountResponse() {
        User user = new User(
                "client@example.com",
                "password-hash",
                UserRole.CLIENT,
                UserStatus.ACTIVE
        );

        UserAccountResponse response = mapper.toAccountResponse(user);

        assertEquals(user.getId(), response.id());
        assertEquals("client@example.com", response.email());
        assertEquals(UserRole.CLIENT, response.role());
    }
}