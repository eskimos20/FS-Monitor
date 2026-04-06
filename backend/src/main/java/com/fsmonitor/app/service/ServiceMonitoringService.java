package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceStatus;
import com.fsmonitor.app.entity.ServiceType;
import com.fsmonitor.app.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServiceMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitoringService.class);

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private NotificationService notificationService;

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
    @Transactional
    public void init() {
        // Initialize lastCheckedAt for all services that don't have it
        List<Service> allServices = serviceRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Service service : allServices) {
            if (service.getLastCheckedAt() == null) {
                service.setLastCheckedAt(now);
                serviceRepository.save(service);
            }
        }
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Run every minute, wait 1 min after startup
    @Transactional
    public void monitorServices() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        
        logger.debug("Starting service monitoring check...");
        
        List<Service> activeServices = serviceRepository.findByIsActiveTrue();
        
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
        LocalDateTime lastCheck = service.getLastCheckedAt();
        if (lastCheck == null) {
            service.setLastCheckedAt(now);
            serviceRepository.save(service);
            return false;
        }
        
        LocalDateTime nextCheck = lastCheck.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
            .plusMinutes(service.getCheckIntervalMinutes().longValue());
        return !now.isBefore(nextCheck);
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
        logger.info("Running check for service: {}", service.getName());
        
        ServiceChecker checker = getCheckerForService(service);
        if (checker == null) {
            logger.warn("No checker found for service type: {}", service.getType());
            service.setStatus(ServiceStatus.UNKNOWN);
            service.setLastError("No checker available for type: " + service.getType());
        } else {
            try {
                boolean isOnline = checker.check(service);
                service.setLastCheckedAt(now);
                
                if (isOnline) {
                    service.setStatus(ServiceStatus.ONLINE);
                    service.setLastSuccessfulCheck(now);
                    service.setLastError(null);
                } else {
                    service.setStatus(ServiceStatus.OFFLINE);
                    service.setLastError("Service check failed");
                }
            } catch (Exception e) {
                service.setStatus(ServiceStatus.OFFLINE);
                service.setLastError(e.getMessage());
                logger.error("Service check failed for {}: {}", service.getName(), e.getMessage());
            }
        }
        
        serviceRepository.save(service);
        
        // Check if service is offline and send notification
        if (service.getIsActive() && service.getStatus() == ServiceStatus.OFFLINE) {
            notificationService.checkAndSendServiceNotification(
                service.getId(), 
                service.getName()
            );
        } else if (service.getIsActive() && service.getStatus() == ServiceStatus.ONLINE) {
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
}
