package in.tubalaw.courtos.modules.auth.dto;

import java.util.List;

public interface AuthDtos {

    record LoginRequest(String email, String password) {}

    record VerifyOtpRequest(String email, String otp) {}

    record ForgotPasswordRequest(String email) {}

    record ResetPasswordRequest(String email, String otp, String newPassword) {}

    record RefreshRequest(String refreshToken) {}

    record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserProfileDto user
    ) {}

    record UserProfileDto(
            long id,
            String name,
            String email,
            String role,
            String department,
            boolean mfa,
            String avatar,
            String initials,
            String gradient,
            List<String> permissions
    ) {}

    record MessageResponse(String message) {}
}
