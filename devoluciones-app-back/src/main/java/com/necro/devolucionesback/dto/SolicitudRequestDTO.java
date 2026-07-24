package com.necro.devolucionesback.dto;

import jakarta.validation.constraints.*;

public record SolicitudRequestDTO(

        @NotBlank(message = "El RUT del cliente es obligatorio")
        @Pattern(regexp = "^\\d{1,8}-[\\dkK]$", message = "Formato de RUT inválido. Ejemplo: 12345678-K")
        String rutCliente,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombreCliente,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        Double monto,

        @NotNull(message = "Debes seleccionar un banco de destino")
        Long bancoDestinoId,

        @NotBlank(message = "La cuenta de destino es obligatoria")
        @Size(max = 30, message = "La cuenta de destino es demasiado larga")
        String cuentaDestino,

        String referenciaBanco
) {}