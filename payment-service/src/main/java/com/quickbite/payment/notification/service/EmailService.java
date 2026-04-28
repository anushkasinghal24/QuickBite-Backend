package com.quickbite.payment.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

/**
 * EmailService â€” JavaMailSender implementation.
 *
 * PDF Section 2.7: "Multi-channel: in-app notification centre, email (JavaMailSender)"
 * PDF Section 4.9: NotificationServiceImpl uses emailSender: JavaMailSender
 *
 * All methods are @Async â€” email sending does NOT block the calling thread.
 * Uses the threadpool defined in AppConfig.
 *
 * In development: set spring.mail.host=localhost and use MailHog/Mailtrap.
 * In production: use Gmail SMTP or SendGrid/AWS SES.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Value("${notification.email-enabled:true}")
    private boolean emailEnabled;

    /**
     * Send a plain text email asynchronously.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param body    plain text body
     */
    @Async("notificationExecutor")
    public void sendSimpleEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL MOCK] To={}, Subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw â€” email failure should never crash the calling service
        }
    }

    /**
     * Send an HTML email (rich format for order receipts, etc.).
     */
    @Async("notificationExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("[EMAIL HTML MOCK] To={}, Subject={}", to, subject);
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  // true = isHtml
            mailSender.send(mimeMessage);
            log.info("HTML email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Build order status email body.
     */
    public String buildOrderStatusEmailBody(String customerName, String status,
                                             Long orderId, String message) {
        return String.format("""
                Dear %s,
                
                %s
                
                Order ID: #%d
                Status: %s
                
                Thank you for choosing QuickBite!
                
                â€” QuickBite Team
                """, customerName, message, orderId, status);
    }
}
