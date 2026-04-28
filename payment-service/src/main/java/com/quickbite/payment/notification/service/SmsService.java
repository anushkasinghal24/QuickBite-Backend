package com.quickbite.payment.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SmsService â€” SMS dispatch via Twilio / AWS SNS.
 *
 * PDF Section 2.7: "Multi-channel: SMS (Twilio/AWS SNS)"
 * PDF Section 4.9: NotificationServiceImpl uses smsSender: SMSGateway
 *
 * Development mode: sms.provider=mock â†’ logs SMS instead of sending.
 * Production: sms.provider=twilio â†’ uses Twilio SDK.
 *
 * HOW TO ADD TWILIO:
 * 1. Add dependency to pom.xml:
 *    <dependency>
 *      <groupId>com.twilio.sdk</groupId>
 *      <artifactId>twilio</artifactId>
 *      <version>9.14.0</version>
 *    </dependency>
 * 2. Uncomment the Twilio block in sendSms() below.
 * 3. Set sms.twilio.account-sid, auth-token, from-number in application.yml.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${sms.provider:mock}")
    private String provider;

    @Value("${sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    /**
     * Send SMS asynchronously.
     *
     * @param toPhone  recipient phone in E.164 format: +91XXXXXXXXXX
     * @param message  SMS body (max 160 chars for single SMS)
     */
    @Async("notificationExecutor")
    public void sendSms(String toPhone, String message) {
        if (!smsEnabled || toPhone == null || toPhone.isBlank()) {
            log.info("[SMS MOCK] To={}: {}", toPhone, message);
            return;
        }

        if ("twilio".equalsIgnoreCase(provider)) {
            sendViaTwilio(toPhone, message);
        } else {
            // Mock â€” log only (safe for dev/test)
            log.info("[SMS MOCK] To={}: {}", toPhone, message);
        }
    }

    private void sendViaTwilio(String toPhone, String message) {
        try {
            /*
             * UNCOMMENT BELOW WHEN TWILIO DEPENDENCY IS ADDED:
             *
             * Twilio.init(twilioAccountSid, twilioAuthToken);
             * Message.creator(
             *     new PhoneNumber(toPhone),
             *     new PhoneNumber(twilioFromNumber),
             *     message
             * ).create();
             * log.info("Twilio SMS sent to: {}", toPhone);
             */
            log.info("[TWILIO SMS] Would send to={}: {}", toPhone, message);
        } catch (Exception e) {
            log.error("Twilio SMS failed to {}: {}", toPhone, e.getMessage());
            // Never propagate â€” SMS failure should not crash order flow
        }
    }

    /**
     * Build order status SMS (keep under 160 chars).
     */
    public String buildOrderSms(Long orderId, String status) {
        return String.format("QuickBite: Order #%d is now %s. Track on app.", orderId, status);
    }
}
