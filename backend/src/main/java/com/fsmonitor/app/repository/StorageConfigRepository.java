package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.StorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorageConfigRepository extends JpaRepository<StorageConfig, Long> {
    List<StorageConfig> findByActiveTrue();
}
