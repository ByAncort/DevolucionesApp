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
        registerEvento(solicitud, currentUser,"Solicitud creada en el sistema");
        return SolicitudResponseDTO.fromEntity(solicitud);
    }
    
    public EventoSolicitud registerEvento(Solicitud solicitud,User currentUser,String comentario){
        EventoSolicitud eventoInicial = EventoSolicitud.builder()
                .solicitud(solicitud)
                .usuario(currentUser)
                 // Es el evento de creación inicial
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
        registerEvento(solicitud, currentUser,"Solicitud actualizada en el sistema");
        return SolicitudResponseDTO.fromEntity(solicitud);
    }

    public SolicitudResponseDTO cambiarEstadoSolicitud(Long id, String accion) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada con id: " + id));
        // anular factura
        switch (accion.toLowerCase()) {
            case "enviar":
                if (solicitud.getEstado().equals(Estado.BORRADOR)) {
                    solicitud.setEstado(Estado.EN_REVISION);
                } else {
                    throw new InvalidStateTransitionException("Solicitud estado invalido "+solicitud.getEstado());
                }
                break;
            case "aprobar":
                if (solicitud.getEstado().equals(Estado.EN_REVISION)) {
                    solicitud.setEstado(Estado.APROBADA);
                } else {
                    throw new InvalidStateTransitionException("Solicitud estado invalido "+solicitud.getEstado());
                }
                break;
            case "rechazar":
                if (solicitud.getEstado().equals(Estado.EN_REVISION)) {
                    solicitud.setEstado(Estado.RECHAZADA);
                } else {
                    throw new InvalidStateTransitionException("Solicitud estado invalido "+solicitud.getEstado());
                }
                break;

        }

        return null;
    }

}

