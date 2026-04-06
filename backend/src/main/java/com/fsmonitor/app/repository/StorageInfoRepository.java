package com.fsmonitor.app.repository;

import com.fsmonitor.app.entity.StorageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StorageInfoRepository extends JpaRepository<StorageInfo, Long> {
    List<StorageInfo> findByStorageConfigId(Long storageConfigId);
    
    @Modifying
    @Transactional
    void deleteByStorageConfigId(Long storageConfigId);
}
