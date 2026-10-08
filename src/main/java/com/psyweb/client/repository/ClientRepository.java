package com.psyweb.client.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psyweb.client.domain.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

}
