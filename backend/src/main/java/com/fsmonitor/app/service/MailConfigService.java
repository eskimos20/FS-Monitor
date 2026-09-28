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
        // Password is write-only in JSON: when the incoming config omits it,
        // preserve the currently stored password instead of wiping it.
        if (mailConfig.getPassword() == null || mailConfig.getPassword().isBlank()) {
            getCurrentMailConfig().ifPresent(existing ->
                    mailConfig.setPassword(existing.getPassword()));
        }
        // Single config: replace existing
        mailConfigRepository.deleteAll();
        return mailConfigRepository.save(mailConfig);
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
