package com.infrati.backendinfrati.controller;
import com.infrati.backendinfrati.dto.AccesoDto.*;
import com.infrati.backendinfrati.security.UsuarioPrincipal;
import com.infrati.backendinfrati.service.AccesoService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/usuarios")
public class UsuarioController {
    private final AccesoService acceso;
    public UsuarioController(AccesoService acceso) { this.acceso=acceso; }
    @GetMapping public List<UsuarioRespuesta> listar() { return acceso.listarUsuarios(); }
    @PostMapping public ResponseEntity<UsuarioRespuesta> crear(@RequestBody UsuarioCrearSolicitud solicitud) { return ResponseEntity.status(HttpStatus.CREATED).body(acceso.crearUsuario(solicitud)); }
    @PutMapping("/{id}") public UsuarioRespuesta editar(@PathVariable Integer id,@RequestBody UsuarioActualizarSolicitud solicitud) { return acceso.actualizarUsuario(id,solicitud); }
    @PatchMapping("/{id}/estado") public UsuarioRespuesta estado(@PathVariable Integer id,@RequestBody EstadoUsuarioSolicitud solicitud,@AuthenticationPrincipal UsuarioPrincipal propio) { return acceso.cambiarEstado(id,solicitud,propio.getId()); }
}
