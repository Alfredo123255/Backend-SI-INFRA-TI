package com.infrati.backendinfrati.controller;
import com.infrati.backendinfrati.dto.AccesoDto.*;
import com.infrati.backendinfrati.security.UsuarioPrincipal;
import com.infrati.backendinfrati.service.AccesoService;
import jakarta.servlet.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contextos;
    private final SessionAuthenticationStrategy sesiones;
    private final AccesoService acceso;
    public AuthController(AuthenticationManager manager,SecurityContextRepository contextos,SessionAuthenticationStrategy sesiones,AccesoService acceso) {
        this.manager=manager;this.contextos=contextos;this.sesiones=sesiones;this.acceso=acceso;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @PostMapping("/login") public ResponseEntity<?> login(@RequestBody LoginSolicitud solicitud,HttpServletRequest request,HttpServletResponse response) {
        try {
            if(solicitud==null || solicitud.correo()==null || solicitud.contrasena()==null || solicitud.correo().isBlank() || solicitud.contrasena().isEmpty() || solicitud.contrasena().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new BadCredentialsException("Credenciales incompletas");
            var auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(solicitud.correo().trim().toLowerCase(Locale.ROOT),solicitud.contrasena()));
            sesiones.onAuthentication(auth,request,response);
            var contexto=SecurityContextHolder.createEmptyContext();contexto.setAuthentication(auth);SecurityContextHolder.setContext(contexto);
            contextos.saveContext(contexto,request,response);
            return ResponseEntity.ok(acceso.sesion(((UsuarioPrincipal)auth.getPrincipal()).getId()));
        } catch(DisabledException ex) {
            return ResponseEntity.status(403).body(Map.of("message","Tu usuario está desactivado. Contacta al administrador del sistema."));
        } catch(org.springframework.security.core.AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("message","Correo o contraseña incorrectos."));
        }
    }
    @GetMapping("/me") public UsuarioSesion me(@AuthenticationPrincipal UsuarioPrincipal usuario) { return acceso.sesion(usuario.getId()); }
}
