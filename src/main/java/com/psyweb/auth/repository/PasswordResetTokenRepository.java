package com.psyweb.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psyweb.auth.domain.PasswordResetToken;

import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	@Query("""
			SELECT t
			FROM PasswordResetToken t
			WHERE t.tokenHash = :tokenHash
			""")
	Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT t
			FROM PasswordResetToken t
			WHERE t.id = :id
			""")
	Optional<PasswordResetToken> findForUpdateById(@Param("id") UUID id);

	@Query("""
			SELECT t
			FROM PasswordResetToken t
			WHERE t.user.id = :userId
			  AND t.consumedAt IS NULL
			  AND t.revokedAt IS NULL
			""")
	List<PasswordResetToken> findUnresolvedByUserId(@Param("userId") Long userId);
}