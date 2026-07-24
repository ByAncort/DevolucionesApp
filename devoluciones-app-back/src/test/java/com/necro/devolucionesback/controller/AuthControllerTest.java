package com.necro.devolucionesback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.dto.RegisterRequest;
import com.necro.devolucionesback.exception.UserAlreadyExistsException;
import com.necro.devolucionesback.service.AuthService;
import com.necro.devolucionesback.service.CustomUserDetailsService;
import com.necro.devolucionesback.service.jwtUtils.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/auth/login").permitAll()
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales validas -> 200 con token")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("analista@test.com", "password123");
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.issuedAt").exists())
                .andExpect(jsonPath("$.expiration").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con email invalido -> 401")
    void login_invalidEmail_returns401() throws Exception {
        LoginRequest request = new LoginRequest("noexiste@test.com", "password123");

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con campos vacios -> 400")
    void login_emptyFields_returns400() throws Exception {
        String invalidJson = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con email formato invalido -> 400")
    void login_invalidEmailFormat_returns400() throws Exception {
        String invalidJson = "{\"email\":\"no-email\",\"password\":\"password123\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register como SUPERVISOR -> 200")
    @WithMockUser(authorities = "SUPERVISOR")
    void register_asSupervisor_returns200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("nuevoanalista")
                .email("nuevo@test.com")
                .password("password123")
                .build();
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-456")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .build();

        given(authService.createUser(any(RegisterRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-456"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register sin SUPERVISOR -> 403")
    @WithMockUser
    void register_withoutSupervisorRole_returns403() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("nuevoanalista")
                .email("nuevo@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register sin autenticacion -> 401")
    void register_noAuth_returns401() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("nuevoanalista")
                .email("nuevo@test.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register email duplicado -> 409")
    @WithMockUser(authorities = "SUPERVISOR")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("otro")
                .email("analista@test.com")
                .password("password123")
                .build();

        given(authService.createUser(any(RegisterRequest.class)))
                .willThrow(new UserAlreadyExistsException("Email ya existe: analista@test.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register campos invalidos -> 400")
    @WithMockUser(authorities = "SUPERVISOR")
    void register_invalidFields_returns400() throws Exception {
        String invalidJson = "{\"username\":\"\",\"email\":\"not-email\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
