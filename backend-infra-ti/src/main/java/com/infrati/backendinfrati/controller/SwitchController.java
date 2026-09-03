package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.SwitchListadoDTO;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.service.ExportService;
import com.infrati.backendinfrati.service.SwitchService;
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
@RequestMapping("/api/switches")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class SwitchController {

    private final SwitchService service;
    private final ExportService exportService;

    public SwitchController(SwitchService service, ExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping
    public List<SwitchListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, proyecto, fabricante, q);
    }

    @GetMapping("/{id}")
    public Switch detalle(@PathVariable Long id) {
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
        List<Switch> datos = service.buscarParaExportar(estado, ubicacion, proyecto, fabricante, q);
        byte[] archivo = exportService.exportarSwitches(datos, csv);

        MediaType tipo = csv
                ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String nombre = "switches." + (csv ? "csv" : "xlsx");

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(archivo);
    }
}
