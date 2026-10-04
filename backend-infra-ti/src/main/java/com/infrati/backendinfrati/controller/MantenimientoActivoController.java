package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.CambiarMantenimientoRequest;
import com.infrati.backendinfrati.dto.MantenimientoActivoRespuesta;
import com.infrati.backendinfrati.service.MantenimientoActivoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class MantenimientoActivoController {
    private final MantenimientoActivoService service;

    public MantenimientoActivoController(MantenimientoActivoService service) {
        this.service = service;
    }

    @PatchMapping("/{id}/mantenimiento")
    public MantenimientoActivoRespuesta cambiar(@PathVariable Long id,
            @RequestBody CambiarMantenimientoRequest solicitud) {
        return service.cambiar(id, solicitud);
    }
}
