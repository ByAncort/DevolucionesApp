package com.necro.devolucionesback.controller;

import com.necro.devolucionesback.dto.CargaDetalleResponseDTO;
import com.necro.devolucionesback.dto.CargaResponseDTO;
import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.CargaErrorRepository;
import com.necro.devolucionesback.repository.CargaRepository;
import com.necro.devolucionesback.service.CargaService;
import com.necro.devolucionesback.service.CustomUserDetailsService;
import com.necro.devolucionesback.service.jwtUtils.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(CargaController.class)
@Import(CargaControllerTest.TestSecurityConfig.class)
class CargaControllerTest {

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
    private CargaService cargaService;

    @MockitoBean
    private CargaRepository cargaRepository;

    @MockitoBean
    private CargaErrorRepository cargaErrorRepository;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private User buildAnalistaUser() {
        return User.builder()
                .id(1L)
                .username("analista1")
                .email("analista@test.com")
                .roles(Set.of(Role.builder().id(1L).name("ANALISTA").build()))
                .build();
    }

    private Carga buildCarga() {
        return Carga.builder()
                .id(1L)
                .nombreArchivo("pagos.csv")
                .totalFilas(100)
                .filasOk(95)
                .filasRechazadas(5)
                .estado(CargaEstado.CON_ERRORES)
                .usuario(buildAnalistaUser())
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/cargas con CSV valido -> 201")
    @WithMockUser(authorities = "ANALISTA")
    void upload_validCSV_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pagos.csv", "text/csv",
                "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                        .getBytes());

        Carga carga = buildCarga();
        given(customUserDetailsService.getCurrentUser()).willReturn(buildAnalistaUser());
        given(cargaService.procesarCSV(any(), any())).willReturn(carga);

        mockMvc.perform(multipart("/api/v1/cargas").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreArchivo").value("pagos.csv"))
                .andExpect(jsonPath("$.filasOk").value(95))
                .andExpect(jsonPath("$.filasRechazadas").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/cargas sin autenticacion -> 401")
    void upload_noAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pagos.csv", "text/csv",
                "data".getBytes());

        mockMvc.perform(multipart("/api/v1/cargas").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/cargas sin autoridad ANALISTA -> 403")
    @WithMockUser(authorities = "SUPERVISOR")
    void upload_withoutAnalistaRole_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pagos.csv", "text/csv",
                "data".getBytes());

        mockMvc.perform(multipart("/api/v1/cargas").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/cargas/{id} -> 200 con detalle")
    @WithMockUser
    void getDetalle_validId_returns200() throws Exception {
        Carga carga = buildCarga();
        CargaError error = CargaError.builder()
                .id(1L)
                .carga(carga)
                .numFila(15)
                .campo("rut_cliente")
                .motivo("RUT invalido")
                .build();

        given(cargaRepository.findById(1L)).willReturn(Optional.of(carga));
        given(cargaErrorRepository.findByCargaIdOrderByNumFilaAsc(1L)).willReturn(List.of(error));

        mockMvc.perform(get("/api/v1/cargas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carga.id").value(1))
                .andExpect(jsonPath("$.carga.nombreArchivo").value("pagos.csv"))
                .andExpect(jsonPath("$.errores", hasSize(1)))
                .andExpect(jsonPath("$.errores[0].numFila").value(15))
                .andExpect(jsonPath("$.errores[0].campo").value("rut_cliente"));
    }

    @Test
    @DisplayName("GET /api/v1/cargas/{id} con id inexistente -> 404")
    @WithMockUser
    void getDetalle_invalidId_returns404() throws Exception {
        given(cargaRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/cargas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/cargas/{id} sin errores -> 200 lista vacia")
    @WithMockUser
    void getDetalle_noErrors_returns200EmptyList() throws Exception {
        Carga carga = buildCarga();
        carga.setFilasRechazadas(0);
        carga.setEstado(CargaEstado.COMPLETADA);

        given(cargaRepository.findById(1L)).willReturn(Optional.of(carga));
        given(cargaErrorRepository.findByCargaIdOrderByNumFilaAsc(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/cargas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errores", hasSize(0)));
    }
}
