package com.quickbite.auth_service.service.impl;

import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserDetailsServiceImpl
 *
 * Bridge between Spring Security and our User entity.
 * Loads user from DB by email for authentication.
 *
 * Role is prefixed with "ROLE_" so Spring Security @PreAuthorize works:
 *   @PreAuthorize("hasRole('ADMIN')")  → checks "ROLE_ADMIN"
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(!user.getIsActive())   // Suspended users are locked
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }
}
