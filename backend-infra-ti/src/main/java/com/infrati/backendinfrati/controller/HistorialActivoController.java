package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.EventoHistoricoActivo;
import com.infrati.backendinfrati.service.HistorialActivoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class HistorialActivoController {
    private final HistorialActivoService service;

    public HistorialActivoController(HistorialActivoService service) {
        this.service = service;
    }

    @GetMapping("/{id}/historial")
    public List<EventoHistoricoActivo> listar(@PathVariable Long id) {
        return service.listar(id);
    }
}
