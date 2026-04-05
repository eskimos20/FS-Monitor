package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.MailConfig;
import com.fsmonitor.app.repository.MailConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MailConfigService {
    
    @Autowired
    private MailConfigRepository mailConfigRepository;
    
    @Transactional
    public MailConfig saveMailConfig(MailConfig mailConfig) {
        // Delete existing config if any
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
