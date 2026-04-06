package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private MailConfigService mailConfigService;

    public boolean sendEmail(String subject, String body) {
        logger.info("Attempting to send email to {} with subject: {}", 
                    mailConfigService.getCurrentMailConfig().map(c -> c.getToEmail()).orElse("unknown"), 
                    subject);
        
        try {
            MailConfig config = mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

            // Create JavaMailSender
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            
            // SMTP server settings
            mailSender.setHost(config.getHost());
            mailSender.setPort(config.getPort());
            
            // Authentication - only if username is provided
            if (config.getUsername() != null && !config.getUsername().trim().isEmpty()) {
                mailSender.setUsername(config.getUsername());
                mailSender.setPassword(config.getPassword());
                logger.info("Using SMTP authentication with username: {}", config.getUsername());
            } else {
                logger.info("Using SMTP without authentication");
            }
            
            // SMTP properties - simple configuration for mail relay without auth
            Properties props = mailSender.getJavaMailProperties();
            
            // Only enable auth if username is provided
            if (config.getUsername() != null && !config.getUsername().trim().isEmpty()) {
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "false");
                props.put("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.ssl.checkserveridentity", "false");
            } else {
                // No auth - plain SMTP relay (like port 25)
                props.put("mail.smtp.auth", "false");
            }
            
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            
            // Create simple message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getFromEmail());
            message.setTo(config.getToEmail());
            message.setSubject(subject);
            message.setText(body);
            
            // Send email
            mailSender.send(message);
            
            logger.info("Email sent successfully to {}", config.getToEmail());
            return true;
            
        } catch (Exception e) {
            logger.warn("Failed to send email: {}", e.getMessage());
            return false;
        }
    }
}
