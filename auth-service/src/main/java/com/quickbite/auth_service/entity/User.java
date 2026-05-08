package com.quickbite.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User Entity
 *
 * Central user table shared across all roles.
 * Role field determines which microservices a user can access:
 *   - CUSTOMER    → cart-service, order-service, payment-service, review-service
 *   - OWNER       → restaurant-service, menu-service, order-service (incoming)
 *   - AGENT       → delivery-service, order-service (assigned orders)
 *   - ADMIN       → all services (full platform management)
 *
 * provider field distinguishes OAuth2 users (GOOGLE/GITHUB) from LOCAL users.
 * providerId stores the OAuth2 sub/id so we never duplicate accounts.
 *
 * FUTURE CHANGES when other services are added:
 *   - restaurant-service: will call GET /api/v1/auth/users/{id} to resolve ownerId -> User
 *   - order-service: will call validateToken endpoint to authenticate requests
 *   - delivery-service: will call GET /api/v1/auth/users/{id} to get agent name/phone
 *   - notification-service: will use email field from this entity
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uq_user_email"),
        @UniqueConstraint(columnNames = {"provider", "provider_id"}, name = "uq_provider_id")
    },
    indexes = {
        @Index(columnList = "email", name = "idx_user_email"),
        @Index(columnList = "role",  name = "idx_user_role"),
        @Index(columnList = "phone", name = "idx_user_phone")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * Null for OAuth2 users (they login via provider, no local password).
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 15)
    private String phone;

    /**
     * Enum stored as String for readability in DB.
     * Values: CUSTOMER | OWNER | AGENT | ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * OAuth2 provider: LOCAL | GOOGLE | GITHUB
     * Default is LOCAL for email/password registration.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * Subject/ID from OAuth2 provider. Null for LOCAL users.
     * Used to prevent duplicate account creation on repeated OAuth2 logins.
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /**
     * Soft-delete / suspension flag.
     * false = suspended/deactivated (Admin can suspend; user can self-deactivate)
     * true  = active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "profile_pic_url", length = 500)
    private String profilePicUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Role Enum ────────────────────────────────────────────────────────────

    public enum Role {
        CUSTOMER,
        OWNER,
        AGENT,
        ADMIN
    }

    // ─── Provider Enum ────────────────────────────────────────────────────────

    public enum AuthProvider {
        LOCAL,
        GOOGLE,
        GITHUB
    }
}
