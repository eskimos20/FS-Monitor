package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.DeleteService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeleteServiceRepository extends JpaRepository<DeleteService, Long> {
    List<DeleteService> findByCleanupEnabledTrue();
    
    @Query("SELECT d FROM DeleteService d WHERE d.cleanupEnabled = true")
    List<DeleteService> findActiveCleanupServices();
}
