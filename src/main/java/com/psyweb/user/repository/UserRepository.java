package com.psyweb.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.psyweb.user.domain.User;

public interface UserRepository extends JpaRepository<User, Long>{
}
