package com.fsmonitor.app.service;

import com.fsmonitor.app.repository.DiskSpaceRepository;
import com.fsmonitor.app.repository.LargestFileRepository;
import com.fsmonitor.app.repository.StorageInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageDataService {
    
    @Autowired
    private StorageInfoRepository storageInfoRepository;
    
    @Autowired
    private LargestFileRepository largestFileRepository;
    
    @Autowired
    private DiskSpaceRepository diskSpaceRepository;
    
    @Transactional
    public void deleteOldStorageData(Long configId) {
        storageInfoRepository.deleteByStorageConfigId(configId);
        largestFileRepository.deleteByStorageConfigId(configId);
        diskSpaceRepository.deleteByStorageConfigId(configId);
    }
}
