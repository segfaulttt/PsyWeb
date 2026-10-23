package com.psyweb.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.auth.domain.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID>{
	
	@Query(
			"""
			SELECT t
			FROM RefreshToken t
			WHERE t.tokenHash = :tokenHash
			"""
			)
	Optional<RefreshToken> findByTokenHash(@Param("tokenHash")String tokenHash);
	
	@Query(
			"""
			SELECT t
			FROM RefreshToken t
			WHERE t.session.id = :sessionId
			"""
			)
	List<RefreshToken> findBySessionId(@Param("sessionId")UUID sessionId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(
			"""
			SELECT t
			FROM RefreshToken t
			WHERE t.id = :id
			"""
			)
	Optional<RefreshToken> findForRotationById(@Param("id") UUID id);
}
