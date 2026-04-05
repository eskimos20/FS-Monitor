package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.NotificationLog;
import com.fsmonitor.app.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    Optional<NotificationLog> findByTypeAndEntityId(NotificationType type, Long entityId);
    
    void deleteByTypeAndEntityId(NotificationType type, Long entityId);
}
