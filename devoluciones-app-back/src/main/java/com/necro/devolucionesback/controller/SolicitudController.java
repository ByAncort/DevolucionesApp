package com.necro.devolucionesback.controller;

import com.necro.devolucionesback.dto.SolicitudRequestDTO;
import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/solicitud")
public class SolicitudController {
    private final SolicitudService solicitudService;

    @PostMapping()
    public SolicitudResponseDTO create(SolicitudRequestDTO requestDTO){
        return solicitudService.CrearSolicitud(requestDTO);
    }
}
