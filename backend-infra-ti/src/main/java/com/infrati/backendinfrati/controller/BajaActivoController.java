package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.BajaActivoRespuesta;
import com.infrati.backendinfrati.dto.DarDeBajaRequest;
import com.infrati.backendinfrati.service.BajaActivoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class BajaActivoController {
    private final BajaActivoService service;

    public BajaActivoController(BajaActivoService service) {
        this.service = service;
    }

    @PatchMapping("/{id}/baja")
    public BajaActivoRespuesta darDeBaja(@PathVariable Long id, @RequestBody DarDeBajaRequest solicitud) {
        return service.darDeBaja(id, solicitud);
    }
}
