package com.necro.devolucionesback.model;

public enum CargaEstado {
    PROCESANDO("Procesando"),
    COMPLETADA("Completada"),
    CON_ERRORES("Completada con errores");

    private final String label;

    CargaEstado(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
