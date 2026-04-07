package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    
    Optional<AppSettings> findFirstByOrderByIdDesc();
}
