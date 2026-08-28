package ai.revexa.identity.api;

import ai.revexa.core.security.CurrentUser;
import ai.revexa.identity.dto.IdentityDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Create an account and receive a token pair")
    public ResponseEntity<IdentityDtos.AuthResponse> register(
            @Valid @RequestBody IdentityDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Exchange credentials for a token pair")
    public IdentityDtos.AuthResponse login(@Valid @RequestBody IdentityDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "Exchange a refresh token for a fresh access token")
    public IdentityDtos.AuthResponse refresh(@Valid @RequestBody IdentityDtos.RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @GetMapping("/auth/me")
    @Operation(summary = "The signed-in user's profile")
    public IdentityDtos.UserView me() {
        return authService.toView(authService.require(CurrentUser.requireId()));
    }

    @PatchMapping("/users/me")
    @Operation(summary = "Update display name, theme, default language or mentor mode")
    public IdentityDtos.UserView updateProfile(
            @Valid @RequestBody IdentityDtos.UpdateProfileRequest request) {
        return authService.updateProfile(CurrentUser.requireId(), request);
    }
}
