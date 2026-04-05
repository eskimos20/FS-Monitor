package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Long> {
    List<Integration> findByMonitoringEnabledTrue();
    List<Integration> findByCleanupEnabledTrue();
    
    @Query("SELECT i FROM Integration i WHERE i.monitoringEnabled = true AND i.isActive = true")
    List<Integration> findActiveMonitoringIntegrations();
    
    @Query("SELECT i FROM Integration i WHERE i.cleanupEnabled = true AND i.isActive = true")
    List<Integration> findActiveCleanupIntegrations();
    
    Optional<Integration> findByName(String name);
}
