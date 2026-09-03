package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.StorageListadoDTO;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.service.ExportService;
import com.infrati.backendinfrati.service.StorageService;
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

@RestController
@RequestMapping("/api/storage")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class StorageController {

    private final StorageService service;
    private final ExportService exportService;

    public StorageController(StorageService service, ExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping
    public List<StorageListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, proyecto, fabricante, q);
    }

    @GetMapping("/{id}")
    public Storage detalle(@PathVariable Long id) {
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
        List<Storage> datos = service.buscarParaExportar(estado, ubicacion, proyecto, fabricante, q);
        byte[] archivo = exportService.exportarStorage(datos, csv);

        MediaType tipo = csv
                ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String nombre = "storage." + (csv ? "csv" : "xlsx");

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(archivo);
    }
}
