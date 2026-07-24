package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.exception.InvalidStateTransitionException;
import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.BancoRepository;
import com.necro.devolucionesback.repository.EventoSolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private BancoRepository bancoRepository;
    @Mock
    private EventoSolicitudRepository eventoSolicitudRepository;
    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private SolicitudService solicitudService;

    @Captor
    private ArgumentCaptor<Solicitud> solicitudCaptor;
    @Captor
    private ArgumentCaptor<EventoSolicitud> eventoCaptor;

    private User analistaUser;
    private User supervisorUser;
    private Banco banco;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        analistaUser = User.builder()
                .id(1L)
                .username("analista1")
                .email("analista@test.com")
                .roles(Set.of(Role.builder().id(1L).name("ANALISTA").build()))
                .build();

        supervisorUser = User.builder()
                .id(2L)
                .username("supervisor1")
                .email("supervisor@test.com")
                .roles(Set.of(Role.builder().id(2L).name("SUPERVISOR").build()))
                .build();

        banco = Banco.builder().id(1L).nombreBanco("BANCO CHILE").build();

        solicitud = Solicitud.builder()
                .id(1L)
                .folio("DEV-2026-000001")
                .rutCliente("12345678-5")
                .nombreCliente("MARIA PEREZ")
                .monto(150000.0)
                .bancoDestino(banco)
                .cuentaDestino("001234567890")
                .estado(Estado.BORRADOR)
                .origen(Origen.MANUAL)
                .createdBy(analistaUser)
                .updatedBy(analistaUser)
                .vecesReabierta(0)
                .build();
    }

    @Nested
    @DisplayName("CrearSolicitud - creacion manual")
    class CrearSolicitudTest {

        @Test
        @DisplayName("Crear solicitud valida -> retorna DTO con folio y estado BORRADOR")
        void crearSolicitud_validRequest_returnsDtoWithFolioAndBorrador() {
            SolicitudRequestDTO request = new SolicitudRequestDTO(
                    "12345678-5", "MARIA PEREZ", 150000.0, 1L, "001234567890");

            given(bancoRepository.findById(1L)).willReturn(Optional.of(banco));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.nextFolio()).willReturn(1L);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.CrearSolicitud(request);

            assertThat(response).isNotNull();
            assertThat(response.estado()).isEqualTo(Estado.BORRADOR);
            assertThat(response.rutCliente()).isEqualTo("12345678-5");
            assertThat(response.monto()).isEqualTo(150000.0);
        }

        @Test
        @DisplayName("R6 - Crear solicitud registra EventoSolicitud en misma transaccion")
        void crearSolicitud_registersEvent() {
            SolicitudRequestDTO request = new SolicitudRequestDTO(
                    "12345678-5", "MARIA PEREZ", 150000.0, 1L, "001234567890");

            given(bancoRepository.findById(1L)).willReturn(Optional.of(banco));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.nextFolio()).willReturn(1L);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            solicitudService.CrearSolicitud(request);

            then(eventoSolicitudRepository).should().save(eventoCaptor.capture());
            EventoSolicitud evento = eventoCaptor.getValue();
            assertThat(evento.getEstadoOrigen()).isNull();
            assertThat(evento.getEstadoDestino()).isEqualTo(Estado.BORRADOR);
            assertThat(evento.getUsuario()).isEqualTo(analistaUser);
            assertThat(evento.getComentario()).contains("creada");
        }

        @Test
        @DisplayName("Banco inexistente -> lanza IllegalArgumentException")
        void crearSolicitud_bancoNotFound_throwsException() {
            SolicitudRequestDTO request = new SolicitudRequestDTO(
                    "12345678-5", "MARIA PEREZ", 150000.0, 99L, "001234567890");

            given(bancoRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudService.CrearSolicitud(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("banco");
        }
    }

    @Nested
    @DisplayName("R1 - Maquina de estados")
    class MaquinaEstadosTest {

        @Test
        @DisplayName("R1 - Enviar: BORRADOR -> EN_REVISION")
        void enviar_borradorToEnRevision() {
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "enviar", null);

            assertThat(response.estado()).isEqualTo(Estado.EN_REVISION);
        }

        @Test
        @DisplayName("R1 - Enviar desde EN_REVISION -> 409 InvalidStateTransitionException")
        void enviar_enRevision_throwsInvalidState() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "enviar", null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("enviar");
        }

        @Test
        @DisplayName("R1 - Aprobar: EN_REVISION -> APROBADA (requiere SUPERVISOR)")
        void aprobar_enRevisionToAprobada() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null);

            assertThat(response.estado()).isEqualTo(Estado.APROBADA);
        }

        @Test
        @DisplayName("R1 - Aprobar desde BORRADOR -> 409")
        void aprobar_borrador_throwsInvalidState() {
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("aprobar");
        }

        @Test
        @DisplayName("R1 - Pagar: APROBADA -> PAGADA (requiere SUPERVISOR)")
        void pagar_aprobadaToPagada() {
            solicitud.setEstado(Estado.APROBADA);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "pagar", null);

            assertThat(response.estado()).isEqualTo(Estado.PAGADA);
        }

        @Test
        @DisplayName("R1 - Pagar desde EN_REVISION -> 409")
        void pagar_enRevision_throwsInvalidState() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "pagar", null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("pagar");
        }

        @Test
        @DisplayName("R1 - Anular: BORRADOR -> ANULADA")
        void anular_borradorToAnulada() {
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "anular", null);

            assertThat(response.estado()).isEqualTo(Estado.ANULADA);
        }

        @Test
        @DisplayName("R1 - Anular desde EN_REVISION -> 409")
        void anular_enRevision_throwsInvalidState() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "anular", null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("anular");
        }

        @Test
        @DisplayName("Accion invalida -> 400 Bad Request")
        void accionInvalida_throwsBadRequest() {
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "invalidar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Accion no valida");
        }

        @Test
        @DisplayName("Solicitud inexistente -> 404")
        void cambiarEstado_solicitudNotFound_throwsNotFound() {
            given(solicitudRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(99L, "enviar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }

    @Nested
    @DisplayName("R2 - Control de roles")
    class ControlRolesTest {

        @Test
        @DisplayName("R2 - Aprobar sin rol SUPERVISOR -> 403")
        void aprobar_analista_throwsForbidden() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("SUPERVISOR");
        }

        @Test
        @DisplayName("R2 - Rechazar sin rol SUPERVISOR -> 403")
        void rechazar_analista_throwsForbidden() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "rechazar", "Motivo"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("SUPERVISOR");
        }

        @Test
        @DisplayName("R2 - Pagar sin rol SUPERVISOR -> 403")
        void pagar_analista_throwsForbidden() {
            solicitud.setEstado(Estado.APROBADA);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "pagar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("SUPERVISOR");
        }
    }

    @Nested
    @DisplayName("R3 - Rechazo con motivo obligatorio")
    class RechazoMotivoTest {

        @Test
        @DisplayName("R3 - Rechazar sin motivo -> 400")
        void rechazar_sinMotivo_throwsBadRequest() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "rechazar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("R3 - Rechazar con motivo en blanco -> 400")
        void rechazar_motivoEnBlanco_throwsBadRequest() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "rechazar", "  "))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("R3 - Rechazar con motivo valido -> RECHAZADA")
        void rechazar_conMotivo_rechazada() {
            solicitud.setEstado(Estado.EN_REVISION);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(
                    1L, "rechazar", "Monto no coincide con comprobante");

            assertThat(response.estado()).isEqualTo(Estado.RECHAZADA);
        }

        @Test
        @DisplayName("R3 - Rechazo guarda motivo en solicitud")
        void rechazar_conMotivo_guardaMotivoEnSolicitud() {
            solicitud.setEstado(Estado.EN_REVISION);
            String motivo = "Documentacion incompleta";
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            solicitudService.cambiarEstadoSolicitud(1L, "rechazar", motivo);

            then(solicitudRepository).should().save(solicitudCaptor.capture());
            assertThat(solicitudCaptor.getValue().getMotivoRechazo()).isEqualTo(motivo);
        }
    }

    @Nested
    @DisplayName("R4 - Reabrir maximo 1 vez")
    class ReabrirTest {

        @Test
        @DisplayName("R4 - Reabrir RECHAZADA (0 reaberturas) -> BORRADOR")
        void reabrir_primerIntento_borrador() {
            solicitud.setEstado(Estado.RECHAZADA);
            solicitud.setVecesReabierta(0);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "reabrir", null);

            assertThat(response.estado()).isEqualTo(Estado.BORRADOR);
        }

        @Test
        @DisplayName("R4 - Reabrir RECHAZADA (ya reabierta 1 vez) -> 409")
        void reabrir_segundaVez_throwsConflict() {
            solicitud.setEstado(Estado.RECHAZADA);
            solicitud.setVecesReabierta(1);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "reabrir", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("reabrir");
        }

        @Test
        @DisplayName("R4 - Reabrir incrementa contador y limpia motivoRechazo")
        void reabrir_primerIntento_incrementaContadorYLimpiaMotivo() {
            solicitud.setEstado(Estado.RECHAZADA);
            solicitud.setVecesReabierta(0);
            solicitud.setMotivoRechazo("Motivo anterior");
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            solicitudService.cambiarEstadoSolicitud(1L, "reabrir", null);

            then(solicitudRepository).should().save(solicitudCaptor.capture());
            Solicitud saved = solicitudCaptor.getValue();
            assertThat(saved.getVecesReabierta()).isEqualTo(1);
            assertThat(saved.getMotivoRechazo()).isNull();
        }

        @Test
        @DisplayName("R4 - Reabrir desde BORRADOR -> 409")
        void reabrir_borrador_throwsInvalidState() {
            solicitud.setEstado(Estado.BORRADOR);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "reabrir", null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("reabrir");
        }
    }

    @Nested
    @DisplayName("R6 - Registro de eventos")
    class RegistroEventosTest {

        @Test
        @DisplayName("R6 - Cada transicion inserta EventoSolicitud")
        void cambiarEstado_insertaEvento() {
            solicitud.setEstado(Estado.BORRADOR);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            solicitudService.cambiarEstadoSolicitud(1L, "enviar", null);

            then(eventoSolicitudRepository).should().save(eventoCaptor.capture());
            EventoSolicitud evento = eventoCaptor.getValue();
            assertThat(evento.getSolicitud()).isEqualTo(solicitud);
            assertThat(evento.getEstadoOrigen()).isEqualTo(Estado.BORRADOR);
            assertThat(evento.getEstadoDestino()).isEqualTo(Estado.EN_REVISION);
            assertThat(evento.getUsuario()).isEqualTo(analistaUser);
        }

        @Test
        @DisplayName("R6 - Rechazo registra evento con motivo")
        void rechazar_registraEventoConMotivo() {
            solicitud.setEstado(Estado.EN_REVISION);
            String motivo = "Monto excesivo";
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            solicitudService.cambiarEstadoSolicitud(1L, "rechazar", motivo);

            then(eventoSolicitudRepository).should().save(eventoCaptor.capture());
            assertThat(eventoCaptor.getValue().getComentario()).contains(motivo);
        }
    }

    @Nested
    @DisplayName("R7 - Separacion de funciones")
    class SeparacionFuncionesTest {

        @Test
        @DisplayName("R7 - Supervisor que creo la solicitud no puede aprobarla")
        void aprobar_mismoCreador_throwsForbidden() {
            solicitud.setEstado(Estado.EN_REVISION);
            solicitud.setCreatedBy(supervisorUser);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);

            assertThatThrownBy(() -> solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no puede ser el mismo usuario");
        }

        @Test
        @DisplayName("R7 - Supervisor diferente al creador puede aprobar")
        void aprobar_supervisorDiferente_aprueba() {
            solicitud.setEstado(Estado.EN_REVISION);
            solicitud.setCreatedBy(analistaUser);
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(customUserDetailsService.getCurrentUser()).willReturn(supervisorUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.cambiarEstadoSolicitud(1L, "aprobar", null);

            assertThat(response.estado()).isEqualTo(Estado.APROBADA);
        }
    }

    @Nested
    @DisplayName("updateSolicitud - actualizacion")
    class UpdateSolicitudTest {

        @Test
        @DisplayName("Actualizar solicitud en BORRADOR -> exito")
        void updateSolicitud_borrador_returnsUpdated() {
            SolicitudRequestDTO requestDTO = new SolicitudRequestDTO(
                    "87654321-0", "NUEVO NOMBRE", 200000.0, 1L, "999999999");
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));
            given(bancoRepository.findById(1L)).willReturn(Optional.of(banco));
            given(customUserDetailsService.getCurrentUser()).willReturn(analistaUser);
            given(solicitudRepository.save(any(Solicitud.class))).willReturn(solicitud);

            SolicitudResponseDTO response = solicitudService.updateSolicitud(1L, requestDTO);

            assertThat(response).isNotNull();
            then(solicitudRepository).should().save(any(Solicitud.class));
        }

        @Test
        @DisplayName("Actualizar solicitud NO en BORRADOR -> 409")
        void updateSolicitud_noBorrador_throwsInvalidState() {
            solicitud.setEstado(Estado.EN_REVISION);
            SolicitudRequestDTO requestDTO = new SolicitudRequestDTO(
                    "87654321-0", "NUEVO NOMBRE", 200000.0, 1L, "999999999");
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));

            assertThatThrownBy(() -> solicitudService.updateSolicitud(1L, requestDTO))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("Actualizar solicitud inexistente -> 404")
        void updateSolicitud_notFound_throwsNotFound() {
            SolicitudRequestDTO requestDTO = new SolicitudRequestDTO(
                    "87654321-0", "NUEVO NOMBRE", 200000.0, 1L, "999999999");
            given(solicitudRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudService.updateSolicitud(99L, requestDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }

    @Nested
    @DisplayName("findById - busqueda")
    class FindByIdTest {

        @Test
        @DisplayName("Solicitud existente -> retorna DTO")
        void findById_existent_returnsDto() {
            given(solicitudRepository.findById(1L)).willReturn(Optional.of(solicitud));

            SolicitudResponseDTO response = solicitudService.findById(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Solicitud inexistente -> 404")
        void findById_inexistente_throwsNotFound() {
            given(solicitudRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> solicitudService.findById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }

    @Nested
    @DisplayName("getHistorial - historico de eventos")
    class GetHistorialTest {

        @Test
        @DisplayName("Historial de solicitud existente -> retorna eventos ordenados")
        void getHistorial_solicitudExistent_returnsEventos() {
            EventoSolicitud evento = EventoSolicitud.builder()
                    .id(1L)
                    .solicitud(solicitud)
                    .usuario(analistaUser)
                    .estadoOrigen(null)
                    .estadoDestino(Estado.BORRADOR)
                    .comentario("Solicitud creada")
                    .build();
            given(solicitudRepository.existsById(1L)).willReturn(true);
            given(eventoSolicitudRepository.findBySolicitudIdOrderByFechaAsc(1L))
                    .willReturn(List.of(evento));

            var historial = solicitudService.getHistorial(1L);

            assertThat(historial).hasSize(1);
            assertThat(historial.get(0).getEstadoDestino()).isEqualTo(Estado.BORRADOR);
        }

        @Test
        @DisplayName("Historial de solicitud inexistente -> 404")
        void getHistorial_solicitudInexistente_throwsNotFound() {
            given(solicitudRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> solicitudService.getHistorial(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrada");
        }
    }

    @Nested
    @DisplayName("findAll - busqueda con filtros")
    class FindAllTest {

        @Test
        @DisplayName("findAll retorna pagina de solicitudes")
        void findAll_returnsPageOfSolicitudes() {
            given(solicitudRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(solicitud)));

            Page<SolicitudResponseDTO> page = solicitudService.findAll(
                    null, null, null, null, null, 0, 10);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).estado()).isEqualTo(Estado.BORRADOR);
        }
    }
}
