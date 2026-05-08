package com.quickbite.auth_service.repository;

import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.entity.User.Role;
import com.quickbite.auth_service.entity.User.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository
 *
 * Methods used by:
 *  - AuthServiceImpl           → login, register, validateToken
 *  - AdminController (future)  → findAllByRole, suspend, delete
 *  - Other services (Feign)    → findByUserId (restaurant-service, delivery-service, notification-service)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // ── Authentication ────────────────────────────────────────────────────────

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── OAuth2 Login ──────────────────────────────────────────────────────────

    /**
     * Called by OAuth2SuccessHandler to find or create user after Google/GitHub login.
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    // ── Phone Lookup ──────────────────────────────────────────────────────────
    // Used by customer profile update to check phone uniqueness

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    // ── Admin Operations ──────────────────────────────────────────────────────

    List<User> findAllByRole(Role role);

    List<User> findAllByIsActive(Boolean isActive);

    /**
     * Admin: search users by name (partial match, case-insensitive).
     * Used in admin dashboard user management.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByFullNameContaining(@Param("name") String name);

    /**
     * Admin: search by role AND active status.
     * E.g. find all active AGENT users.
     */
    List<User> findAllByRoleAndIsActive(Role role, Boolean isActive);

    // ── Soft Delete / Suspend ─────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.isActive = :status WHERE u.userId = :userId")
    int updateActiveStatus(@Param("userId") Integer userId, @Param("status") Boolean status);

    // ── Feign Client Methods (called by other microservices) ──────────────────

    /**
     * restaurant-service: resolve ownerId → User (to get name, email, phone)
     * delivery-service: resolve agentId → User
     * notification-service: get email for sending notifications
     */
    Optional<User> findByUserId(Integer userId);

    /**
     * Count users per role — used by Admin analytics dashboard.
     */
    long countByRole(Role role);

    long countByRoleAndIsActive(Role role, Boolean isActive);
}
