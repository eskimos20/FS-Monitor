package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.ServiceCacheService;
import com.fsmonitor.app.cache.ServiceCacheService.ServiceCache;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceStatus;
import com.fsmonitor.app.entity.ServiceType;
import com.fsmonitor.app.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ServiceMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitoringService.class);

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ServiceCacheService serviceCacheService;

    private final Map<ServiceType, ServiceChecker> checkers;

    public ServiceMonitoringService(WebServiceChecker webChecker, PingServiceChecker pingChecker, 
                                 FtpServiceChecker ftpChecker, SftpServiceChecker sftpChecker, 
                                 SmbServiceChecker smbChecker, TcpServiceChecker tcpChecker) {
        this.checkers = new HashMap<>();
        this.checkers.put(ServiceType.WEB, webChecker);
        this.checkers.put(ServiceType.HTTPS, webChecker);
        this.checkers.put(ServiceType.PING, pingChecker);
        this.checkers.put(ServiceType.FTP, ftpChecker);
        this.checkers.put(ServiceType.SFTP, sftpChecker);
        this.checkers.put(ServiceType.SMB, smbChecker);
        this.checkers.put(ServiceType.SSH, tcpChecker);
        this.checkers.put(ServiceType.MYSQL, tcpChecker);
        this.checkers.put(ServiceType.POSTGRESQL, tcpChecker);
        this.checkers.put(ServiceType.MONGODB, tcpChecker);
        this.checkers.put(ServiceType.REDIS, tcpChecker);
        this.checkers.put(ServiceType.MSSQL, tcpChecker);
        this.checkers.put(ServiceType.DNS, tcpChecker);
        this.checkers.put(ServiceType.LDAP, tcpChecker);
        this.checkers.put(ServiceType.RDP, tcpChecker);
    }

    @jakarta.annotation.PostConstruct
    public void initializeServiceMonitoring() {
        logger.info("Initializing service monitoring on startup");
        // Note: Don't set lastCheckedAt or status to avoid false timestamps
        // Cache will be populated when actual checks occur
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    public void monitorServices() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        
        logger.debug("Starting service monitoring check...");
        
        List<Service> activeServices = serviceRepository.findByIsActiveTrue();
        
        // MEMORY CLEANUP: Clear cache entries for deleted services
        cleanupDeletedServices(activeServices);
        
        for (Service service : activeServices) {
            try {
                if (shouldCheckService(service, now)) {
                    checkService(service, now);
                }
            } catch (Exception e) {
                logger.error("Error monitoring service: " + service.getName(), e);
            }
        }
        
        logger.debug("Service monitoring check completed for {} services", activeServices.size());
    }

    private boolean shouldCheckService(Service service, LocalDateTime now) {
        // Check schedule constraints first
        if (Boolean.TRUE.equals(service.getScheduleEnabled())) {
            if (!isWithinSchedule(service.getActiveDays(), 
                                   service.getActiveStartHour(), 
                                   service.getActiveEndHour(), 
                                   now)) {
                logger.debug("Service {} is outside scheduled time, skipping", service.getName());
                return false;
            }
        }
        
        ServiceCache cache = serviceCacheService.getCache(service.getId());
        LocalDateTime lastCheck = cache.getLastCheckedAt();
        if (lastCheck == null) {
            // First check - run immediately
            return true;
        }
        
        LocalDateTime nextCheck = lastCheck.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(service.getCheckIntervalMinutes().longValue());
        return !now.isBefore(nextCheck);
    }

    private boolean isWithinSchedule(String activeDays, Integer startHour, Integer endHour, LocalDateTime now) {
        // Check day of week
        if (activeDays != null && !activeDays.isEmpty()) {
            String currentDay = now.getDayOfWeek().name().substring(0, 3).toUpperCase();
            if (!activeDays.toUpperCase().contains(currentDay)) {
                return false;
            }
        }
        
        // Check time of day
        if (startHour != null && endHour != null) {
            int currentHour = now.getHour();
            if (currentHour < startHour || currentHour >= endHour) {
                return false;
            }
        }
        
        return true;
    }

    private ServiceChecker getCheckerForService(Service service) {
        if (service.getType() == ServiceType.CUSTOM && service.getCheckMethod() != null) {
            // For CUSTOM services, use checkMethod to determine checker
            switch (service.getCheckMethod()) {
                case TCP:
                    return checkers.get(ServiceType.SSH); // Use TCP checker
                case HTTP:
                    return checkers.get(ServiceType.WEB); // Use HTTP checker
                case PING:
                    return checkers.get(ServiceType.PING); // Use PING checker
                default:
                    return checkers.get(ServiceType.SSH); // Default to TCP
            }
        }
        return checkers.get(service.getType());
    }

    private void checkService(Service service, LocalDateTime now) {
        long startTime = System.currentTimeMillis();
        ServiceCache cache = serviceCacheService.getCache(service.getId());
        
        ServiceChecker checker = getCheckerForService(service);
        if (checker == null) {
            logger.warn("No checker found for service type: {}", service.getType());
            cache.setStatus(ServiceStatus.UNKNOWN);
            cache.setLastError("No checker available for type: " + service.getType());
        } else {
            try {
                boolean isOnline = checker.check(service);
                cache.setLastCheckedAt(now);
                
                if (isOnline) {
                    cache.setStatus(ServiceStatus.ONLINE);
                    cache.setLastSuccessfulCheck(now);
                    cache.setLastError(null);
                } else {
                    cache.setStatus(ServiceStatus.OFFLINE);
                    cache.setLastError("Service check failed");
                }
                
                long endTime = System.currentTimeMillis();
                logger.info("Service scan completed - Name: {} - Duration: {}ms", service.getName(), (endTime - startTime));
            } catch (Exception e) {
                cache.setStatus(ServiceStatus.OFFLINE);
                cache.setLastError(e.getMessage());
                logger.error("Service check failed for {}: {}", service.getName(), e.getMessage());
            }
        }
        
        // Check if service is offline and send notification
        if (service.getIsActive() && cache.getStatus() == ServiceStatus.OFFLINE) {
            notificationService.checkAndSendServiceNotification(
                service.getId(), 
                service.getName()
            );
        } else if (service.getIsActive() && cache.getStatus() == ServiceStatus.ONLINE) {
            // Service is online, clear any existing notification
            notificationService.clearServiceNotification(service.getId());
        }
    }

    // Public methods for controllers
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Service getServiceById(Long id) {
        return serviceRepository.findById(id).orElse(null);
    }

    public Service saveService(Service service) {
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }
    
    /**
     * MEMORY CLEANUP: Remove cache entries for deleted services
     * This prevents memory leaks when services are deleted from database
     */
    private void cleanupDeletedServices(List<Service> activeServices) {
        // Get all active service IDs
        Set<Long> activeServiceIds = activeServices.stream()
            .map(Service::getId)
            .collect(Collectors.toSet());
        
        // Clear cache entries for deleted services
        Set<Long> cachedServiceIds = serviceCacheService.getAllCachedIds();
        for (Long cachedId : cachedServiceIds) {
            if (!activeServiceIds.contains(cachedId)) {
                serviceCacheService.clearCache(cachedId);
                logger.debug("Cleared cache for deleted service ID: {}", cachedId);
            }
        }
    }
}
