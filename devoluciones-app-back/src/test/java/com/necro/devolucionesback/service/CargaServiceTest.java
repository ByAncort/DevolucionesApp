package com.necro.devolucionesback.service;

import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class CargaServiceTest {

    @Mock
    private CargaRepository cargaRepository;
    @Mock
    private CargaErrorRepository cargaErrorRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private BancoRepository bancoRepository;
    @Mock
    private EventoSolicitudRepository eventoSolicitudRepository;
    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private CargaService cargaService;

    private User analistaUser;
    private Banco bancoChile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cargaService, "maxFilas", 50000);

        analistaUser = User.builder()
                .id(1L)
                .username("analista1")
                .email("analista@test.com")
                .roles(Set.of(Role.builder().id(1L).name("ANALISTA").build()))
                .build();

        bancoChile = Banco.builder().id(1L).nombreBanco("BANCO CHILE").build();
    }

    @Nested
    @DisplayName("procesarCSV - carga masiva")
    class ProcesarCSVTest {

        @Test
        @DisplayName("CSV con filas validas -> Carga COMPLETADA")
        void procesarCSV_validRows_completada() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000.00;BANCO CHILE;001234567890;REF-001\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);
            given(bancoRepository.findByNombreBancoIgnoreCase("BANCO CHILE"))
                    .willReturn(Optional.of(bancoChile));
            given(solicitudRepository.nextFolio()).willReturn(1L);
            given(solicitudRepository.existsByReferenciaBanco("REF-001")).willReturn(false);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            assertThat(result).isNotNull();
            then(solicitudRepository).should().saveAll(anyList());
            then(eventoSolicitudRepository).should().saveAll(anyList());
        }

        @Test
        @DisplayName("CSV con referencia duplicada en archivo -> error duplicada")
        void procesarCSV_duplicateReferenceInFile_error() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000.00;BANCO CHILE;001234567890;REF-001\n"
                    + "87654321-0;JUAN GARCIA;200000.00;BANCO CHILE;001234567891;REF-001\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .anyMatch(e -> e.getMotivo().contains("duplicada"))));
        }

        @Test
        @DisplayName("CSV con referencia existente en BD -> error existente")
        void procesarCSV_existingReferenceInDB_error() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000.00;BANCO CHILE;001234567890;REF-EXIST\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);
            given(solicitudRepository.existsByReferenciaBanco("REF-EXIST")).willReturn(true);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .anyMatch(e -> e.getMotivo().contains("existente en el sistema"))));
        }

        @Test
        @DisplayName("CSV con banco inexistente -> error banco_destino")
        void procesarCSV_unknownBank_error() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000.00;BANCO INEXISTENTE;001234567890;REF-001\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);
            given(bancoRepository.findByNombreBancoIgnoreCase("BANCO INEXISTENTE"))
                    .willReturn(Optional.empty());

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .anyMatch(e -> e.getCampo().equals("banco_destino"))));
        }

        @Test
        @DisplayName("CSV vacio (solo header) -> CON_ERRORES con 0 filas")
        void procesarCSV_emptyCSV_conErrores() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaRepository).should().save(argThat(c ->
                    c.getTotalFilas() == 0 && c.getEstado() == CargaEstado.CON_ERRORES));
        }

        @Test
        @DisplayName("CSV con RUT invalido -> error en fila")
        void procesarCSV_invalidRut_error() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "RUT-INVALIDO;MARIA PEREZ;150000.00;BANCO CHILE;001234567890;REF-001\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .anyMatch(e -> e.getCampo().equals("rut_cliente"))));
        }

        @Test
        @DisplayName("CSV excede maxFilas -> filas adicionales ignoradas con error")
        void procesarCSV_exceedsMaxFilas_additionalRowsIgnored() throws IOException {
            ReflectionTestUtils.setField(cargaService, "maxFilas", 2);

            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000;BANCO CHILE;001234567890;REF-001\n"
                    + "87654321-4;JUAN GARCIA;200000;BANCO CHILE;001234567891;REF-002\n"
                    + "11111111-1;PEDRO LOPEZ;50000;BANCO CHILE;001234567892;REF-003\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);
            given(bancoRepository.findByNombreBancoIgnoreCase("BANCO CHILE"))
                    .willReturn(Optional.of(bancoChile));
            given(solicitudRepository.nextFolio()).willReturn(1L, 2L);
            given(solicitudRepository.existsByReferenciaBanco(anyString())).willReturn(false);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .anyMatch(e -> e.getMotivo().contains("limite maximo"))));
        }

        @Test
        @DisplayName("Referencias duplicadas dentro del mismo archivo se detectan")
        void procesarCSV_duplicateReferenceInFileDetected() throws IOException {
            String csv = "rut_cliente;nombre_cliente;monto;banco_destino;cuenta_destino;referencia_banco\n"
                    + "12345678-5;MARIA PEREZ;150000.00;BANCO CHILE;001234567890;REF-DUP\n"
                    + "87654321-0;JUAN GARCIA;200000.00;BANCO CHILE;001234567891;REF-DUP\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "pagos.csv", "text/csv",
                    csv.getBytes(StandardCharsets.UTF_8));

            Carga carga = Carga.builder().id(1L).nombreArchivo("pagos.csv").build();
            given(cargaRepository.save(any(Carga.class))).willReturn(carga);

            Carga result = cargaService.procesarCSV(file, analistaUser);

            then(cargaErrorRepository).should().saveAll(argThat(errors ->
                    java.util.stream.StreamSupport.stream(errors.spliterator(), false)
                            .filter(e -> e.getMotivo().contains("duplicada")).count() == 1));
        }
    }
}
