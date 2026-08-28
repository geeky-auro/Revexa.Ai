package ai.revexa.core.security;

import ai.revexa.core.error.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

/** Static accessor for the caller, so services do not have to thread the principal everywhere. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<AuthenticatedUser> find() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public static AuthenticatedUser require() {
        return find().orElseThrow(() -> ApiException.unauthorized("Authentication is required"));
    }

    public static UUID requireId() {
        return require().id();
    }
}
