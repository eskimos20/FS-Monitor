package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.repository.MailConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MailConfigService {
    
    private final MailConfigRepository mailConfigRepository;

    public MailConfigService(MailConfigRepository mailConfigRepository) {
        this.mailConfigRepository = mailConfigRepository;
    }
    
    @Transactional
    public MailConfig saveMailConfig(MailConfig mailConfig) {
        // Single config: update the existing row in place. Deleting and
        // re-inserting an entity that already has an id fails under
        // GenerationType.IDENTITY (detached entity passed to persist).
        return getCurrentMailConfig()
                .map(existing -> {
                    existing.setHost(mailConfig.getHost());
                    existing.setPort(mailConfig.getPort());
                    existing.setFromEmail(mailConfig.getFromEmail());
                    existing.setToEmail(mailConfig.getToEmail());
                    existing.setUsername(mailConfig.getUsername());
                    // Password is write-only in JSON: when the incoming config
                    // omits it, keep the currently stored password.
                    if (mailConfig.getPassword() != null && !mailConfig.getPassword().isBlank()) {
                        existing.setPassword(mailConfig.getPassword());
                    }
                    return mailConfigRepository.save(existing);
                })
                .orElseGet(() -> mailConfigRepository.save(mailConfig));
    }
    
    @Transactional(readOnly = true)
    public Optional<MailConfig> getCurrentMailConfig() {
        return mailConfigRepository.findFirstByOrderByIdDesc();
    }
    
    @Transactional
    public void deleteMailConfig(Long id) {
        mailConfigRepository.deleteById(id);
    }
}
