package com.necro.devolucionesback.dto;

public record CargaErrorDTO(
        int numFila,
        String campo,
        String motivo
) {}
