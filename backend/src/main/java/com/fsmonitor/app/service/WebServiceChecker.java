package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;
import com.fsmonitor.app.entity.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Component
public class WebServiceChecker implements ServiceChecker {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceChecker.class);

    private final RestTemplate restTemplate;

    public WebServiceChecker(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .readTimeout(java.time.Duration.ofSeconds(10))
            // Never throw on HTTP error status - any response proves the
            // service is answering; the status is evaluated in check().
            .errorHandler(new DefaultResponseErrorHandler() {
                @Override
                public boolean hasError(HttpStatusCode statusCode) {
                    return false;
                }
            })
            .build();
    }

    @Override
    public boolean check(Service service) {
        try {
            boolean https = service.getType() == ServiceType.HTTPS;
            String url = String.format("%s://%s:%d%s",
                https ? "https" : "http",
                formatHost(service.getHost()),
                service.getPort() != null ? service.getPort() : (https ? 443 : 80),
                normalizePath(service.getPath()));

            logger.debug("Checking web service: {}", url);

            // Read only the status line - the body is never buffered, so a
            // huge response cannot exhaust memory.
            HttpStatusCode status = restTemplate.execute(url, HttpMethod.GET, null,
                response -> response.getStatusCode());

            // Any HTTP response means the service is up and talking - even
            // 401/403/404 (auth required, missing path). Only 5xx or a failed
            // connection counts as offline.
            if (status != null && status.is5xxServerError()) {
                logger.debug("Web service {} answered {} - reporting OFFLINE", url, status.value());
                return false;
            }
            return status != null;
        } catch (Exception e) {
            logger.debug("Web service check failed for {}: {}", service.getName(), e.getMessage());
            return false;
        }
    }

    /** IPv6 literals must be bracket-wrapped to form a valid URL. */
    private String formatHost(String host) {
        if (host != null && host.contains(":") && !host.startsWith("[")) {
            return "[" + host + "]";
        }
        return host;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
