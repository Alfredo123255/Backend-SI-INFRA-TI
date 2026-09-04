package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ChasisBladeListadoDTO;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.service.ChasisBladeService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chasis-blade")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ChasisBladeController {

    private final ChasisBladeService service;

    public ChasisBladeController(ChasisBladeService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChasisBladeListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String cluster,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, cluster, fabricante, q);
    }

    @GetMapping("/{id}")
    public ChasisBlade detalle(@PathVariable Long id) {
        return service.obtenerDetalle(id);
    }
}
