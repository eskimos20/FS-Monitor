package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.MailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MailConfigRepository extends JpaRepository<MailConfig, Long> {
    
    Optional<MailConfig> findFirstByOrderByIdDesc();
}
