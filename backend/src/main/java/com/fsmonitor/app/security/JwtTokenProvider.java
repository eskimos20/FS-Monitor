package com.fsmonitor.app.security;

import com.fsmonitor.app.util.SecretManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final long jwtExpirationInMs;
    private final SecretKey signingKey;

    public JwtTokenProvider(SecretManager secretManager,
                            @Value("${jwt.expiration:86400000}") long jwtExpirationInMs) {
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.signingKey = Keys.hmacShaKeyFor(
                secretManager.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        int tokenVersion = authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal.getTokenVersion() : 0;
        return generateToken(authentication, tokenVersion);
    }

    public String generateToken(Authentication authentication, int tokenVersion) {
        String username = authentication.getName();

        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .claim("tv", tokenVersion)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        return parseClaims(token).getSubject();
    }

    /** Token version carried by the JWT; -1 when the claim is absent (pre-revocation tokens). */
    public int getTokenVersionFromJWT(String token) {
        Object tv = parseClaims(token).get("tv");
        return tv instanceof Number number ? number.intValue() : -1;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
