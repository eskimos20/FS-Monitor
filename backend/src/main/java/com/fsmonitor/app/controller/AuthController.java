package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.LoginRequest;
import com.fsmonitor.app.dto.LoginResponse;
import com.fsmonitor.app.dto.PasswordChangeRequest;
import com.fsmonitor.app.dto.SignUpRequest;
import com.fsmonitor.app.dto.UserResponse;
import com.fsmonitor.app.entity.User;
import com.fsmonitor.app.security.LoginRateLimiter;
import com.fsmonitor.app.security.UserPrincipal;
import com.fsmonitor.app.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final boolean trustForwardedHeaders;

    public AuthController(AuthService authService,
                          LoginRateLimiter loginRateLimiter,
                          @Value("${fsmonitor.security.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                          HttpServletRequest request) {
        // Two locks: per-account (brute force against one user, incl. XFF spoofing)
        // and per-IP with a higher ceiling (credential spraying across many users)
        String userKey = "U:" + loginRequest.getUsernameOrEmail().toLowerCase();
        String ipKey = "IP:" + clientIp(request);

        if (loginRateLimiter.isBlocked(userKey, ipKey)) {
            long waitSeconds = Math.max(
                    loginRateLimiter.remainingLockSeconds(userKey),
                    loginRateLimiter.remainingLockSeconds(ipKey));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + (waitSeconds / 60 + 1) + " minutes.");
        }

        try {
            LoginResponse loginResponse = authService.authenticateUser(loginRequest);
            loginRateLimiter.recordSuccess(userKey, ipKey);
            return ResponseEntity.ok(loginResponse);
        } catch (AuthenticationException ex) {
            loginRateLimiter.recordFailure(userKey, ipKey);
            if (loginRateLimiter.isBlocked(userKey) || loginRateLimiter.isBlocked(ipKey)) {
                logger.warn("Login throttled for user '{}' from {} after repeated failures",
                        loginRequest.getUsernameOrEmail(), clientIp(request));
            }
            throw ex;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        User user = authService.registerUser(signUpRequest);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody PasswordChangeRequest passwordChangeRequest,
                                                      @AuthenticationPrincipal UserPrincipal currentUser) {

        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmPassword())) {
            return ResponseEntity.badRequest().build();
        }

        LoginResponse loginResponse = authService.changePassword(passwordChangeRequest, currentUser);
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = authService.getCurrentUser(userPrincipal);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private String clientIp(HttpServletRequest request) {
        // X-Forwarded-For is client-controllable; only honor it when the
        // deployment explicitly trusts forwarded headers (i.e. sits behind a
        // reverse proxy that sanitizes the header).
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
