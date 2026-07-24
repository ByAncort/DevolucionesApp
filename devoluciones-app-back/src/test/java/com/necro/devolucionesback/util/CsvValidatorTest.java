package com.necro.devolucionesback.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvValidatorTest {

    @Nested
    @DisplayName("Validacion de columnas")
    class ColumnValidationTest {

        @Test
        @DisplayName("Fila null -> error all")
        void validarFila_nullCampos_returnsErrorAll() {
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(null, 1);
            assertThat(errores).hasSize(1);
            assertThat(errores.get(0).campo()).isEqualTo("all");
            assertThat(errores.get(0).motivo()).contains("Fila incompleta");
        }

        @Test
        @DisplayName("Fila con menos de 6 columnas -> error all")
        void validarFila_tooFewColumns_returnsErrorAll() {
            String[] campos = {"12345678-5", "NOMBRE", "1000"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).hasSize(1);
            assertThat(errores.get(0).campo()).isEqualTo("all");
        }

        @Test
        @DisplayName("Fila vacia (6 campos vacios) -> todos los campos con error")
        void validarFila_allEmpty_returnsErrorsForAllFields() {
            String[] campos = {"", "", "", "", "", ""};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).hasSize(6);
        }
    }

    @Nested
    @DisplayName("Validacion de RUT (R5)")
    class RutValidationTest {

        @Test
        @DisplayName("RUT vacio -> error rut_cliente")
        void validarFila_emptyRut_returnsError() {
            String[] campos = {"", "NOMBRE", "1000", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("rut_cliente"));
        }

        @Test
        @DisplayName("RUT invalido -> error rut_cliente")
        void validarFila_invalidRut_returnsError() {
            String[] campos = {"12345678-0", "NOMBRE", "1000", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("rut_cliente"));
        }

        @Test
        @DisplayName("RUT valido -> sin error de RUT")
        void validarFila_validRut_noRutError() {
            String[] campos = {"12345678-5", "NOMBRE", "1000", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).noneMatch(e -> e.campo().equals("rut_cliente"));
        }
    }

    @Nested
    @DisplayName("Validacion de nombre")
    class NombreValidationTest {

        @Test
        @DisplayName("Nombre vacio -> error nombre_cliente")
        void validarFila_emptyName_returnsError() {
            String[] campos = {"12345678-5", "", "1000", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("nombre_cliente"));
        }

        @Test
        @DisplayName("Nombre presente -> sin error de nombre")
        void validarFila_validName_noNameError() {
            String[] campos = {"12345678-5", "MARIA PEREZ", "1000", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).noneMatch(e -> e.campo().equals("nombre_cliente"));
        }
    }

    @Nested
    @DisplayName("Validacion de monto (R5)")
    class MontoValidationTest {

        @Test
        @DisplayName("Monto vacio -> error monto")
        void validarFila_emptyAmount_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("monto"));
        }

        @Test
        @DisplayName("Monto no numerico -> error monto")
        void validarFila_nonNumericAmount_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "abc", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("monto"));
        }

        @Test
        @DisplayName("Monto <= 0 -> error monto")
        void validarFila_zeroAmount_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "0", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("monto"));
        }

        @Test
        @DisplayName("Monto > 10.000.000 -> error monto")
        void validarFila_amountExceedsMax_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "10000001", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("monto"));
        }

        @Test
        @DisplayName("Monto valido con coma decimal -> sin error de monto")
        void validarFila_validAmountWithComma_noAmountError() {
            String[] campos = {"12345678-5", "NOMBRE", "150.000,50", "BANCO", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).noneMatch(e -> e.campo().equals("monto"));
        }
    }

    @Nested
    @DisplayName("Validacion de banco y cuenta")
    class BancoCuentaValidationTest {

        @Test
        @DisplayName("Banco vacio -> error banco_destino")
        void validarFila_emptyBank_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "1000", "", "CUENTA", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("banco_destino"));
        }

        @Test
        @DisplayName("Cuenta vacia -> error cuenta_destino")
        void validarFila_emptyAccount_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "1000", "BANCO", "", "REF-001"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("cuenta_destino"));
        }
    }

    @Nested
    @DisplayName("Validacion de referencia")
    class ReferenciaValidationTest {

        @Test
        @DisplayName("Referencia vacia -> error referencia_banco")
        void validarFila_emptyReference_returnsError() {
            String[] campos = {"12345678-5", "NOMBRE", "1000", "BANCO", "CUENTA", ""};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).anyMatch(e -> e.campo().equals("referencia_banco"));
        }
    }

    @Nested
    @DisplayName("Fila completa valida")
    class ValidRowTest {

        @Test
        @DisplayName("Fila completa y valida -> sin errores")
        void validarFila_validRow_noErrors() {
            String[] campos = {"12345678-5", "MARIA PEREZ SOTO", "150000.00", "BANCO CHILE", "001234567890", "REF-2026-000123"};
            List<CsvValidator.CsvValidationError> errores = CsvValidator.validarFila(campos, 1);
            assertThat(errores).isEmpty();
        }
    }
}
