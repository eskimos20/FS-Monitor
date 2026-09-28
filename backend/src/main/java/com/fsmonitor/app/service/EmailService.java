package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.util.Properties;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final MailConfigService mailConfigService;

    public EmailService(MailConfigService mailConfigService) {
        this.mailConfigService = mailConfigService;
    }

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
            
            // Set UTF-8 encoding for Swedish characters (åäö)
            props.put("mail.mime.charset", "UTF-8");
            
            // Create MimeMessage with proper encoding (matching working implementation)
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            
            mimeMessage.setFrom(new jakarta.mail.internet.InternetAddress(config.getFromEmail()));
            mimeMessage.setRecipients(jakarta.mail.Message.RecipientType.TO, 
                                     jakarta.mail.internet.InternetAddress.parse(config.getToEmail()));
            
            // Set headers for proper UTF-8 encoding
            mimeMessage.setHeader("Content-Transfer-Encoding", "quoted-printable");
            mimeMessage.setHeader("Content-Type", "text/plain;charset=UTF-8");
            
            // Encode subject with Base64 for Swedish characters
            mimeMessage.setSubject(MimeUtility.encodeWord(subject, "UTF-8", "B"));
            
            // Set content with explicit charset
            mimeMessage.setContent(body, "text/plain;charset=UTF-8");
            
            // Send email
            mailSender.send(mimeMessage);
            
            logger.info("Email sent successfully to {}", config.getToEmail());
            return true;
            
        } catch (Exception e) {
            logger.warn("Failed to send email: {}", e.getMessage());
            return false;
        }
    }
}
