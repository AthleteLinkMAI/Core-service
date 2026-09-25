package team.anonyms.athletelink.coreservice.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Schema(description = "Error returned by the Core Service API")
    public record ApiError(
            @Schema(description = "Stable machine-readable error category", example = "VALIDATION_ERROR") String code,
            @Schema(description = "Human-readable explanation", example = "Request body validation failed") String message,
            @Schema(description = "Field or parameter validation errors; empty for other errors") Map<String, String> details
    ) {
        public ApiError(String code, String message) {
            this(code, message, Map.of());
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenOperationException ex) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex) {
        return respond(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                details.put(error.getObjectName(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(
                new ApiError("VALIDATION_ERROR", "Request body validation failed", details));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            String name = parameter != null ? parameter : "parameter";
            result.getResolvableErrors().forEach(error ->
                    details.put(name, error.getDefaultMessage()));
        });
        return ResponseEntity.badRequest().body(
                new ApiError("VALIDATION_ERROR", "Request parameter validation failed", details));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex) {
        logger.debug("Malformed request", ex);
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing or malformed request data");
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
