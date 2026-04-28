package com.quickbite.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * RefreshToken Entity
 *
 * Stores long-lived refresh tokens in DB so we can:
 *  - Invalidate on logout (delete record)
 *  - Support token rotation on refresh
 *  - Detect reuse attacks
 *
 * Each user can have ONE active refresh token.
 * On logout → token record is deleted.
 * On refresh → old token is replaced with new one.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(columnList = "token",   name = "idx_rt_token"),
        @Index(columnList = "user_id", name = "idx_rt_user")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The actual UUID-based refresh token string.
     * Stored as plain text (not sensitive like access token).
     */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /**
     * Owner of this refresh token.
     * One-to-one: each user has at most one active refresh token.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    /**
     * Expiry time. Checked on every refresh request.
     * Default: 7 days (configurable in application.yml)
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public boolean isExpired() {
        return expiryDate.compareTo(Instant.now()) < 0;
    }
}
