package in.tubalaw.courtos.modules.auth.controller;

import in.tubalaw.courtos.common.security.SecurityScopeContext;
import in.tubalaw.courtos.common.util.ApiResponse;
import in.tubalaw.courtos.modules.audit.service.AuditLogService;
import in.tubalaw.courtos.modules.auth.dto.AuthDtos;
import in.tubalaw.courtos.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> login(
            @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> verifyOtp(
            @RequestBody AuthDtos.VerifyOtpRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(req)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDtos.LoginResponse>> refresh(
            @RequestBody AuthDtos.RefreshRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(req)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<AuthDtos.MessageResponse>> forgotPassword(
            @RequestBody AuthDtos.ForgotPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.forgotPassword(req)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        SecurityScopeContext.UserSecurityDetails user = SecurityScopeContext.getCurrentUser();
        auditLogService.log("default",
                user != null ? user.getUserId() : null,
                user != null ? user.getEmail() : "anonymous",
                "Logout", "Auth", user != null && user.getUserId() != null ? user.getUserId().toString() : null,
                "User logged out from " + request.getRemoteAddr(),
                request.getRemoteAddr(), "LOW");
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @RequestBody java.util.Map<String, String> req) {
        String email = (String) auth.getPrincipal();
        authService.changePassword(email, req.get("oldPassword"), req.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.UserProfileDto>> me(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(email)));
    }
}
