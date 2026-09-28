package com.fsmonitor.app.service;

import com.fsmonitor.app.dto.LoginRequest;
import com.fsmonitor.app.dto.LoginResponse;
import com.fsmonitor.app.dto.SignUpRequest;
import com.fsmonitor.app.dto.PasswordChangeRequest;
import com.fsmonitor.app.entity.Role;
import com.fsmonitor.app.entity.RoleName;
import com.fsmonitor.app.entity.User;
import com.fsmonitor.app.repository.RoleRepository;
import com.fsmonitor.app.repository.UserRepository;
import com.fsmonitor.app.security.JwtTokenProvider;
import com.fsmonitor.app.security.UserPrincipal;
import com.fsmonitor.app.util.PasswordPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        return new LoginResponse(jwt, userPrincipal.getPasswordChanged(), 
                               userPrincipal.getUsername(), userPrincipal.getEmail());
    }

    @Transactional
    public User registerUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email Address already in use!");
        }

        PasswordPolicy.validate(signUpRequest.getPassword());

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User Role not set."));

        user.getRoles().add(userRole);

        return userRepository.save(user);
    }

    @Transactional
    public LoginResponse changePassword(PasswordChangeRequest passwordChangeRequest, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        PasswordPolicy.validate(passwordChangeRequest.getNewPassword());

        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        user.setPasswordChanged(true);
        // Bump token version: every JWT issued before this change becomes invalid
        int newTokenVersion = user.getTokenVersion() + 1;
        user.setTokenVersion(newTokenVersion);

        userRepository.save(user);

        // Generate new JWT token carrying the new token version
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUser.getUsername(),
                null,
                currentUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String newJwt = tokenProvider.generateToken(authentication, newTokenVersion);
        
        return new LoginResponse(newJwt, true, currentUser.getUsername(), currentUser.getEmail());
    }

    public User getCurrentUser(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
