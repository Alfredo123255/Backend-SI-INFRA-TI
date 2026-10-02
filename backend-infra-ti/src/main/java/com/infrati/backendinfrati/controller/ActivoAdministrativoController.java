package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ActualizarAdministrativosActivoRequest;
import com.infrati.backendinfrati.dto.DatosAdministrativosActivo;
import com.infrati.backendinfrati.service.ActivoAdministrativoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ActivoAdministrativoController {
    private final ActivoAdministrativoService service;

    public ActivoAdministrativoController(ActivoAdministrativoService service) {
        this.service = service;
    }

    @PatchMapping("/{id}/administrativos")
    public DatosAdministrativosActivo actualizar(@PathVariable Long id,
            @RequestBody ActualizarAdministrativosActivoRequest solicitud) {
        return service.actualizar(id, solicitud);
    }
}
