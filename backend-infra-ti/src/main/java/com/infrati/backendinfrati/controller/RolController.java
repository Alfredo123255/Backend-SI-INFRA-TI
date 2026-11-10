package com.infrati.backendinfrati.controller;
import com.infrati.backendinfrati.dto.AccesoDto.*;
import com.infrati.backendinfrati.service.AccesoService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/roles")
public class RolController {
    private final AccesoService acceso;
    public RolController(AccesoService acceso) { this.acceso=acceso; }
    @GetMapping public List<RolRespuesta> listar() { return acceso.listarRoles(); }
    @PostMapping public ResponseEntity<RolRespuesta> crear(@RequestBody CrearRolSolicitud solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(acceso.crearRol(solicitud)); }
    @PutMapping("/{id}/permisos") public RolRespuesta permisos(@PathVariable Integer id,@RequestBody Map<String,Boolean> valores) { return acceso.cambiarPermisos(id,valores); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Integer id) { acceso.eliminarRol(id);return ResponseEntity.noContent().build(); }
}
