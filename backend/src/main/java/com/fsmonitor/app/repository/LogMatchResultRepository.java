package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.LogMatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogMatchResultRepository extends JpaRepository<LogMatchResult, Long> {
    List<LogMatchResult> findByLogConfigIdOrderByFoundAtDesc(Long logConfigId);
    List<LogMatchResult> findByFoundAtAfterOrderByFoundAtDesc(LocalDateTime after);
    void deleteByLogConfigId(Long logConfigId);
}
