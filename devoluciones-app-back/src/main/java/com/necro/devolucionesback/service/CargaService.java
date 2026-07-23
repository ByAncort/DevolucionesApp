package com.necro.devolucionesback.service;

import com.necro.devolucionesback.model.*;
import com.necro.devolucionesback.repository.*;
import com.necro.devolucionesback.util.CsvValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CargaService {

    private final CargaRepository cargaRepository;
    private final CargaErrorRepository cargaErrorRepository;
    private final SolicitudRepository solicitudRepository;
    private final BancoRepository bancoRepository;
    private final EventoSolicitudRepository eventoSolicitudRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${carga.max-filas:50000}")
    private int maxFilas;

    private static final int BATCH_SIZE = 500;

    public Carga procesarCSV(MultipartFile file, User usuario) {
        Carga carga = Carga.builder()
                .nombreArchivo(file.getOriginalFilename())
                .usuario(usuario)
                .estado(CargaEstado.PROCESANDO)
                .build();
        carga = cargaRepository.save(carga);

        List<CargaError> errores = new ArrayList<>();
        List<Solicitud> solicitudesBatch = new ArrayList<>();
        int numFila = 0;
        int filasOk = 0;
        int filasRechazadas = 0;

        Set<String> referenciasVistas = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                carga.setEstado(CargaEstado.CON_ERRORES);
                carga.setTotalFilas(0);
                cargaRepository.save(carga);
                return carga;
            }

            String linea;
            while ((linea = reader.readLine()) != null) {
                numFila++;

                if (numFila > maxFilas) {
                    log.warn("Archivo excede maximo de {} filas. Fila {} ignorada.", maxFilas, numFila);
                    errores.add(CargaError.builder()
                            .carga(carga)
                            .numFila(numFila)
                            .campo("all")
                            .motivo("Fila excede el limite maximo de " + maxFilas + " filas")
                            .build());
                    filasRechazadas++;
                    continue;
                }

                String[] campos = linea.split(";", -1);
                List<CsvValidator.CsvValidationError> erroresFila = CsvValidator.validarFila(campos, numFila);

                String referencia = campos.length > 5 ? safeTrim(campos[4]) : "";
                if (!referencia.isEmpty() && !referenciasVistas.add(referencia)) {
                    erroresFila.add(new CsvValidator.CsvError("referencia_banco",
                            "Referencia duplicada en el archivo: " + referencia));
                }

                if (!erroresFila.isEmpty()) {
                    for (CsvValidator.CsvValidationError error : erroresFila) {
                        errores.add(CargaError.builder()
                                .carga(carga)
                                .numFila(numFila)
                                .campo(error.campo())
                                .motivo(error.motivo())
                                .build());
                    }
                    filasRechazadas++;
                    continue;
                }

                String rut = safeTrim(campos[0]);
                String nombre = safeTrim(campos[1]);
                Double monto = Double.parseDouble(safeTrim(campos[2]).replace(",", "."));
                String nombreBanco = safeTrim(campos[3]);
                String cuenta = safeTrim(campos[4]);
                String referenciaBanco = safeTrim(campos[5]);

                if (solicitudRepository.existsByReferenciaBanco(referenciaBanco)) {
                    errores.add(CargaError.builder()
                            .carga(carga)
                            .numFila(numFila)
                            .campo("referencia_banco")
                            .motivo("Referencia ya existente en el sistema: " + referenciaBanco)
                            .build());
                    filasRechazadas++;
                    continue;
                }

                Banco banco = bancoRepository.findByNombreBancoIgnoreCase(nombreBanco).orElse(null);
                if (banco == null) {
                    errores.add(CargaError.builder()
                            .carga(carga)
                            .numFila(numFila)
                            .campo("banco_destino")
                            .motivo("Banco no encontrado: " + nombreBanco)
                            .build());
                    filasRechazadas++;
                    continue;
                }

                Solicitud solicitud = Solicitud.builder()
                        .folio(generarFolio())
                        .rutCliente(rut)
                        .nombreCliente(nombre)
                        .monto(monto)
                        .bancoDestino(banco)
                        .cuentaDestino(cuenta)
                        .referenciaBanco(referenciaBanco)
                        .origen(Origen.CARGA_MASIVA)
                        .estado(Estado.EN_REVISION)
                        .createdBy(usuario)
                        .updatedBy(usuario)
                        .build();

                solicitudesBatch.add(solicitud);
                filasOk++;

                if (solicitudesBatch.size() >= BATCH_SIZE) {
                    flushBatch(solicitudesBatch, carga, usuario);
                    solicitudesBatch.clear();
                }

            }

            if (!solicitudesBatch.isEmpty()) {
                flushBatch(solicitudesBatch, carga, usuario);
                solicitudesBatch.clear();
            }

        } catch (IOException e) {
            log.error("Error leyendo archivo CSV", e);
            carga.setEstado(CargaEstado.CON_ERRORES);
        }

        carga.setTotalFilas(numFila);
        carga.setFilasOk(filasOk);
        carga.setFilasRechazadas(filasRechazadas);
        carga.setEstado(filasRechazadas > 0 ? CargaEstado.CON_ERRORES : CargaEstado.COMPLETADA);
        cargaRepository.save(carga);

        cargaErrorRepository.saveAll(errores);

        log.info("Carga {} finalizada: {} total, {} OK, {} rechazadas",
                carga.getId(), numFila, filasOk, filasRechazadas);

        return carga;
    }

    @Transactional
    public void flushBatch(List<Solicitud> solicitudes, Carga carga, User usuario) {
        List<Solicitud> saved = solicitudRepository.saveAll(solicitudes);
        solicitudRepository.flush();

        List<EventoSolicitud> eventos = saved.stream()
                .map(s -> EventoSolicitud.builder()
                        .solicitud(s)
                        .usuario(usuario)
                        .estadoOrigen(null)
                        .estadoDestino(Estado.EN_REVISION)
                        .comentario("Solicitud creada por carga masiva")
                        .build())
                .toList();
        eventoSolicitudRepository.saveAll(eventos);
        eventoSolicitudRepository.flush();
    }

    private String generarFolio() {
        int anioActual = java.time.LocalDate.now().getYear();
        long correlativo = solicitudRepository.nextFolio();
        return String.format("DEV-%d-%06d", anioActual, correlativo);
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }
}
