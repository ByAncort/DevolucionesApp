package com.necro.devolucionesback.service.jwtUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necro.devolucionesback.dto.AuthErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccessDeniedHandlerJwt implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedHandlerJwt.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        log.error("Access denied: {}", accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        AuthErrorResponse body = AuthErrorResponse.of(
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Access denied: you do not have permission to access this resource",
                request.getServletPath()
        );

        mapper.writeValue(response.getOutputStream(), body);
    }
}
