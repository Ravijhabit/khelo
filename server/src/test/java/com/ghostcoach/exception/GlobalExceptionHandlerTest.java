package com.ghostcoach.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApi_returnsConfiguredStatusAndMessage() {
        ApiException ex = new ApiException("Access denied", HttpStatus.FORBIDDEN);
        ResponseEntity<Map<String, String>> response = handler.handleApi(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "Access denied");
    }

    @Test
    void handleRuntime_notFoundMessage_returns404() {
        RuntimeException ex = new RuntimeException("Session not found.");
        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handleRuntime_genericMessage_returns400() {
        RuntimeException ex = new RuntimeException("Email already registered.");
        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_nullMessage_returns400WithFallback() {
        RuntimeException ex = new RuntimeException((String) null);
        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "An error occurred.");
    }

    @Test
    void handleValidation_returnsFieldNameAndMessage() throws Exception {
        // addError bypasses bean introspection — avoids needing a real POJO with matching getters
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "must not be blank"));

        Method method = getClass().getDeclaredMethod("handleValidation_returnsFieldNameAndMessage");
        MethodParameter param = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).contains("email").contains("must not be blank");
    }

    @Test
    void handleFileSize_returns413WithMessage() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5_242_880L);
        ResponseEntity<Map<String, String>> response = handler.handleFileSize(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).containsKey("message");
    }
}
