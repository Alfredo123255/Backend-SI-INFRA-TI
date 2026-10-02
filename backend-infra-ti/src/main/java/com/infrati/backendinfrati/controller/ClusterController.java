package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ClusterRespuesta;
import com.infrati.backendinfrati.dto.CrearClusterRequest;
import com.infrati.backendinfrati.service.ClusterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clusters")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ClusterController {
    private final ClusterService service;

    public ClusterController(ClusterService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClusterRespuesta> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<ClusterRespuesta> crear(@RequestBody CrearClusterRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(solicitud));
    }
}
