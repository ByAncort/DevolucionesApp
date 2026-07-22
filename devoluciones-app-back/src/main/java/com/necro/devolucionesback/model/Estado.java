package com.necro.devolucionesback.model;

public enum Estado {
    BORRADOR("Borrador"),
    EN_REVISION ("En Revision"),
    APROBADA ("Aprobada"),
    RECHAZADA ("Rechazada"),
    PAGADA("Pagada Total"),
    ANULADA("Anulada");

    private final String label;

    Estado(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
