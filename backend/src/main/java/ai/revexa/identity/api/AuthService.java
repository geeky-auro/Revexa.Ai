package ai.revexa.identity.api;

import ai.revexa.core.error.ApiException;
import ai.revexa.core.security.JwtService;
import ai.revexa.core.security.TokenPair;
import ai.revexa.identity.domain.UserAccount;
import ai.revexa.identity.domain.UserAccountRepository;
import ai.revexa.identity.dto.IdentityDtos;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final List<String> THEMES = List.of("system", "light", "dark");
    private static final List<String> MENTOR_MODES = List.of("guided", "direct");

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public IdentityDtos.AuthResponse register(IdentityDtos.RegisterRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account already exists for that email address");
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setDisplayName(request.displayName().strip());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastActiveAt(Instant.now());
        return authResponse(users.save(user));
    }

    @Transactional
    public IdentityDtos.AuthResponse login(IdentityDtos.LoginRequest request) {
        UserAccount user =
                users.findByEmailIgnoreCase(request.email().strip())
                        .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email or password is incorrect");
        }
        user.setLastActiveAt(Instant.now());
        return authResponse(user);
    }

    @Transactional
    public IdentityDtos.AuthResponse refresh(String refreshToken) {
        UUID userId = jwtService.verifyRefreshToken(refreshToken);
        UserAccount user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserAccount require(UUID id) {
        return users.findById(id).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    @Transactional
    public IdentityDtos.UserView updateProfile(UUID id, IdentityDtos.UpdateProfileRequest request) {
        UserAccount user = require(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().strip());
        }
        if (request.theme() != null && !request.theme().isBlank()) {
            String theme = request.theme().toLowerCase(Locale.ROOT);
            if (!THEMES.contains(theme)) {
                throw ApiException.badRequest("Theme must be one of: " + String.join(", ", THEMES));
            }
            user.setTheme(theme);
        }
        if (request.preferredLanguage() != null && !request.preferredLanguage().isBlank()) {
            user.setPreferredLanguage(request.preferredLanguage().toLowerCase(Locale.ROOT));
        }
        if (request.mentorMode() != null && !request.mentorMode().isBlank()) {
            String mode = request.mentorMode().toLowerCase(Locale.ROOT);
            if (!MENTOR_MODES.contains(mode)) {
                throw ApiException.badRequest("Mentor mode must be one of: " + String.join(", ", MENTOR_MODES));
            }
            user.setMentorMode(mode);
        }
        return toView(user);
    }

    private IdentityDtos.AuthResponse authResponse(UserAccount user) {
        TokenPair tokens = jwtService.issue(user);
        return new IdentityDtos.AuthResponse(
                toView(user), tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds());
    }

    public IdentityDtos.UserView toView(UserAccount user) {
        return new IdentityDtos.UserView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getTheme(),
                user.getPreferredLanguage(),
                user.getMentorMode(),
                user.getCreatedAt());
    }
}
