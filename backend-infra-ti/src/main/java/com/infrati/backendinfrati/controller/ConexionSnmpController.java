package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.dto.ActivoCreado;
import com.infrati.backendinfrati.dto.ActualizarConexionSnmpRequest;
import com.infrati.backendinfrati.dto.ConexionSnmpDetalle;
import com.infrati.backendinfrati.dto.ProbarRespuesta;
import com.infrati.backendinfrati.dto.ProbarSolicitud;
import com.infrati.backendinfrati.dto.RegistroConexionRequest;
import com.infrati.backendinfrati.service.ConexionSnmpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conexiones-snmp")
@CrossOrigin(origins = "${app.cors.origen-front}")
public class ConexionSnmpController {
    private final ConexionSnmpService service;

    public ConexionSnmpController(ConexionSnmpService service) {
        this.service = service;
    }

    @PostMapping("/probar")
    public ProbarRespuesta probar(@RequestBody ProbarSolicitud solicitud) {
        return service.probar(solicitud);
    }

    @GetMapping("/activos/{activoId}")
    public ConexionSnmpDetalle obtener(@PathVariable Long activoId) {
        return service.obtenerPorActivo(activoId);
    }

    @PatchMapping("/activos/{activoId}")
    public ConexionSnmpDetalle actualizar(@PathVariable Long activoId,
            @RequestBody ActualizarConexionSnmpRequest solicitud) {
        return service.actualizar(activoId, solicitud);
    }

    @PostMapping
    public ResponseEntity<ActivoCreado> registrar(@RequestBody RegistroConexionRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarYCrear(solicitud));
    }
}
