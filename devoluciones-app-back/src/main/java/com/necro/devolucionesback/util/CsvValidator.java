package com.necro.devolucionesback.util;

import com.necro.devolucionesback.service.SolicitudValidator;

import java.util.ArrayList;
import java.util.List;

public class CsvValidator {

    private static final String[] HEADERS_ESPERADOS = {
            "rut_cliente", "nombre_cliente", "monto", "banco_destino", "cuenta_destino", "referencia_banco"
    };

    public static List<CsvValidationError> validarFila(String[] campos, int numFila) {
        List<CsvValidationError> errores = new ArrayList<>();

        if (campos == null || campos.length < 6) {
            errores.add(new CsvError("all", "Fila incompleta: se esperan 6 columnas, se recibieron " + (campos == null ? 0 : campos.length)));
            return errores;
        }

        String rut = safeTrim(campos[0]);
        String nombre = safeTrim(campos[1]);
        String montoStr = safeTrim(campos[2]);
        String banco = safeTrim(campos[3]);
        String cuenta = safeTrim(campos[4]);
        String referencia = safeTrim(campos[5]);

        if (rut.isEmpty()) {
            errores.add(new CsvError("rut_cliente", "El RUT es obligatorio"));
        } else if (!SolicitudValidator.validarRutChilenoPublico(rut)) {
            errores.add(new CsvError("rut_cliente", "RUT inválido: " + rut));
        }

        if (nombre.isEmpty()) {
            errores.add(new CsvError("nombre_cliente", "El nombre del cliente es obligatorio"));
        }

        if (montoStr.isEmpty()) {
            errores.add(new CsvError("monto", "El monto es obligatorio"));
        } else {
            try {
                Double monto = Double.parseDouble(montoStr.replace(",", "."));
                if (monto <= 0 || monto > 10_000_000) {
                    errores.add(new CsvError("monto", "El monto debe ser mayor a 0 y no superar 10.000.000 CLP"));
                }
            } catch (NumberFormatException e) {
                errores.add(new CsvError("monto", "El monto no es un número válido: " + montoStr));
            }
        }

        if (banco.isEmpty()) {
            errores.add(new CsvError("banco_destino", "El banco de destino es obligatorio"));
        }

        if (cuenta.isEmpty()) {
            errores.add(new CsvError("cuenta_destino", "La cuenta de destino es obligatoria"));
        }

        if (referencia.isEmpty()) {
            errores.add(new CsvError("referencia_banco", "La referencia del banco es obligatoria"));
        }

        return errores;
    }

    private static String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    public record CsvError(String campo, String motivo) implements CsvValidationError {}

    public interface CsvValidationError {
        String campo();
        String motivo();
    }
}
