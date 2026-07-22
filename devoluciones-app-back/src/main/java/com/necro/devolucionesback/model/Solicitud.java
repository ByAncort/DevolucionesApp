package com.necro.devolucionesback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true)
    private String folio;

    @Column(name = "rut_cliente", nullable = false)
    private String rutCliente;

    @Column(name = "nombre_cliente")
    private String nombreCliente;

    private Double monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banco_destino_id")
    private Banco bancoDestino;

    @Column(name="cuenta_destino", nullable = false)
    private String cuentaDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    // campos de auditoria testear que se generen siempre

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por", updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizada_por", updatable = false)
    private User updatedBy;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_actualizacion", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = Estado.BORRADOR;
        }
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
