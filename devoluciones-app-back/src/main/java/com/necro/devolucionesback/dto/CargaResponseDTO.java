package com.necro.devolucionesback.dto;

import com.necro.devolucionesback.model.Carga;
import com.necro.devolucionesback.model.CargaEstado;

import java.time.LocalDateTime;

public record CargaResponseDTO(
        Long id,
        String nombreArchivo,
        int totalFilas,
        int filasOk,
        int filasRechazadas,
        CargaEstado estado,
        LocalDateTime fechaCreacion
) {
    public static CargaResponseDTO fromEntity(Carga carga) {
        return new CargaResponseDTO(
                carga.getId(),
                carga.getNombreArchivo(),
                carga.getTotalFilas(),
                carga.getFilasOk(),
                carga.getFilasRechazadas(),
                carga.getEstado(),
                carga.getFechaCreacion()
        );
    }
}
