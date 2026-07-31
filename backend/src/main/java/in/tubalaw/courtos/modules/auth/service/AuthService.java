package in.tubalaw.courtos.modules.auth.service;

import in.tubalaw.courtos.common.exception.BusinessException;
import in.tubalaw.courtos.common.exception.ResourceNotFoundException;
import in.tubalaw.courtos.common.security.JwtService;
import in.tubalaw.courtos.modules.auth.dto.AuthDtos;
import in.tubalaw.courtos.modules.auth.entity.User;
import in.tubalaw.courtos.modules.auth.repository.UserRepository;
import in.tubalaw.courtos.modules.settings.controller.SettingsController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepo;
    private final JwtService      jwtService;
    private final PasswordEncoder passwordEncoder;
    private final in.tubalaw.courtos.modules.auth.repository.UserSessionRepository userSessionRepository;
    private final in.tubalaw.courtos.modules.audit.service.AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest request;
    private final SettingsController settingsController;

    private static final String DEFAULT_TENANT = "default";

    // ── Login ─────────────────────────────────────────────────
    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        User user = userRepo.findByEmailAndTenantId(req.email(), DEFAULT_TENANT)
                .orElseThrow(() -> {
                    auditLogService.log(DEFAULT_TENANT, null, req.email(), "Login Failed", "Auth", null, "User not found or invalid credentials", ipAddress, "HIGH");
                    return new BusinessException("Invalid credentials");
                });

        if (!"active".equals(user.getStatus())) {
            auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Login Blocked", "Auth", user.getId().toString(), "Suspended account login attempt", ipAddress, "HIGH");
            throw new BusinessException("Your account is suspended. Contact admin.");
        }

        // Lockout check
        if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
            auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Login Blocked", "Auth", user.getId().toString(), "Account currently locked out", ipAddress, "MEDIUM");
            throw new BusinessException("Account locked. Try again later.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            userRepo.incrementFailedAttempts(user.getId());
            if (user.getFailedAttempts() + 1 >= 5) {
                // Lock for 15 minutes
                user.setLockedUntil(Instant.now().plusSeconds(15 * 60));
                userRepo.save(user);
                auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Account Locked", "User", user.getId().toString(), "Account locked due to 5 failed attempts", ipAddress, "HIGH");
            } else {
                auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Login Failed", "Auth", user.getId().toString(), "Invalid password attempt " + (user.getFailedAttempts() + 1), ipAddress, "MEDIUM");
            }
            throw new BusinessException("Invalid credentials");
        }

        userRepo.resetLockout(user.getId());
        userRepo.updateLastLogin(user.getId(), Instant.now());

        // Track user session
        String sessionId = java.util.UUID.randomUUID().toString();
        String device = parseDevice(userAgent);
        String location = "New Delhi"; // Simulating geoip location for premium dashboard feel
        
        in.tubalaw.courtos.modules.auth.entity.UserSession session = in.tubalaw.courtos.modules.auth.entity.UserSession.builder()
                .id(sessionId)
                .user(user)
                .tenantId(DEFAULT_TENANT)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .device(device)
                .location(location)
                .lastActive(Instant.now())
                .createdAt(Instant.now())
                .build();
        userSessionRepository.save(session);

        auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Login Success", "Auth", user.getId().toString(), "Logged in from " + ipAddress + " using " + device, ipAddress, "LOW");

        return buildTokenResponse(user);
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String os = "Unknown OS";
        String browser = "Unknown Browser";
        if (userAgent.contains("Windows")) os = "Windows 11";
        else if (userAgent.contains("Macintosh")) os = "macOS";
        else if (userAgent.contains("iPhone")) os = "iOS (iPhone)";
        else if (userAgent.contains("Android")) os = "Android";

        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Firefox")) browser = "Firefox";
        else if (userAgent.contains("Safari")) browser = "Safari";
        else if (userAgent.contains("Edge")) browser = "Edge";
        return browser + " (" + os + ")";
    }

    // ── Verify OTP (2FA stub) ─────────────────────────────────
    @Transactional
    public AuthDtos.LoginResponse verifyOtp(AuthDtos.VerifyOtpRequest req) {
        // TOTP verification would go here for MFA-enabled users
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.email()));
        userRepo.updateLastLogin(user.getId(), Instant.now());
        return buildTokenResponse(user);
    }

    // ── Refresh Token ─────────────────────────────────────────
    public AuthDtos.LoginResponse refresh(AuthDtos.RefreshRequest req) {
        if (!jwtService.isTokenValid(req.refreshToken())) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        String email = jwtService.extractEmail(req.refreshToken());
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return buildTokenResponse(user);
    }

    // ── Get Me ────────────────────────────────────────────────
    public AuthDtos.UserProfileDto getMe(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return toProfileDto(user);
    }

    // ── Change Password ───────────────────────────────────────
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        String ipAddress = request.getRemoteAddr();
        auditLogService.log(DEFAULT_TENANT, user.getId(), user.getEmail(), "Password Changed", "User", user.getId().toString(), "User changed their own password", ipAddress, "HIGH");
    }

    // ── Forgot Password (stub — would send email) ─────────────
    public AuthDtos.MessageResponse forgotPassword(AuthDtos.ForgotPasswordRequest req) {
        // In prod: generate token, send email
        log.info("Password reset requested for: {}", req.email());
        return new AuthDtos.MessageResponse("If that email exists, a reset link has been sent.");
    }

    // ── Private helpers ───────────────────────────────────────
    private AuthDtos.LoginResponse buildTokenResponse(User user) {
        List<String> perms = user.getPermissions() != null
                ? Arrays.asList(user.getPermissions())
                : List.of();

        // Extract scope from permissions or fallback based on role
        String scope = "own";
        if (perms.contains("scope_org") || "admin".equalsIgnoreCase(user.getRole())) {
            scope = "org";
        } else if (perms.contains("scope_team")) {
            scope = "team";
        } else if (perms.contains("scope_own")) {
            scope = "own";
        }

        String access  = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole(), user.getDepartment(), scope, perms);
        String refresh = jwtService.generateRefreshToken(user.getEmail());

        long timeoutSeconds = settingsController != null ? settingsController.getSessionTimeoutSeconds() : 900;
        long expiry = Math.min(900, timeoutSeconds);

        return new AuthDtos.LoginResponse(
                access,
                refresh,
                "Bearer",
                expiry,
                toProfileDto(user)
        );
    }

    private AuthDtos.UserProfileDto toProfileDto(User user) {
        List<String> perms = user.getPermissions() != null
                ? Arrays.asList(user.getPermissions())
                : List.of();
        return new AuthDtos.UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.isMfaEnabled(),
                user.getAvatar(),
                user.getInitials(),
                user.getGradient(),
                perms
        );
    }
}
