package com.vesta.api.auth;

import com.vesta.api.common.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender sender;
    private final MailProperties properties;

    public MailService(JavaMailSender sender, MailProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    void sendVerification(String recipient, String token) {
        send(recipient, "Verify your Vesta email",
                "Verify your email: " + properties.frontendUrl() + "/verify-email?token=" + token);
    }

    void sendPasswordReset(String recipient, String token) {
        send(recipient, "Reset your Vesta password",
                "Reset your password: " + properties.frontendUrl() + "/reset-password?token=" + token);
    }

    private void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Account email delivery failed; token and recipient were not logged");
        }
    }
}

