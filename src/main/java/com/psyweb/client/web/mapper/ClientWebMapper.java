package com.psyweb.client.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.client.domain.Client;
import com.psyweb.client.web.dto.response.ClientProfileResponse;

@Component
public class ClientWebMapper {

    public ClientProfileResponse toProfileResponse(Client client) {
        return new ClientProfileResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName()
        );
    }
}