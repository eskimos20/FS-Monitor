package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Integration;
import com.fsmonitor.app.repository.IntegrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IntegrationService {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Transactional
    public List<Integration> getAllIntegrations() {
        return integrationRepository.findAll();
    }

    @Transactional
    public Optional<Integration> getIntegrationById(Long id) {
        return integrationRepository.findById(id);
    }

    @Transactional
    public Integration createIntegration(Integration integration) {
        return integrationRepository.save(integration);
    }

    @Transactional
    public Integration updateIntegration(Long id, Integration integrationDetails) {
        return integrationRepository.findById(id)
                .map(integration -> {
                    integration.setName(integrationDetails.getName());
                    integration.setPath(integrationDetails.getPath());
                    integration.setIsActive(integrationDetails.getIsActive());
                    integration.setMonitoringEnabled(integrationDetails.getMonitoringEnabled());
                    integration.setCleanupEnabled(integrationDetails.getCleanupEnabled());
                    integration.setCheckIntervalValue(integrationDetails.getCheckIntervalValue());
                    integration.setCheckIntervalUnit(integrationDetails.getCheckIntervalUnit());
                    integration.setThresholdValue(integrationDetails.getThresholdValue());
                    integration.setThresholdUnit(integrationDetails.getThresholdUnit());
                    integration.setCleanupAgeValue(integrationDetails.getCleanupAgeValue());
                    integration.setCleanupAgeUnit(integrationDetails.getCleanupAgeUnit());
                    integration.setMonitorAllFiles(integrationDetails.getMonitorAllFiles());
                    integration.setScheduleDays(integrationDetails.getScheduleDays());
                    integration.setMonitoredFileTypes(integrationDetails.getMonitoredFileTypes());
                    integration.setScheduleEnabled(integrationDetails.getScheduleEnabled());
                    integration.setActiveDays(integrationDetails.getActiveDays());
                    integration.setActiveStartHour(integrationDetails.getActiveStartHour());
                    integration.setActiveEndHour(integrationDetails.getActiveEndHour());
                    integration.setUpdatedAt(java.time.LocalDateTime.now());
                    return integrationRepository.save(integration);
                })
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
    }

    @Transactional
    public void deleteIntegration(Long id) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        integrationRepository.delete(integration);
    }

    @Transactional
    public List<Integration> getActiveMonitoringIntegrations() {
        return integrationRepository.findActiveMonitoringIntegrations();
    }

    @Transactional
    public List<Integration> getActiveCleanupIntegrations() {
        return integrationRepository.findActiveCleanupIntegrations();
    }

    @Transactional
    public void updateIntegrationStatus(Long id, boolean isActive) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        integration.setIsActive(isActive);
        integration.setUpdatedAt(java.time.LocalDateTime.now());
        integrationRepository.save(integration);
    }

    @Transactional
    public void updateLastFileFound(Long id, java.time.LocalDateTime lastFileFound) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration not found with id: " + id));
        integration.setLastFileFound(lastFileFound);
        integration.setUpdatedAt(java.time.LocalDateTime.now());
        integrationRepository.save(integration);
    }
}
