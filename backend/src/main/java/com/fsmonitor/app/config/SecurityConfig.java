package com.fsmonitor.app.config;

import com.fsmonitor.app.security.CustomUserDetailsService;
import com.fsmonitor.app.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/file-browser/**").permitAll()
                        .requestMatchers("/api/version").permitAll()
                        // Static resources (frontend) - Allow all routes for React Router
                        .requestMatchers("/", "/index.html", "/login", "/dashboard", "/settings", "/assets/**", "/favicon.ico", "/vite.svg", "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.svg").permitAll()
                        // API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/integrations/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/integrations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/integrations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/integrations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/file-types/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/file-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/file-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/file-types/**").hasRole("ADMIN")
                        .requestMatchers("/api/mail-config/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/storage-configs/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/storage-configs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/storage-configs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/storage-configs/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
