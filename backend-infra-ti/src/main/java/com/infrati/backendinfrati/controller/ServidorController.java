package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ServidorListadoDTO;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.service.ServidorService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RF-014 a RF-021: listado unico (fisicos + blades, columna tipo) con
 * filtros y busqueda y ficha de detalle. Los mismos GET de aca abajo ya
 * satisfacen RF-020 (API JSON).
 */
@RestController
@RequestMapping("/api/servidores")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ServidorController {

    private final ServidorService service;

    public ServidorController(ServidorService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServidorListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, proyecto, fabricante, q);
    }

    @GetMapping("/{id}")
    public Servidor detalle(@PathVariable Long id) {
        return service.detalle(id);
    }
}
