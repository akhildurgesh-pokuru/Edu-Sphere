package com.edusphere.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MSG = "Use 8-72 characters with at least one letter and one number";

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 120, message = "Name is too long") String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 190) String email,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password,
            String role) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record ProfileRequest(@NotBlank(message = "Name is required") @Size(max = 120, message = "Name is too long") String name,
            Boolean emailNotifications) {}

    public record ForgotRequest(@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email) {}

    public record ResetRequest(
            @NotBlank(message = "Token is required") String token,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password) {}

    public record InvitationRequest(@NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email) {}

    public record InvitationResponse(String email, String link, Instant expiresAt) {}

    public record UserResponse(Long id, String email, String name, String role, boolean hasPassword, boolean emailNotifications, Instant createdAt) {}

    public record ProvidersResponse(boolean google, boolean teacherSignup) {}
}
