package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.BancoRepository;
import com.necro.devolucionesback.repository.EventoSolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        registerEvento(solicitud, currentUser);
        return SolicitudResponseDTO.fromEntity(solicitud);
    }
    
    public EventoSolicitud registerEvento(Solicitud solicitud,User currentUser){
        EventoSolicitud eventoInicial = EventoSolicitud.builder()
                .solicitud(solicitud)
                .usuario(currentUser)
                 // Es el evento de creación inicial
                .estadoDestino(solicitud.getEstado())
                .comentario("Solicitud creada en el sistema")
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

}

