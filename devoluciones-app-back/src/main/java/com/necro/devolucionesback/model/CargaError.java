package com.necro.devolucionesback.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "carga_errores")
public class CargaError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id", nullable = false, updatable = false)
    private Carga carga;

    @Column(name = "num_fila", nullable = false, updatable = false)
    private int numFila;

    @Column(name = "campo", nullable = false, updatable = false)
    private String campo;

    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;
}
