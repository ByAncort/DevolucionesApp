package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.BancoRepository;
import com.necro.devolucionesback.repository.EventoSolicitudRepository;
import com.necro.devolucionesback.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        EventoSolicitud eventoSolicitud = registerEvento(solicitud);

        solicitud = solicitudRepository.save(solicitud);

        return SolicitudResponseDTO.fromEntity(solicitud);
    }
    public EventoSolicitud registerEvento(Solicitud solicitud){
        EventoSolicitud eventoInicial = EventoSolicitud.builder()
                .solicitud(solicitud)
                .estadoOrigen(null) // Es el evento de creación inicial
                .estadoDestino(Estado.BORRADOR)
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
}

