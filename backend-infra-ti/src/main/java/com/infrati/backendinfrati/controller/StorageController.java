package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.StorageListadoDTO;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.service.StorageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    @GetMapping
    public List<StorageListadoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String proyecto,
            @RequestParam(required = false) String fabricante,
            @RequestParam(required = false) String q) {
        return service.listar(estado, ubicacion, proyecto, fabricante, q);
    }

    @GetMapping("/{id}")
    public Storage detalle(@PathVariable Long id) {
        return service.detalle(id);
    }
}
