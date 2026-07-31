package in.tubalaw.courtos.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import in.tubalaw.courtos.modules.settings.controller.SettingsController;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Service
public class JwtService {

    @Autowired
    private SettingsController settingsController;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.access-expiry-seconds:900}")
    private long accessExpirySeconds;

    @Value("${security.jwt.refresh-expiry-seconds:604800}")
    private long refreshExpirySeconds;

    private SecretKey signingKey() {
        // Pad/encode to ensure minimum 256-bit key for HS256
        byte[] keyBytes = Base64.getEncoder().encode(jwtSecret.getBytes());
        byte[] decoded  = Decoders.BASE64.decode(new String(keyBytes));
        return Keys.hmacShaKeyFor(decoded);
    }

    public String generateAccessToken(String email, long userId, String role, String department, String scope, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub",        email);
        claims.put("uid",        userId);
        claims.put("role",       role);
        claims.put("department", department != null ? department : "");
        claims.put("scope",      scope != null ? scope : "own");
        claims.put("perms",      permissions != null ? permissions : List.of());
        
        long timeoutSeconds = settingsController != null ? settingsController.getSessionTimeoutSeconds() : accessExpirySeconds;
        long expiry = Math.min(900, timeoutSeconds); // access token maximum is 15 minutes or session timeout if smaller
        
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry * 1000))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(String email) {
        long timeoutSeconds = settingsController != null ? settingsController.getSessionTimeoutSeconds() : refreshExpirySeconds;
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + timeoutSeconds * 1000))
                .signWith(signingKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }
}
