package com.necro.devolucionesback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitudValidatorTest {

    @Nested
    @DisplayName("R5 - validarMontoRut")
    class ValidarMontoRutTest {

        @Test
        @DisplayName("RUT valido y monto dentro de rango -> no lanza excepcion")
        void validarMontoRut_validRutAndAmount_throwsNothing() {
            assertThatCode(() -> SolicitudValidator.validarMontoRut("12345678-5", 150000.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("RUT con K valido")
        void validarMontoRut_validRutWithK_throwsNothing() {
            assertThatCode(() -> SolicitudValidator.validarMontoRut("11111111-1", 1000.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("RUT nulo -> lanza IllegalArgumentException")
        void validarMontoRut_nullRut_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut(null, 1000.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RUT");
        }

        @Test
        @DisplayName("RUT vacio -> lanza IllegalArgumentException")
        void validarMontoRut_emptyRut_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("", 1000.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RUT");
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345678-0", "12345678-9", "abc-def"})
        @DisplayName("RUT invalido -> lanza IllegalArgumentException")
        void validarMontoRut_invalidRut_throwsException(String rutInvalido) {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut(rutInvalido, 1000.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RUT");
        }

        @Test
        @DisplayName("Monto nulo -> lanza IllegalArgumentException")
        void validarMontoRut_nullAmount_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("12345678-5", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Monto <= 0 -> lanza IllegalArgumentException")
        void validarMontoRut_zeroAmount_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("12345678-5", 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Monto negativo -> lanza IllegalArgumentException")
        void validarMontoRut_negativeAmount_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("12345678-5", -100.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Monto supera 10.000.000 -> lanza IllegalArgumentException")
        void validarMontoRut_amountExceedsMax_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("12345678-5", 10000001.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Monto exactamente 10.000.000 -> no lanza excepcion")
        void validarMontoRut_exactMaxAmount_throwsNothing() {
            assertThatCode(() -> SolicitudValidator.validarMontoRut("12345678-5", 10000000.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Monto justo encima del limite -> lanza IllegalArgumentException")
        void validarMontoRut_justAboveMax_throwsException() {
            assertThatThrownBy(() -> SolicitudValidator.validarMontoRut("12345678-5", 10000000.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto");
        }
    }

    @Nested
    @DisplayName("R5 - validarRutChilenoPublico")
    class ValidarRutChilenoPublicoTest {

        @Test
        @DisplayName("RUT valido retorna true")
        void validarRutChilenoPublico_validRut_returnsTrue() {
            assertThat(SolicitudValidator.validarRutChilenoPublico("12345678-5")).isTrue();
        }

        @Test
        @DisplayName("RUT invalido retorna false")
        void validarRutChilenoPublico_invalidRut_returnsFalse() {
            assertThat(SolicitudValidator.validarRutChilenoPublico("12345678-0")).isFalse();
        }

        @Test
        @DisplayName("RUT nulo retorna false")
        void validarRutChilenoPublico_nullRut_returnsFalse() {
            assertThat(SolicitudValidator.validarRutChilenoPublico(null)).isFalse();
        }

        @Test
        @DisplayName("RUT con formato incorrecto retorna false")
        void validarRutChilenoPublico_wrongFormat_returnsFalse() {
            assertThat(SolicitudValidator.validarRutChilenoPublico("abc")).isFalse();
        }

        @Test
        @DisplayName("RUT muy corto retorna false")
        void validarRutChilenoPublico_tooShort_returnsFalse() {
            assertThat(SolicitudValidator.validarRutChilenoPublico("1-K")).isFalse();
        }
    }
}
