package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceChecker.class);
    
    private final RestTemplate restTemplate;

    public WebServiceChecker(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(java.time.Duration.ofSeconds(5))
            .setReadTimeout(java.time.Duration.ofSeconds(10))
            .build();
    }

    @Override
    public boolean check(Service service) {
        try {
            String url = String.format("http://%s:%d%s", 
                service.getHost(), 
                service.getPort() != null ? service.getPort() : 80,
                service.getPath() != null ? service.getPath() : "/");
            
            logger.debug("Checking web service: {}", url);
            
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (Exception e) {
            logger.debug("Web service check failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }
}
