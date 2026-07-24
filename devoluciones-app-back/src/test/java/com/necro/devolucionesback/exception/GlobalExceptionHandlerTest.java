package com.necro.devolucionesback.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("ResponseStatusException 404 -> retorna 404 con mensaje")
    void handleResponseStatus_404_returnsNotFound() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Solicitud no encontrada con id: 99");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Not Found");
        assertThat(response.getBody().get("message").toString()).contains("no encontrada");
    }

    @Test
    @DisplayName("ResponseStatusException 409 -> retorna 409 con Conflict")
    void handleResponseStatus_409_returnsConflict() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.CONFLICT, "Transicion invalida");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().get("error")).isEqualTo("Conflict");
    }

    @Test
    @DisplayName("ResponseStatusException 400 -> retorna 400 con Bad Request")
    void handleResponseStatus_400_returnsBadRequest() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Accion no valida: invalidar");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("ResponseStatusException 403 -> retorna 403 con Forbidden")
    void handleResponseStatus_403_returnsForbidden() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Se requiere rol SUPERVISOR");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().get("error")).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("UserAlreadyExistsException -> 409 Conflict")
    void handleUserAlreadyExists_returnsConflict() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Email ya existe: test@test.com");

        ResponseEntity<Map<String, Object>> response = handler.handleUserAlreadyExists(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().get("error")).isEqualTo("Conflict");
        assertThat(response.getBody().get("message").toString()).contains("Email ya existe");
    }

    @Test
    @DisplayName("InvalidStateTransitionException -> 409 Conflict")
    void handleInvalidStateTransition_returnsConflict() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException(
                "No se puede enviar: estado actual es EN_REVISION");

        ResponseEntity<Map<String, Object>> response = handler.handleInvalidStateTransition(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().get("error")).isEqualTo("Conflict");
        assertThat(response.getBody().get("message").toString()).contains("enviar");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 Bad Request")
    void handleIllegalArgument_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("El banco seleccionado no existe");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        assertThat(response.getBody().get("message").toString()).contains("banco");
    }

    @Test
    @DisplayName("Exception generica -> 500 Internal Server Error")
    void handleGeneric_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected failure");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("RuntimeException -> 500 Internal Server Error")
    void handleRuntimeException_returnsInternalServerError() {
        RuntimeException ex = new RuntimeException("DB connection failed");

        ResponseEntity<Map<String, Object>> response = handler.runtimeException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("Response body contiene timestamp y path")
    void handleAny_containsTimestampAndPath() {
        IllegalArgumentException ex = new IllegalArgumentException("test error");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("path")).isEqualTo("/api/v1/test");
    }
}
