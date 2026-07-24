package com.necro.devolucionesback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.necro.devolucionesback.dto.EventoResponseDTO;
import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.exception.InvalidStateTransitionException;
import com.necro.devolucionesback.model.Estado;
import com.necro.devolucionesback.model.Origen;
import com.necro.devolucionesback.service.SolicitudService;
import com.necro.devolucionesback.service.CustomUserDetailsService;
import com.necro.devolucionesback.service.jwtUtils.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SolicitudController.class)
@Import(SolicitudControllerTest.TestSecurityConfig.class)
class SolicitudControllerTest {

    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudService solicitudService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SolicitudResponseDTO buildResponse(Long id, Estado estado) {
        return new SolicitudResponseDTO(
                id, "DEV-2026-000001", "12345678-5", "MARIA PEREZ",
                150000.0, 1L, "BANCO CHILE", "001234567890",
                estado, null, "analista1", "analista1",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes -> 201 con Location")
    @WithMockUser
    void create_validRequest_returns201() throws Exception {
        SolicitudRequestDTO request = new SolicitudRequestDTO(
                "12345678-5", "MARIA PEREZ", 150000.0, 1L, "001234567890");
        SolicitudResponseDTO response = buildResponse(1L, Estado.BORRADOR);

        given(solicitudService.CrearSolicitud(any(SolicitudRequestDTO.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.rutCliente").value("12345678-5"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes con campos invalidos -> 400")
    @WithMockUser
    void create_invalidRequest_returns400() throws Exception {
        String invalidJson = "{\"rutCliente\":\"\",\"nombreCliente\":\"\",\"monto\":-1}";

        mockMvc.perform(post("/api/v1/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes sin autenticacion -> 401/403")
    void create_noAuth_returns401or403() throws Exception {
        SolicitudRequestDTO request = new SolicitudRequestDTO(
                "12345678-5", "MARIA PEREZ", 150000.0, 1L, "001234567890");

        mockMvc.perform(post("/api/v1/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/solicitudes -> 200 paginado")
    @WithMockUser
    void findAll_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.BORRADOR);
        given(solicitudService.findAll(any(), any(), any(), any(), any(), eq(0), eq(10)))
                .willReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/solicitudes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("GET /api/v1/solicitudes/{id} -> 200")
    @WithMockUser
    void findById_validId_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.BORRADOR);
        given(solicitudService.findById(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/solicitudes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.folio").value("DEV-2026-000001"));
    }

    @Test
    @DisplayName("GET /api/v1/solicitudes/{id} con id inexistente -> 404")
    @WithMockUser
    void findById_invalidId_returns404() throws Exception {
        given(solicitudService.findById(99L))
                .willThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada con id: 99"));

        mockMvc.perform(get("/api/v1/solicitudes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/solicitudes/{id}/historial -> 200")
    @WithMockUser
    void getHistorial_validId_returns200() throws Exception {
        EventoResponseDTO evento = EventoResponseDTO.builder()
                .id(1L)
                .estadoOrigen(null)
                .estadoDestino(Estado.BORRADOR)
                .usuario("analista1")
                .fecha(LocalDateTime.now())
                .comentario("Solicitud creada")
                .build();
        given(solicitudService.getHistorial(1L)).willReturn(List.of(evento));

        mockMvc.perform(get("/api/v1/solicitudes/1/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoDestino").value("BORRADOR"));
    }

    @Test
    @DisplayName("PUT /api/v1/solicitudes/{id} -> 200")
    @WithMockUser
    void updateSolicitud_validRequest_returns200() throws Exception {
        SolicitudRequestDTO request = new SolicitudRequestDTO(
                "12345678-5", "MARIA PEREZ ACTUALIZADA", 200000.0, 1L, "001234567890");
        SolicitudResponseDTO response = buildResponse(1L, Estado.BORRADOR);
        given(solicitudService.updateSolicitud(eq(1L), any(SolicitudRequestDTO.class))).willReturn(response);

        mockMvc.perform(put("/api/v1/solicitudes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/solicitudes/{id} en estado no BORRADOR -> 409")
    @WithMockUser
    void updateSolicitud_noBorrador_returns409() throws Exception {
        SolicitudRequestDTO request = new SolicitudRequestDTO(
                "12345678-5", "MARIA PEREZ", 200000.0, 1L, "001234567890");
        given(solicitudService.updateSolicitud(eq(1L), any(SolicitudRequestDTO.class)))
                .willThrow(new InvalidStateTransitionException(
                        "No se puede actualizar: solo editable en BORRADOR"));

        mockMvc.perform(put("/api/v1/solicitudes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/enviar -> 200 (ANALISTA)")
    @WithMockUser(authorities = "ANALISTA")
    void enviar_asAnalista_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.EN_REVISION);
        given(solicitudService.cambiarEstadoSolicitud(1L, "enviar", null)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/enviar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/enviar sin autoridad -> 403")
    @WithMockUser
    void enviar_withoutAuthority_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes/1/enviar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/aprobar -> 200 (SUPERVISOR)")
    @WithMockUser(authorities = "SUPERVISOR")
    void aprobar_asSupervisor_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.APROBADA);
        given(solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/aprobar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/rechazar -> 200 (SUPERVISOR)")
    @WithMockUser(authorities = "SUPERVISOR")
    void rechazar_asSupervisor_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.RECHAZADA);
        String motivo = "Monto incorrecto";
        given(solicitudService.cambiarEstadoSolicitud(1L, "rechazar", motivo)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoRechazo\":\"Monto incorrecto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/rechazar sin motivo -> 400")
    @WithMockUser(authorities = "SUPERVISOR")
    void rechazar_sinMotivo_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes/1/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoRechazo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/pagar -> 200 (SUPERVISOR)")
    @WithMockUser(authorities = "SUPERVISOR")
    void pagar_asSupervisor_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.PAGADA);
        given(solicitudService.cambiarEstadoSolicitud(1L, "pagar", null)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/anular -> 200 (ANALISTA)")
    @WithMockUser(authorities = "ANALISTA")
    void anular_asAnalista_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.ANULADA);
        given(solicitudService.cambiarEstadoSolicitud(1L, "anular", null)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/reabrir -> 200 (ANALISTA)")
    @WithMockUser(authorities = "ANALISTA")
    void reabrir_asAnalista_returns200() throws Exception {
        SolicitudResponseDTO response = buildResponse(1L, Estado.BORRADOR);
        given(solicitudService.cambiarEstadoSolicitud(1L, "reabrir", null)).willReturn(response);

        mockMvc.perform(post("/api/v1/solicitudes/1/reabrir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("POST /api/v1/solicitudes/{id}/enviar estado invalido -> 409")
    @WithMockUser(authorities = "ANALISTA")
    void enviar_estadoInvalido_returns409() throws Exception {
        willThrow(new InvalidStateTransitionException(
                "No se puede enviar: estado actual es EN_REVISION"))
                .given(solicitudService).cambiarEstadoSolicitud(1L, "enviar", null);

        mockMvc.perform(post("/api/v1/solicitudes/1/enviar"))
                .andExpect(status().isConflict());
    }
}
