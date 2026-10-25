package com.example.employees.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Off by default so the project runs with zero setup. Flip app.mail.enabled=true
    // and fill in real spring.mail.* settings (see application.properties) to send for real.
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendApproval(String to, String name) {
        String subject = "Your details have been approved";
        String body = "Hi " + name + ",\n\n"
                + "Good news - a manager has reviewed and approved the details you submitted. "
                + "Your employee record has been created and saved.\n\nWelcome aboard!";
        send(to, subject, body);
    }

    public void sendRejection(String to, String name, String reason) {
        String subject = "Update on your application";
        String body = "Hi " + name + ",\n\n"
                + "A manager has reviewed the details you submitted, and it was not approved at this time."
                + (reason != null && !reason.isBlank() ? ("\n\nReason: " + reason) : "")
                + "\n\nYou're welcome to reach out with any questions.";
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            // No real mail server configured - log what WOULD have been sent instead of crashing.
            log.info(">> [EMAIL not actually sent - app.mail.enabled=false] To: {} | Subject: {} | Body: {}",
                    to, subject, body.replace("\n", " "));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info(">> Email sent to {}", to);
        } catch (Exception ex) {
            log.error("Could not send email to {}: {}", to, ex.getMessage());
        }
    }
}
