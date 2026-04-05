package com.fsmonitor.app.controller;

import com.fsmonitor.app.dto.LoginRequest;
import com.fsmonitor.app.dto.LoginResponse;
import com.fsmonitor.app.dto.PasswordChangeRequest;
import com.fsmonitor.app.dto.SignUpRequest;
import com.fsmonitor.app.entity.User;
import com.fsmonitor.app.security.UserPrincipal;
import com.fsmonitor.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        User user = authService.registerUser(signUpRequest);
        return ResponseEntity.ok(user);
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
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = authService.getCurrentUser(userPrincipal);
        return ResponseEntity.ok(user);
    }
}
