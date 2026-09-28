package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByIsActiveTrue();
    
    List<Service> findByIsActiveTrueAndType(com.fsmonitor.app.entity.ServiceType type);

    /** Bulk update - does not trigger @PreUpdate, so updatedAt is untouched. */
    @Modifying
    @Transactional
    @Query("UPDATE Service s SET s.lastCheck = :checkedAt WHERE s.id = :id")
    void updateLastCheck(Long id, LocalDateTime checkedAt);
}
