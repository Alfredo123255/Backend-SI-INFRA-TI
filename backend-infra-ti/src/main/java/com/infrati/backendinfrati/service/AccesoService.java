package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.AccesoDto.*;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;

import com.infrati.backendinfrati.model.Rol;
import com.infrati.backendinfrati.model.Usuario;
import com.infrati.backendinfrati.repository.RolRepository;
import com.infrati.backendinfrati.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AccesoService {
    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final EntityManager em;
    public AccesoService(RolRepository roles,UsuarioRepository usuarios,PasswordEncoder encoder,EntityManager em) {
        this.roles=roles;this.usuarios=usuarios;this.encoder=encoder;this.em=em;
    }
    // Serializa todas las modificaciones de acceso, incluso desde varias instancias Java.
    private void bloquear() { em.createNativeQuery("SELECT pg_advisory_xact_lock(841904004)").getSingleResult(); }
    private void exigirAdministradorActivo() {
        if(usuarios.contarAdministradoresActivos()==0)
            throw new RecursoDuplicadoException("Debe permanecer al menos un usuario activo con el permiso administrar_usuarios.");
    }
    private Usuario usuario(Integer id) { return usuarios.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Usuario no encontrado.")); }
    private Rol rol(Integer id) {
        if(id==null)throw new SolicitudInvalidaException("Selecciona un rol.");
        return roles.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Rol no encontrado."));
    }
    private static String nombre(String valor) {
        if(valor==null || valor.isBlank() || valor.trim().length()>255 || valor.chars().anyMatch(Character::isISOControl))
            throw new SolicitudInvalidaException("El nombre es obligatorio y debe tener como máximo 255 caracteres.");
        return valor.trim();
    }
    private static String correo(String valor) {
        if(valor==null || valor.trim().length()>254 || !valor.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new SolicitudInvalidaException("Ingresa un correo válido.");
        return valor.trim().toLowerCase(Locale.ROOT);
    }
    private String codificar(String valor) {
        if(valor==null || valor.length()<8)throw new SolicitudInvalidaException("La contraseña debe tener al menos 8 caracteres.");
        if(valor.getBytes(StandardCharsets.UTF_8).length>72)throw new SolicitudInvalidaException("La contraseña no puede superar los 72 bytes admitidos por BCrypt.");
        return encoder.encode(valor);
    }
    private UsuarioRespuesta respuesta(Usuario u) {
        return new UsuarioRespuesta(u.getId(),u.getNombre(),u.getCorreo(),new RolResumen(u.getRol().getId(),u.getRol().getNombre()),u.isActivo(),u.getFechaCreacion());
    }
    private RolRespuesta respuesta(Rol r) { return new RolRespuesta(r.getId(),r.getNombre(),r.permisos(),usuarios.countByRolId(r.getId())); }
    @Transactional(readOnly=true)
    public UsuarioSesion sesion(Integer id) {
        var u=usuario(id);
        return new UsuarioSesion(u.getId(),u.getNombre(),u.getCorreo(),new RolResumen(u.getRol().getId(),u.getRol().getNombre()),
            u.getRol().permisos().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList());
    }
    @Transactional(readOnly=true)
    public List<UsuarioRespuesta> listarUsuarios() { return usuarios.findAllByOrderByIdAsc().stream().map(this::respuesta).toList(); }
    @Transactional(readOnly=true)
    public List<RolRespuesta> listarRoles() { return roles.findAllByOrderByIdAsc().stream().map(this::respuesta).toList(); }
    @Transactional
    public UsuarioRespuesta crearUsuario(UsuarioCrearSolicitud s) {
        bloquear();
        if(s==null)throw new SolicitudInvalidaException("Completa los datos del usuario.");
        String email=correo(s.correo());
        if(usuarios.existsByCorreo(email))throw new RecursoDuplicadoException("Ya existe un usuario con este correo.");
        var u=new Usuario();u.setNombre(nombre(s.nombre()));u.setCorreo(email);u.setRol(rol(s.rolId()));
        u.setContrasenaHash(codificar(s.contrasenaTemporal()));u.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));
        return respuesta(usuarios.saveAndFlush(u));
    }
    @Transactional
    public UsuarioRespuesta actualizarUsuario(Integer id,UsuarioActualizarSolicitud s) {
        bloquear();
        if(s==null)throw new SolicitudInvalidaException("Completa los datos del usuario.");
        var u=usuario(id);u.setNombre(nombre(s.nombre()));u.setRol(rol(s.rolId()));
        if(s.contrasena()!=null && !s.contrasena().isEmpty())u.setContrasenaHash(codificar(s.contrasena()));
        usuarios.saveAndFlush(u);
        exigirAdministradorActivo();return respuesta(u);
    }
    @Transactional
    public UsuarioRespuesta cambiarEstado(Integer id,EstadoUsuarioSolicitud s,Integer propioId) {
        bloquear();
        if(s==null || s.activo()==null)throw new SolicitudInvalidaException("Indica si el usuario está activo.");
        if(id.equals(propioId) && !s.activo())throw new RecursoDuplicadoException("No puedes desactivar tu propio usuario.");
        var u=usuario(id);u.setActivo(s.activo());usuarios.saveAndFlush(u);exigirAdministradorActivo();return respuesta(u);
    }
    @Transactional
    public RolRespuesta crearRol(CrearRolSolicitud s) {
        bloquear();
        if(s==null)throw new SolicitudInvalidaException("El nombre del rol es obligatorio.");
        String valor=nombre(s.nombre());
        if(roles.existsByNombreIgnoreCase(valor))throw new RecursoDuplicadoException("Ya existe un rol con este nombre.");
        var r=new Rol();r.setNombre(valor);r.aplicarPermisos(Map.of("ver_inventario",true));
        return respuesta(roles.saveAndFlush(r));
    }
    @Transactional
    public RolRespuesta cambiarPermisos(Integer id,Map<String,Boolean> valores) {
        bloquear();var r=rol(id);
        if(valores==null || !valores.keySet().equals(r.permisos().keySet()) || valores.values().stream().anyMatch(Objects::isNull))
            throw new SolicitudInvalidaException("Envía los nueve permisos del rol con valores true o false.");
        r.aplicarPermisos(valores);roles.saveAndFlush(r);exigirAdministradorActivo();return respuesta(r);
    }
    @Transactional
    public void eliminarRol(Integer id) {
        bloquear();var r=rol(id);
        if(usuarios.countByRolId(id)>0)throw new RecursoDuplicadoException("No se puede eliminar el rol porque tiene usuarios asignados.");
        roles.delete(r);roles.flush();
    }
    @Transactional
    public boolean crearPrimerAdministrador(String email,String nombre,String contrasena) {
        if(email==null || email.isBlank() || nombre==null || nombre.isBlank() || contrasena==null || contrasena.isBlank())return false;
        bloquear();if(usuarios.count()!=0)return false;
        var r=roles.findByNombreIgnoreCase("Administrador").orElseThrow(()->new IllegalStateException("Aplica la migración de usuarios y roles antes de iniciar."));
        if(!Boolean.TRUE.equals(r.permisos().get("administrar_usuarios")))throw new IllegalStateException("El rol inicial debe permitir administrar usuarios.");
        var u=new Usuario();u.setNombre(nombre(nombre));u.setCorreo(correo(email));u.setRol(r);
        u.setContrasenaHash(codificar(contrasena));u.setFechaCreacion(LocalDateTime.now(ZoneOffset.UTC));usuarios.saveAndFlush(u);return true;
    }
}
