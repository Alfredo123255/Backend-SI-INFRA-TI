package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ActualizarUmbralRequest;
import com.infrati.backendinfrati.dto.UmbralRespuesta;
import com.infrati.backendinfrati.service.UmbralService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/umbrales")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class UmbralController {
    private final UmbralService service;

    public UmbralController(UmbralService service) {
        this.service = service;
    }

    @GetMapping
    public List<UmbralRespuesta> listar() {
        return service.listar();
    }

    @PutMapping("/{nombre}")
    public UmbralRespuesta actualizar(@PathVariable String nombre, @RequestBody ActualizarUmbralRequest solicitud) {
        return service.actualizar(nombre, solicitud);
    }
}
