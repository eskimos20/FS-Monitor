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

    public boolean sendEmail(String subject, String body, String username, String password) {
        logger.info("Attempting to send email to {} with subject: {}", 
                    mailConfigService.getCurrentMailConfig().map(c -> c.getRecipient()).orElse("unknown"), 
                    subject);
        
        try {
            MailConfig config = mailConfigService.getCurrentMailConfig()
                .orElseThrow(() -> new RuntimeException("Mail configuration not found"));

            // Create JavaMailSender with Gmail settings
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            
            // SMTP server settings
            mailSender.setHost(config.getServer());
            mailSender.setPort(config.getPort());
            
            // Authentication
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            
            // SMTP properties for Gmail
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            
            // Create simple message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getSender());
            message.setTo(config.getRecipient());
            message.setSubject(subject);
            message.setText(body);
            
            // Send email
            mailSender.send(message);
            
            logger.info("Email sent successfully to {}", config.getRecipient());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
            return false;
        }
    }
}
