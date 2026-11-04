package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ActualizarDataCenterRequest;
import com.infrati.backendinfrati.dto.DataCenterRequest;
import com.infrati.backendinfrati.dto.DataCenterRespuesta;
import com.infrati.backendinfrati.service.DataCenterService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/datacenters")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class DataCenterController {
    private final DataCenterService service;

    public DataCenterController(DataCenterService service) {
        this.service = service;
    }

    @GetMapping
    public List<DataCenterRespuesta> listar() {
        return service.listar();
    }
    @PostMapping
    public ResponseEntity<DataCenterRespuesta> crear(@RequestBody DataCenterRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(solicitud));
    }
    @PutMapping("/{nombre}")
    public DataCenterRespuesta actualizar(@PathVariable String nombre, @RequestBody ActualizarDataCenterRequest solicitud) {
        return service.actualizar(nombre, solicitud);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        service.eliminar(nombre);
        return ResponseEntity.noContent().build();
    }
}
