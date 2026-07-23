package com.necro.devolucionesback.controller;

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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/solicitud")
public class SolicitudController {
    private final SolicitudService solicitudService;

    @PostMapping()
    public SolicitudResponseDTO create(@Valid @RequestBody SolicitudRequestDTO requestDTO){
        return solicitudService.CrearSolicitud(requestDTO);
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
}
