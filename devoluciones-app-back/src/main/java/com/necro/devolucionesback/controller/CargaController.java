package com.necro.devolucionesback.controller;

import com.necro.devolucionesback.dto.CargaDetalleResponseDTO;
import com.necro.devolucionesback.dto.CargaResponseDTO;
import com.necro.devolucionesback.model.Carga;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.CargaErrorRepository;
import com.necro.devolucionesback.repository.CargaRepository;
import com.necro.devolucionesback.service.CargaService;
import com.necro.devolucionesback.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cargas")
public class CargaController {

    private final CargaService cargaService;
    private final CargaRepository cargaRepository;
    private final CargaErrorRepository cargaErrorRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @PreAuthorize("hasAuthority('ANALISTA')")
    @PostMapping
    public ResponseEntity<CargaResponseDTO> upload(@RequestParam("file") MultipartFile file) {
        User currentUser = customUserDetailsService.getCurrentUser();
        Carga carga = cargaService.procesarCSV(file, currentUser);
        return ResponseEntity.created(URI.create("/api/v1/cargas/" + carga.getId()))
                .body(CargaResponseDTO.fromEntity(carga));
    }

    @GetMapping("/{id}")
    public CargaDetalleResponseDTO getDetalle(@PathVariable Long id) {
        Carga carga = cargaRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Carga no encontrada con id: " + id));
        var errores = cargaErrorRepository.findByCargaIdOrderByNumFilaAsc(id);
        return CargaDetalleResponseDTO.fromEntity(carga, errores);
    }
}
