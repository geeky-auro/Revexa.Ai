package ai.revexa.identity.dto;

import ai.revexa.identity.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class IdentityDtos {

    private IdentityDtos() {}

    public record RegisterRequest(
            @Email @NotBlank @Size(max = 320) String email,
            @NotBlank @Size(min = 2, max = 120) String displayName,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UpdateProfileRequest(
            @Size(min = 2, max = 120) String displayName,
            @Size(max = 16) String theme,
            @Size(max = 32) String preferredLanguage,
            @Size(max = 16) String mentorMode) {}

    public record UserView(
            UUID id,
            String email,
            String displayName,
            UserRole role,
            String theme,
            String preferredLanguage,
            String mentorMode,
            Instant createdAt) {}

    public record AuthResponse(UserView user, String accessToken, String refreshToken, long expiresInSeconds) {}
}
