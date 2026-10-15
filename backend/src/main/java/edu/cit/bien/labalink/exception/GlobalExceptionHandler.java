package edu.cit.bien.labalink.exception;

import edu.cit.bien.labalink.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus())
            .body(new ApiResponse(false, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
            .stream()
            .map(err -> err.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false,
                message.isBlank() ? "Invalid request" : message));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse> handleUnreadable(Exception e) {
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false, "Malformed request"));
    }

    @ExceptionHandler({
        AccessDeniedException.class,
        AuthorizationDeniedException.class
    })
    public ResponseEntity<ApiResponse> handleDenied(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiResponse(false, "Access denied"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleMethod(Exception e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new ApiResponse(false, "Method not allowed"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleMediaType(Exception e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(new ApiResponse(false, "Unsupported media type"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResource(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse(false, "Not found"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleIntegrity(
            DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}",
            e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiResponse(false,
                "Request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse(false,
                "Something went wrong. Please try again."));
    }
}
