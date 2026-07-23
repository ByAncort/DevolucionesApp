package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.exception.InvalidStateTransitionException;
import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.BancoRepository;
import com.necro.devolucionesback.repository.EventoSolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class SolicitudService {
    private final CustomUserDetailsService customUserDetailsService;
    private final SolicitudRepository solicitudRepository;
    private final BancoRepository bancoRepository;
    private final EventoSolicitudRepository eventoSolicitudRepository;

    @Transactional
    public SolicitudResponseDTO CrearSolicitud(SolicitudRequestDTO request) {
        //R5
        SolicitudValidator.validarMontoRut(request.rutCliente(),request.monto());

        Banco banco = bancoRepository.findById(request.bancoDestinoId())
                .orElseThrow(()-> new IllegalArgumentException("El banco seleccionado no existe"));
        User currentUser = customUserDetailsService.getCurrentUser();
        Solicitud solicitud = Solicitud.builder()
                .folio(this.generarFolio())
                .rutCliente(request.rutCliente())
                .nombreCliente(request.nombreCliente())
                .monto(request.monto())
                //.moneda() agregar moneda transacciones sin monedas no es posible
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .bancoDestino(banco)
                .cuentaDestino(request.cuentaDestino())
//                .origen() por el momento metodo creacion manual todas manual
                .estado(Estado.BORRADOR)
                .build();

        solicitud = solicitudRepository.save(solicitud);
        registerEvento(solicitud, currentUser, null, "Solicitud creada en el sistema");
        return SolicitudResponseDTO.fromEntity(solicitud);
    }
    
    public EventoSolicitud registerEvento(Solicitud solicitud, User currentUser, Estado estadoOrigen, String comentario) {
        EventoSolicitud eventoInicial = EventoSolicitud.builder()
                .solicitud(solicitud)
                .usuario(currentUser)
                .estadoOrigen(estadoOrigen)
                .estadoDestino(solicitud.getEstado())
                .comentario(comentario)
                .build();
        eventoSolicitudRepository.save(eventoInicial);
        return eventoInicial;
    }

    public synchronized String generarFolio() {
        int anioActual = LocalDate.now().getYear();
        long correlativo = solicitudRepository.countByAnio(anioActual) + 1;
        return String.format("DEV-%d-%06d", anioActual, correlativo);
    }

    public Page<SolicitudResponseDTO> findAll(
            Estado estado,
            String rut,
            Origen origen,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int page,
            int size) {

        Specification<Solicitud> spec = SolicitudSpec.conFiltros(estado, rut, origen, fechaInicio, fechaFin);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return solicitudRepository.findAll(spec, pageable)
                .map(SolicitudResponseDTO::fromEntity);
    }

    public SolicitudResponseDTO findById(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada con id: " + id));
        return SolicitudResponseDTO.fromEntity(solicitud);
    }

    public SolicitudResponseDTO updateSolicitud(Long id, @Valid SolicitudRequestDTO requestDTO) {

        Solicitud solicitud=solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada con id: " + id));

        Banco banco = bancoRepository.findById(requestDTO.bancoDestinoId())
                .orElseThrow(()-> new IllegalArgumentException("El banco seleccionado no existe"));
        User currentUser = customUserDetailsService.getCurrentUser();

        // actualizar valores base
        solicitud.setRutCliente(requestDTO.rutCliente());
        solicitud.setNombreCliente(requestDTO.nombreCliente());
        solicitud.setMonto(requestDTO.monto());
        solicitud.setBancoDestino(banco);
        solicitud.setCuentaDestino(requestDTO.cuentaDestino());

        solicitudRepository.save(solicitud);
        registerEvento(solicitud, currentUser, solicitud.getEstado(), "Solicitud actualizada en el sistema");
        return SolicitudResponseDTO.fromEntity(solicitud);
    }

    @Transactional
    public SolicitudResponseDTO cambiarEstadoSolicitud(Long id, String accion, String motivoRechazo) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada con id: " + id));

        User currentUser = customUserDetailsService.getCurrentUser();
        Estado estadoActual = solicitud.getEstado();
        String comentario;

        switch (accion.toLowerCase()) {
            case "enviar":
                // R1: BORRADOR → EN_REVISION
                // R2: basta ANALISTA
                requireEstado(solicitud, Estado.BORRADOR, "enviar");
                comentario = "Solicitud enviada a revisión";
                solicitud.setEstado(Estado.EN_REVISION);
                break;

            case "aprobar":
                // R1: EN_REVISION → APROBADA
                // R2: requiere SUPERVISOR
                // R7: supervisor no puede ser el creador
                requireEstado(solicitud, Estado.EN_REVISION, "aprobar");
                requireSupervisor(currentUser);
                requireNoCreador(solicitud, currentUser);
                comentario = "Solicitud aprobada por supervisor";
                solicitud.setEstado(Estado.APROBADA);
                break;

            case "rechazar":
                // R1: EN_REVISION → RECHAZADA
                // R2: requiere SUPERVISOR
                // R3: motivo_rechazo obligatorio
                requireEstado(solicitud, Estado.EN_REVISION, "rechazar");
                requireSupervisor(currentUser);
                requireMotivoRechazo(motivoRechazo);
                solicitud.setMotivoRechazo(motivoRechazo);
                comentario = "Solicitud rechazada: " + motivoRechazo;
                solicitud.setEstado(Estado.RECHAZADA);
                break;

            case "pagar":
                // R1: APROBADA → PAGADA
                // R2: requiere SUPERVISOR
                requireEstado(solicitud, Estado.APROBADA, "pagar");
                requireSupervisor(currentUser);
                comentario = "Solicitud pagada";
                solicitud.setEstado(Estado.PAGADA);
                break;

            case "anular":
                // R1: BORRADOR → ANULADA
                // R2: basta ANALISTA
                requireEstado(solicitud, Estado.BORRADOR, "anular");
                comentario = "Solicitud anulada";
                solicitud.setEstado(Estado.ANULADA);
                break;

            case "reabrir":
                // R1: RECHAZADA → BORRADOR
                // R2: basta ANALISTA
                // R4: una sola vez (vecesReabierta)
                requireEstado(solicitud, Estado.RECHAZADA, "reabrir");
                requireReabrirDisponible(solicitud);
                solicitud.setVecesReabierta(solicitud.getVecesReabierta() + 1);
                solicitud.setMotivoRechazo(null);
                comentario = "Solicitud reabierta (intento " + solicitud.getVecesReabierta() + ")";
                solicitud.setEstado(Estado.BORRADOR);
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Accion no valida: " + accion + ". Acciones disponibles: enviar, aprobar, rechazar, pagar, anular, reabrir");
        }

        solicitud.setUpdatedBy(currentUser);
        solicitudRepository.save(solicitud);
        // R6: registrar evento en la misma transaccion
        registerEvento(solicitud, currentUser, estadoActual, comentario);

        return SolicitudResponseDTO.fromEntity(solicitud);
    }

    private void requireEstado(Solicitud solicitud, Estado esperado, String accion) {
        if (!solicitud.getEstado().equals(esperado)) {
            throw new InvalidStateTransitionException(
                    "No se puede " + accion + ": estado actual es " + solicitud.getEstado().getLabel()
                            + ", se requiere " + esperado.getLabel());
        }
    }

    private void requireSupervisor(User user) {
        boolean esSupervisor = user.getRoles().stream()
                .anyMatch(role -> "SUPERVISOR".equals(role.getName()));
        if (!esSupervisor) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Se requiere rol SUPERVISOR para esta accion");
        }
    }

    private void requireNoCreador(Solicitud solicitud, User currentUser) {
        if (solicitud.getCreatedBy() != null
                && solicitud.getCreatedBy().getId() == currentUser.getId()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El supervisor que aprueba no puede ser el mismo usuario que creo la solicitud");
        }
    }

    private void requireMotivoRechazo(String motivoRechazo) {
        if (motivoRechazo == null || motivoRechazo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo de rechazo es obligatorio (R3)");
        }
    }

    private void requireReabrirDisponible(Solicitud solicitud) {
        if (solicitud.getVecesReabierta() >= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La solicitud ya fue reabierta. No se puede reabrir mas de una vez (R4)");
        }
    }

}

