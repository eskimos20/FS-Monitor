package com.fsmonitor.app.config;

import com.fsmonitor.app.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${cors.allowed-origins:http://localhost:8088,http://localhost:3000,http://localhost:5173}")
                          List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Stateless JWT API - CSRF protection is not required
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentTypeOptions(cto -> {})
                        // sameOrigin still blocks cross-origin clickjacking while
                        // allowing the H2 console frameset in development
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(ref -> ref.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/version").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // H2 console is only enabled outside the production profile
                        // and is bound to localhost (web-allow-others: false)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Static resources (frontend) - Allow all routes for React Router
                        .requestMatchers("/", "/index.html", "/login", "/change-password",
                                "/dashboard", "/system", "/integrations", "/services",
                                "/storage", "/delete-services", "/log-control",
                                "/settings", "/settings/**",
                                "/assets/**", "/favicon.ico", "/vite.svg",
                                "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.svg").permitAll()
                        // User registration restricted to administrators
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        // File browser exposes the server filesystem - administrators only
                        .requestMatchers("/api/file-browser/**").hasRole("ADMIN")
                        // Read-only monitoring data for any authenticated user,
                        // mutations restricted to administrators
                        .requestMatchers(HttpMethod.GET, "/api/integrations/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/file-types/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/storage-configs/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/app-settings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/log-configs/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/delete-services/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/services/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/monitoring/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/monitoring-cache/**").authenticated()
                        // Process list can leak secrets via command-line args - admin only
                        .requestMatchers("/api/system/**").hasRole("ADMIN")
                        // Everything else under /api requires authentication;
                        // mutating endpoints additionally enforce ADMIN via @PreAuthorize
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
