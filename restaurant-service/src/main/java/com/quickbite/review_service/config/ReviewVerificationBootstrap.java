package com.quickbite.review_service.config;

import com.quickbite.review_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Keeps legacy review rows from showing as pending in admin views.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewVerificationBootstrap implements CommandLineRunner {

    private final ReviewRepository reviewRepository;

    @Override
    public void run(String... args) {
        int updated = reviewRepository.markAllAsVerified();
        if (updated > 0) {
            log.info("Auto-verified {} legacy review row(s) on startup", updated);
        }
    }
}
