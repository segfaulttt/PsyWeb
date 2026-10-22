package com.psyweb.auth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.auth.domain.RefreshSession;

import jakarta.persistence.LockModeType;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID>{
	
	@Query(
			"""
			SELECT s
			FROM RefreshSession s
			WHERE s.user.id = :userId
			"""
			)
	List<RefreshSession> findByUserId(@Param("userId")Long userId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(
			"""
			SELECT s
			FROM RefreshSession s
			WHERE s.id = :id
			"""
			)
	Optional<RefreshSession> findForRefreshById(@Param("id")UUID id);
	
	@Query(
			"""
			SELECT s
			FROM RefreshSession s
			WHERE s.expiresAt <= :cleanupDeadline
			"""
			)
	List<RefreshSession> findForCleanup(@Param("cleanupDeadline") Instant cleanupDeadline);
}
