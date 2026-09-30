package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Long> {
    List<Integration> findByMonitoringEnabledTrue();
    
    @Query("SELECT DISTINCT i FROM Integration i LEFT JOIN FETCH i.monitoredFileTypes WHERE i.monitoringEnabled = true AND i.isActive = true")
    List<Integration> findActiveMonitoringIntegrations();
    
    Optional<Integration> findByName(String name);

    /** Bulk update - does not trigger @PreUpdate, so updatedAt is untouched. */
    @Modifying
    @Transactional
    @Query("UPDATE Integration i SET i.lastCheck = :checkedAt WHERE i.id = :id")
    void updateLastCheck(Long id, LocalDateTime checkedAt);

    /** Bulk update - does not trigger @PreUpdate, so updatedAt is untouched. */
    @Modifying
    @Transactional
    @Query("UPDATE Integration i SET i.lastFileFound = :fileFound, i.lastFileName = :fileName WHERE i.id = :id")
    void updateLastFileFound(Long id, LocalDateTime fileFound, String fileName);
}
