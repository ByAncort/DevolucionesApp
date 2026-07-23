package com.necro.devolucionesback.controller;

import com.necro.devolucionesback.dto.EventoResponseDTO;
import com.necro.devolucionesback.dto.RechazoRequest;
import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.model.Estado;
import com.necro.devolucionesback.model.Origen;
import com.necro.devolucionesback.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {
    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudResponseDTO> create(@Valid @RequestBody SolicitudRequestDTO requestDTO) {
        SolicitudResponseDTO created = solicitudService.CrearSolicitud(requestDTO);
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + created.id()))
                .body(created);
    }

    @GetMapping
    public Page<SolicitudResponseDTO> findAll(
            @RequestParam(required = false) Estado estado,
            @RequestParam(required = false) String rut,
            @RequestParam(required = false) Origen origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return solicitudService.findAll(estado, rut, origen, fechaInicio, fechaFin, page, size);
    }

    @GetMapping("/{id}")
    public SolicitudResponseDTO findById(@PathVariable Long id) {
        return solicitudService.findById(id);
    }

    @GetMapping("/{id}/historial")
    public List<EventoResponseDTO> getHistorial(@PathVariable Long id) {
        return solicitudService.getHistorial(id);
    }

    @PutMapping("/{id}")
    public SolicitudResponseDTO updateSolicitud(@PathVariable Long id, @Valid @RequestBody SolicitudRequestDTO requestDTO) {
        return solicitudService.updateSolicitud(id, requestDTO);
    }

    @PreAuthorize("hasAuthority('ANALISTA')")
    @PostMapping("/{id}/enviar")
    public SolicitudResponseDTO enviar(@PathVariable Long id) {
        return solicitudService.cambiarEstadoSolicitud(id, "enviar", null);
    }

    @PreAuthorize("hasAuthority('SUPERVISOR')")
    @PostMapping("/{id}/aprobar")
    public SolicitudResponseDTO aprobar(@PathVariable Long id) {
        return solicitudService.cambiarEstadoSolicitud(id, "aprobar", null);
    }

    @PreAuthorize("hasAuthority('SUPERVISOR')")
    @PostMapping("/{id}/rechazar")
    public SolicitudResponseDTO rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest request) {
        return solicitudService.cambiarEstadoSolicitud(id, "rechazar", request.getMotivoRechazo());
    }

    @PreAuthorize("hasAuthority('SUPERVISOR')")
    @PostMapping("/{id}/pagar")
    public SolicitudResponseDTO pagar(@PathVariable Long id) {
        return solicitudService.cambiarEstadoSolicitud(id, "pagar", null);
    }

    @PreAuthorize("hasAuthority('ANALISTA')")
    @PostMapping("/{id}/anular")
    public SolicitudResponseDTO anular(@PathVariable Long id) {
        return solicitudService.cambiarEstadoSolicitud(id, "anular", null);
    }

    @PreAuthorize("hasAuthority('ANALISTA')")
    @PostMapping("/{id}/reabrir")
    public SolicitudResponseDTO reabrir(@PathVariable Long id) {
        return solicitudService.cambiarEstadoSolicitud(id, "reabrir", null);
    }
}
