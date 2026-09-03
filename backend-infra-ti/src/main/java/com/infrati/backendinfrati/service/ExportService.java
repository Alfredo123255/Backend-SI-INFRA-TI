package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ServidorListadoDTO;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.model.Activos.Switch;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * RF-018 (Excel) y RF-019 (CSV). Exporta la misma forma "tabla" que ve el
 * usuario en el listado (RF-017), no la ficha de detalle completa: una fila
 * por activo con sus columnas visibles, igual que se veria impreso.
 */
@Service
public class ExportService {

    private static final List<String> ENCABEZADOS_SERVIDOR =
            List.of("id", "tipo", "hostname", "ubicacion", "estado", "proyecto", "uso_cpu_pct", "uso_memoria_pct");

    private static final List<String> ENCABEZADOS_STORAGE =
            List.of("id", "hostname", "ubicacion", "estado", "proyecto", "fabricante", "capacidad_utilizada", "protocolo");

    private static final List<String> ENCABEZADOS_SWITCH =
            List.of("id", "hostname", "ubicacion", "estado", "fabricante", "tipo_red", "puertos_ocupados", "puertos_totales", "ip_gestion");

    public byte[] exportarServidores(List<ServidorListadoDTO> datos, boolean csv) {
        List<List<String>> filas = datos.stream().map(s -> List.of(
                texto(s.getId()), texto(s.getTipo()), texto(s.getHostname()), texto(s.getUbicacion()),
                texto(s.getEstadoOperativo()), texto(s.getCluster()), texto(s.getUsoCpuPct()), texto(s.getUsoRamPct())
        )).toList();
        return csv ? aCsv(ENCABEZADOS_SERVIDOR, filas) : aExcel("Servidores", ENCABEZADOS_SERVIDOR, filas);
    }

    public byte[] exportarStorage(List<Storage> datos, boolean csv) {
        List<List<String>> filas = datos.stream().map(s -> List.of(
                texto(s.getId()), texto(s.getHostname()), texto(s.getUbicacion()), texto(s.getEstado_operativo()),
                texto(s.getCluster()), texto(s.getFabricante()), texto(s.getCapacidad_usada_TB()), texto(s.getProtocolo_comunicacion())
        )).toList();
        return csv ? aCsv(ENCABEZADOS_STORAGE, filas) : aExcel("Storage", ENCABEZADOS_STORAGE, filas);
    }

    public byte[] exportarSwitches(List<Switch> datos, boolean csv) {
        List<List<String>> filas = datos.stream().map(equipo -> List.of(
                texto(equipo.getId()), texto(equipo.getHostname()), texto(equipo.getUbicacion()), texto(equipo.getEstado_operativo()),
                texto(equipo.getFabricante()), texto(equipo.getTipoRED()), texto(equipo.getCantidad_puertos_ocupados()),
                texto(equipo.getCantidad_puertos()), texto(equipo.getIp_gestion())
        )).toList();
        return csv ? aCsv(ENCABEZADOS_SWITCH, filas) : aExcel("Switches", ENCABEZADOS_SWITCH, filas);
    }

    private byte[] aExcel(String nombreHoja, List<String> encabezados, List<List<String>> filas) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet(nombreHoja);

            Row filaEncabezado = hoja.createRow(0);
            for (int c = 0; c < encabezados.size(); c++) {
                filaEncabezado.createCell(c).setCellValue(encabezados.get(c));
            }

            for (int f = 0; f < filas.size(); f++) {
                Row fila = hoja.createRow(f + 1);
                List<String> valores = filas.get(f);
                for (int c = 0; c < valores.size(); c++) {
                    fila.createCell(c).setCellValue(valores.get(c));
                }
            }

            for (int c = 0; c < encabezados.size(); c++) {
                hoja.autoSizeColumn(c);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel de exportacion", e);
        }
    }

    private byte[] aCsv(List<String> encabezados, List<List<String>> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", encabezados)).append("\n");
        for (List<String> fila : filas) {
            sb.append(String.join(",", fila.stream().map(this::escaparCsv).toList())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        boolean necesitaComillas = valor.contains(",") || valor.contains("\"") || valor.contains("\n");
        String escapado = valor.replace("\"", "\"\"");
        return necesitaComillas ? "\"" + escapado + "\"" : escapado;
    }

    private String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }
}
