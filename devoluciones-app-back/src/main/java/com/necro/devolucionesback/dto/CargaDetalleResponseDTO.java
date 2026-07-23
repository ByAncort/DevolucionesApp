package com.necro.devolucionesback.dto;

import com.necro.devolucionesback.model.Carga;
import com.necro.devolucionesback.model.CargaError;

import java.util.List;

public record CargaDetalleResponseDTO(
        CargaResponseDTO carga,
        List<CargaErrorDTO> errores
) {
    public static CargaDetalleResponseDTO fromEntity(Carga carga, List<CargaError> errores) {
        return new CargaDetalleResponseDTO(
                CargaResponseDTO.fromEntity(carga),
                errores.stream()
                        .map(e -> new CargaErrorDTO(e.getNumFila(), e.getCampo(), e.getMotivo()))
                        .toList()
        );
    }
}
