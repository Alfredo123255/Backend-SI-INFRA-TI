package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.AlertaActivo;
import com.infrati.backendinfrati.service.AlertaActivoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class AlertaActivoController {
    private final AlertaActivoService service;

    public AlertaActivoController(AlertaActivoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertaActivo> listar() {
        return service.listar();
    }
}
