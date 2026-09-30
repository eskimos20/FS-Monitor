package com.fsmonitor.app.service;

import com.fsmonitor.app.cache.ServiceCacheService;
import com.fsmonitor.app.cache.ServiceCacheService.ServiceCache;
import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceStatus;
import com.fsmonitor.app.entity.ServiceType;
import com.fsmonitor.app.repository.ServiceRepository;
import com.fsmonitor.app.util.ScheduleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ServiceMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitoringService.class);

    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;
    private final ServiceCacheService serviceCacheService;
    private final Map<ServiceType, ServiceChecker> checkers;

    public ServiceMonitoringService(ServiceRepository serviceRepository,
                                    NotificationService notificationService,
                                    ServiceCacheService serviceCacheService,
                                    WebServiceChecker webChecker,
                                    PingServiceChecker pingChecker,
                                    FtpServiceChecker ftpChecker,
                                    SftpServiceChecker sftpChecker,
                                    SmbServiceChecker smbChecker,
                                    TcpServiceChecker tcpChecker) {
        this.serviceRepository = serviceRepository;
        this.notificationService = notificationService;
        this.serviceCacheService = serviceCacheService;

        this.checkers = new EnumMap<>(ServiceType.class);
        checkers.put(ServiceType.WEB, webChecker);
        checkers.put(ServiceType.HTTPS, webChecker);
        checkers.put(ServiceType.PING, pingChecker);
        checkers.put(ServiceType.FTP, ftpChecker);
        checkers.put(ServiceType.SFTP, sftpChecker);
        checkers.put(ServiceType.SMB, smbChecker);
        // TCP-based checks for all port-listening service types
        for (ServiceType type : List.of(ServiceType.SSH, ServiceType.MYSQL, ServiceType.POSTGRESQL,
                ServiceType.MONGODB, ServiceType.REDIS, ServiceType.MSSQL, ServiceType.DNS,
                ServiceType.LDAP, ServiceType.RDP)) {
            checkers.put(type, tcpChecker);
        }
    }

    @PostConstruct
    public void initializeServiceMonitoring() {
        logger.info("Initializing service monitoring on startup");
        // Seed lastCheckedAt from the persisted lastCheck column so a restart
        // does not trigger an immediate rescan of every service.
        try {
            for (Service service : serviceRepository.findAll()) {
                if (service.getLastCheck() != null) {
                    serviceCacheService.getCache(service.getId())
                            .setLastCheckedAt(service.getLastCheck());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not seed service check timestamps: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void monitorServices() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        logger.debug("Starting service monitoring check...");

        List<Service> activeServices = serviceRepository.findByIsActiveTrue();

        // Drop cache entries for deleted services
        Set<Long> activeIds = activeServices.stream()
                .map(Service::getId)
                .collect(Collectors.toSet());
        serviceCacheService.cleanupDeletedConfigs(activeIds);
        notificationService.pruneServiceFailures(activeIds);

        for (Service service : activeServices) {
            try {
                if (shouldCheckService(service, now)) {
                    checkService(service, now);
                }
            } catch (Exception e) {
                logger.error("Error monitoring service: {}", service.getName(), e);
            }
        }

        logger.debug("Service monitoring check completed for {} services", activeServices.size());
    }

    private boolean shouldCheckService(Service service, LocalDateTime now) {
        // Check schedule constraints first
        if (Boolean.TRUE.equals(service.getScheduleEnabled())) {
            if (!ScheduleUtil.isWithinSchedule(service.getActiveDays(),
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
            return true; // First check - run immediately
        }

        Integer interval = service.getCheckIntervalMinutes();
        LocalDateTime nextCheck = lastCheck.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(interval != null ? interval.longValue() : 5L);
        return !now.isBefore(nextCheck);
    }

    private ServiceChecker getCheckerForService(Service service) {
        if (service.getType() == ServiceType.CUSTOM && service.getCheckMethod() != null) {
            // For CUSTOM services, checkMethod determines the checker
            return switch (service.getCheckMethod()) {
                case HTTP -> checkers.get(ServiceType.WEB);
                case PING -> checkers.get(ServiceType.PING);
                case TCP -> checkers.get(ServiceType.SSH); // TCP socket check
            };
        }
        if (service.getType() == ServiceType.CUSTOM) {
            return checkers.get(ServiceType.SSH); // Default CUSTOM to TCP
        }
        return checkers.get(service.getType());
    }

    private void checkService(Service service, LocalDateTime now) {
        long startTime = System.currentTimeMillis();
        ServiceCache cache = serviceCacheService.getCache(service.getId());
        cache.setLastCheckedAt(now);
        // Persist so check pacing survives restarts (bulk update - does not touch updatedAt)
        try {
            serviceRepository.updateLastCheck(service.getId(), now);
        } catch (Exception e) {
            logger.debug("Could not persist lastCheck for service {}: {}", service.getId(), e.getMessage());
        }

        ServiceChecker checker = getCheckerForService(service);
        if (checker == null) {
            logger.warn("No checker found for service type: {}", service.getType());
            cache.setStatus(ServiceStatus.UNKNOWN);
            cache.setLastError("No checker available for type: " + service.getType());
        } else {
            try {
                boolean isOnline = checker.check(service);

                if (isOnline) {
                    cache.setStatus(ServiceStatus.ONLINE);
                    cache.setLastSuccessfulCheck(now);
                    cache.setLastError(null);
                } else {
                    cache.setStatus(ServiceStatus.OFFLINE);
                    cache.setLastError("Service check failed");
                }

                logger.info("Service scan completed - Name: {} - Duration: {}ms",
                        service.getName(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                cache.setStatus(ServiceStatus.OFFLINE);
                cache.setLastError(e.getMessage());
                logger.error("Service check failed for {}: {}", service.getName(), e.getMessage());
            }
        }

        // Notify on transition to offline; clear once it recovers
        if (Boolean.TRUE.equals(service.getIsActive()) && cache.getStatus() == ServiceStatus.OFFLINE) {
            notificationService.checkAndSendServiceNotification(service);
        } else if (Boolean.TRUE.equals(service.getIsActive()) && cache.getStatus() == ServiceStatus.ONLINE) {
            notificationService.clearServiceNotification(service);
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
}
