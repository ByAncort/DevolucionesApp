package com.necro.devolucionesback.service.jwtUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necro.devolucionesback.dto.AuthErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtUtils;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            final String token = getTokenFromRequest(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authenticateUser(token, request, response)) {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            logger.error("Error en la autenticación: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error en la autenticación", request.getServletPath());
        }
    }

    private boolean authenticateUser(String token, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String username = jwtUtils.getUsernameFromToken(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtils.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    return true;
                }
            }
            return false;
        } catch (ExpiredJwtException e) {
            logger.error("Token expirado: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token expirado", request.getServletPath());
        } catch (MalformedJwtException e) {
            logger.error("Token inválido: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Token inválido", request.getServletPath());
        } catch (SignatureException e) {
            logger.error("Firma del token no válida: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Firma del token no válida", request.getServletPath());
        } catch (IllegalArgumentException e) {
            logger.error("Token ausente o incorrecto: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Token ausente o incorrecto", request.getServletPath());
        } catch (UsernameNotFoundException e) {
            logger.error("Usuario no encontrado: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Usuario no encontrado", request.getServletPath());
        }
        return false;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message, String path)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        AuthErrorResponse body = AuthErrorResponse.of(status, getErrorLabel(status), message, path);
        mapper.writeValue(response.getOutputStream(), body);
    }

    private String getErrorLabel(int status) {
        return switch (status) {
            case HttpServletResponse.SC_UNAUTHORIZED -> "Unauthorized";
            case HttpServletResponse.SC_BAD_REQUEST -> "Bad Request";
            default -> "Internal Server Error";
        };
    }

    private String getTokenFromRequest(HttpServletRequest request){
        final String authHeader=request.getHeader(HttpHeaders.AUTHORIZATION);
        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
