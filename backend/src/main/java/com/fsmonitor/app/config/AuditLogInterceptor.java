package com.fsmonitor.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Audit trail: every mutating API call is logged with the authenticated
 * user, HTTP method, path and response status. Output goes to the normal
 * log (journald under systemd), keeping a record of who changed what.
 */
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final Logger auditLog = LoggerFactory.getLogger("fsmonitor.audit");
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!MUTATING_METHODS.contains(request.getMethod())) {
            return;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";

        auditLog.info("AUDIT user={} method={} path={} status={}{}",
                user, request.getMethod(), uri, response.getStatus(),
                ex != null ? " exception=" + ex.getClass().getSimpleName() : "");
    }
}
