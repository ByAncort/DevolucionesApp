package com.necro.devolucionesback.service;

public class SolicitudValidator {
    public static void validarMontoRut(String rut, Double monto) {
        if (!validarRutChileno(rut)) {
            throw new IllegalArgumentException("El RUT ingresado no es un RUT chileno válido.");
        }

        if (monto == null || monto <= 0 || monto > 10000000) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0 y no puede superar los 10.000.000 CLP.");
        }
    }

    private static boolean validarRutChileno(String rut) {
        if (rut == null) return false;
        String limpio = rut.replace(".", "").replace("-", "").trim();
        if (limpio.length() < 2) return false;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dvInput = Character.toUpperCase(limpio.charAt(limpio.length() - 1));
        try {
            int rutNum = Integer.parseInt(cuerpo);
            int m = 0, s = 1;
            for (; rutNum != 0; rutNum /= 10) {
                s = (s + rutNum % 10 * (9 - m++ % 6)) % 11;
            }
            char dvEsperado = (char) (s != 0 ? s + 47 : 75); // 75 es 'K' en ASCII
            return dvInput == dvEsperado;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
