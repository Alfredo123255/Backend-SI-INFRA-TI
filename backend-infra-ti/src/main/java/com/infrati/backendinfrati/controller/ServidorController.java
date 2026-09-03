package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ServidorListadoDTO;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.service.ExportService;
import com.infrati.backendinfrati.service.ServidorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RF-014 a RF-021: listado unico (fisicos + blades, columna tipo) con
 * filtros y busqueda, ficha de detalle y exportacion. Los mismos GET de
 * aca abajo ya satisfacen RF-020 (API JSON).
 */
@RestController
@RequestMapping("/api/servidores")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ServidorController {

    private final ServidorService service;
    private final ExportService exportService;

    public ServidorController(ServidorService service, ExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping
    public List<ServidorListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, proyecto, fabricante, q);
    }

    @GetMapping("/{id}")
    public Servidor detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(defaultValue = "excel") String formato,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {

        boolean csv = "csv".equalsIgnoreCase(formato);
        List<ServidorListadoDTO> datos = service.buscarParaExportar(estado, ubicacion, proyecto, fabricante, q);
        byte[] archivo = exportService.exportarServidores(datos, csv);

        MediaType tipo = csv
                ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String nombre = "servidores." + (csv ? "csv" : "xlsx");

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(archivo);
    }
}
