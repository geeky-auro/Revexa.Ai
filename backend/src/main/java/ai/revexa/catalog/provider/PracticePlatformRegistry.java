package ai.revexa.catalog.provider;

import ai.revexa.core.error.ApiException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves the provider for an import, preferring the most specific one that can handle it. */
@Component
public class PracticePlatformRegistry {

    private final List<PracticePlatformProvider> providers;

    public PracticePlatformRegistry(List<PracticePlatformProvider> providers) {
        this.providers = providers;
    }

    public List<PracticePlatformProvider> all() {
        return providers;
    }

    public Optional<PracticePlatformProvider> byId(String id) {
        return providers.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /**
     * Picks a provider for the payload. The structured and URL-aware providers get first refusal;
     * manual entry is the guaranteed fallback, which is why import never fails for want of a provider.
     */
    public PracticePlatformProvider resolve(String requestedId, PracticePlatformProvider.ImportRequest request) {
        if (requestedId != null && !requestedId.isBlank()) {
            PracticePlatformProvider provider =
                    byId(requestedId)
                            .orElseThrow(() -> ApiException.badRequest("Unknown practice platform provider: " + requestedId));
            if (!provider.supports(request)) {
                throw ApiException.badRequest(
                        provider.displayName() + " cannot read this payload. Expected one of: "
                                + String.join(", ", provider.capabilities().acceptedInputs()));
            }
            return provider;
        }
        return providers.stream()
                .filter(p -> !p.id().equals("manual"))
                .filter(p -> p.supports(request))
                .findFirst()
                .orElseGet(
                        () ->
                                byId("manual")
                                        .orElseThrow(() -> new IllegalStateException("Manual provider must be registered")));
    }
}
