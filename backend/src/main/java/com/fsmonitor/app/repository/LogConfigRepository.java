package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.LogConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogConfigRepository extends JpaRepository<LogConfig, Long> {
    List<LogConfig> findByActiveTrue();
}
