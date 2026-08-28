package ai.revexa.core.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * The single error envelope every endpoint returns, so the client only has to know one shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        List<FieldError> fieldErrors,
        String path,
        Instant timestamp) {

    public record FieldError(String field, String message) {}

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, null, path, Instant.now());
    }

    public static ApiError validation(List<FieldError> fieldErrors, String path) {
        return new ApiError(
                "validation_failed", "Some fields were invalid", fieldErrors, path, Instant.now());
    }
}
