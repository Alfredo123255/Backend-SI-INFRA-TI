package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.MetricasServidorRespuesta;
import com.infrati.backendinfrati.service.MetricasServidorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class MetricasServidorController {
    private final MetricasServidorService service;

    public MetricasServidorController(MetricasServidorService service) {
        this.service = service;
    }

    @GetMapping("/{id}/metricas")
    public MetricasServidorRespuesta consultar(@PathVariable long id,
            @RequestParam(defaultValue = "24h") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) List<Long> comparar) {
        return service.consultar(id, periodo, desde, hasta, comparar);
    }
}
