package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@org.springframework.stereotype.Service
public class CredentialsTestService {
    private static final Logger logger = LoggerFactory.getLogger(CredentialsTestService.class);

    private final FtpServiceChecker ftpChecker;
    private final SftpServiceChecker sftpChecker;
    private final SmbServiceChecker smbChecker;

    public CredentialsTestService(FtpServiceChecker ftpChecker,
                                  SftpServiceChecker sftpChecker,
                                  SmbServiceChecker smbChecker) {
        this.ftpChecker = ftpChecker;
        this.sftpChecker = sftpChecker;
        this.smbChecker = smbChecker;
    }

    public Map<String, Object> testCredentials(Service service) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.debug("Testing credentials for {} service: {}", service.getType(), service.getName());
            
            boolean success = false;
            String message = "";
            long startTime = System.currentTimeMillis();
            
            switch (service.getType()) {
                case FTP:
                    success = ftpChecker.check(service);
                    message = success ? "FTP connection successful" : "FTP connection failed";
                    break;
                    
                case SFTP:
                    success = sftpChecker.check(service);
                    message = success ? "SFTP connection successful" : "SFTP connection failed";
                    break;
                    
                case SMB:
                    success = smbChecker.check(service);
                    message = success ? "SMB share access successful" : "SMB share access failed";
                    break;
                    
                default:
                    result.put("success", false);
                    result.put("message", "Service type " + service.getType() + " does not support credential testing");
                    return result;
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("success", success);
            result.put("message", message);
            result.put("responseTime", responseTime);
            result.put("serviceType", service.getType().toString());
            
            if (success) {
                result.put("details", buildSuccessDetails(service));
            } else {
                result.put("details", buildFailureDetails(service));
            }
            
        } catch (Exception e) {
            logger.error("Error testing credentials for {}: {}", service.getName(), e.getMessage());
            result.put("success", false);
            result.put("message", "Test failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }

    private String buildSuccessDetails(Service service) {
        StringBuilder details = new StringBuilder();
        details.append("✓ Connected to ").append(service.getHost()).append(":").append(service.getPort()).append("\n");
        details.append("✓ Authenticated as ").append(service.getUsername()).append("\n");
        
        if (service.getSharePath() != null && !service.getSharePath().isEmpty()) {
            details.append("✓ Path accessible: ").append(service.getSharePath()).append("\n");
        }
        
        return details.toString();
    }

    private String buildFailureDetails(Service service) {
        StringBuilder details = new StringBuilder();
        details.append("Possible issues:\n");
        details.append("• Check hostname/IP: ").append(service.getHost()).append("\n");
        details.append("• Check port: ").append(service.getPort()).append("\n");
        details.append("• Verify username: ").append(service.getUsername()).append("\n");
        details.append("• Check password/key\n");
        
        if (service.getSharePath() != null && !service.getSharePath().isEmpty()) {
            details.append("• Verify path exists: ").append(service.getSharePath()).append("\n");
        }
        
        if (service.getType() == ServiceType.SFTP) {
            details.append("• Check SSH key format (if using key auth)\n");
            details.append("• Verify server supports your cipher/kex algorithms\n");
        }
        
        if (service.getType() == ServiceType.SMB) {
            details.append("• Check domain (if using DOMAIN\\username)\n");
            details.append("• Verify SMB version compatibility\n");
        }
        
        return details.toString();
    }
}
