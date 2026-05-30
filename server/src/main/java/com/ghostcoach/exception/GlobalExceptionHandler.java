package com.ghostcoach.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response mapping for the entire API.
 * Keeps controllers free of try-catch blocks and ensures all errors return
 * a consistent {@code { "message": "..." }} JSON body regardless of where
 * in the stack the exception was thrown.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles explicitly thrown {@link ApiException}s where the caller
     * has already chosen the appropriate HTTP status code.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Catch-all for {@link RuntimeException}s thrown from services.
     * Infers 404 from "not found" in the message — a lightweight convention
     * that avoids importing the exception package in every service class.
     * Everything else maps to 400 Bad Request.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(Map.of("message", ex.getMessage() != null ? ex.getMessage() : "An error occurred."));
    }

    /**
     * Formats Bean Validation failures ({@code @Valid} on request bodies) into a
     * single comma-separated string. Surfaces the field name so the frontend can
     * highlight the specific form field that failed validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    /**
     * Handles files that exceed {@code spring.servlet.multipart.max-file-size} (5MB).
     * Spring throws this before the controller is reached, so it must be caught here
     * rather than in the controller itself.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleFileSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "File size exceeds the 5MB limit."));
    }
}
