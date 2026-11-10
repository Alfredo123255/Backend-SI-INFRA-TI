package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.model.*;
import com.infrati.backendinfrati.repository.*;
import com.infrati.backendinfrati.service.AccesoService;
import com.infrati.backendinfrati.dto.AccesoDto.*;
import com.infrati.backendinfrati.exception.RecursoDuplicadoException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@Transactional
class AccesoSeguridadTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccesoService acceso;
    private Usuario admin,consulta,operador;
    private String clave;
    private String sufijo;
    @BeforeEach void preparar() {
        clave=UUID.randomUUID().toString(); sufijo=UUID.randomUUID().toString();
        admin=crear("admin", "Administrador"); consulta=crear("consulta","Consulta"); operador=crear("operador","Operador");
    }
    private Usuario crear(String nombre,String rol) {
        var u=new Usuario();u.setNombre(nombre);u.setCorreo(nombre+sufijo+"@example.org");
        u.setContrasenaHash(encoder.encode(clave));u.setRol(roles.findByNombreIgnoreCase(rol).orElseThrow());
        u.setFechaCreacion(LocalDateTime.now());return usuarios.saveAndFlush(u);
    }
    private MockHttpSession login(Usuario u) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"correo\":\""+u.getCorreo()+"\",\"contrasena\":\""+clave+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contrasena_hash").doesNotExist())
            .andReturn().getRequest().getSession(false);
    }
    @Test void sinSesionEs401JsonSinRedireccion() throws Exception {
        mvc.perform(get("/api/servidores")).andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Location")).andExpect(jsonPath("$.message").exists());
    }
    @Test void contrasenaIncorrectaEs401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"correo\":\""+admin.getCorreo()+"\",\"contrasena\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }
    @Test void usuarioInactivoEs403Distinto() throws Exception {
        consulta.setActivo(false);usuarios.saveAndFlush(consulta);
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"correo\":\""+consulta.getCorreo()+"\",\"contrasena\":\""+clave+"\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Tu usuario está desactivado. Contacta al administrador del sistema."));
    }
    @Test void consultaNoModificaUmbral() throws Exception {
        mvc.perform(put("/api/umbrales/cpu_uso_ghz").session(login(consulta)).with(csrf())
            .contentType("application/json").content("{\"advertencia\":80,\"critico\":90}"))
            .andExpect(status().isForbidden());
    }
    @Test void administradorSiModificaUmbral() throws Exception {
        mvc.perform(put("/api/umbrales/cpu_uso_ghz").session(login(admin)).with(csrf())
            .contentType("application/json").content("{\"advertencia\":80,\"critico\":90}"))
            .andExpect(status().isOk());
    }
    @Test void operadorDaBajaPeroNoCreaUsuarios() throws Exception {
        var session=login(operador);
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",sufijo);
        Long id=jdbc.queryForObject("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo) VALUES(?,?,?,'SERVIDOR','Encendido') RETURNING id",Long.class,sufijo,"CP acceso",sufijo);
        jdbc.update("INSERT INTO servidor(id,tipo) VALUES(?,'RACKEABLE')",id);
        mvc.perform(patch("/api/activos/"+id+"/baja").session(session).with(csrf())
            .contentType("application/json").content("{\"responsable\":\"Operaciones\",\"motivo\":\"Prueba de permisos\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/usuarios").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void propioUsuarioNoSeDesactiva() throws Exception {
        mvc.perform(patch("/api/usuarios/"+admin.getId()+"/estado").session(login(admin)).with(csrf())
            .contentType("application/json").content("{\"activo\":false}"))
            .andExpect(status().isConflict());
    }
    @Test void ultimoPermisoDeAdministracionNoSeQuita() throws Exception {
        var permisos=new LinkedHashMap<>(admin.getRol().permisos());permisos.put("administrar_usuarios",false);
        String json="{"+String.join(",",permisos.entrySet().stream().map(e->"\""+e.getKey()+"\":"+e.getValue()).toList())+"}";
        mvc.perform(put("/api/roles/"+admin.getRol().getId()+"/permisos").session(login(admin)).with(csrf()).contentType("application/json").content(json))
            .andExpect(status().isConflict());
    }
    @Test void ultimoAdministradorActivoNoSeDesactiva() throws Exception {
        // El servicio recibe el identificador del solicitante; la prohibición es general, además de la de auto-desactivación.
        assertThrows(RecursoDuplicadoException.class,()->acceso.cambiarEstado(admin.getId(),new EstadoUsuarioSolicitud(false),operador.getId()));
    }
    @Test void ultimoAdministradorNoPuedeCambiarAConsulta() throws Exception {
        mvc.perform(put("/api/usuarios/"+admin.getId()).session(login(admin)).with(csrf()).contentType("application/json")
            .content("{\"nombre\":\"Admin\",\"rol_id\":"+consulta.getRol().getId()+"}"))
            .andExpect(status().isConflict());
    }
    @Test void rolConUsuariosNoSeElimina() throws Exception {
        mvc.perform(delete("/api/roles/"+consulta.getRol().getId()).session(login(admin)).with(csrf()))
            .andExpect(status().isConflict());
    }
    @Test void correoRepetidoNormalizadoEs409() throws Exception {
        mvc.perform(post("/api/usuarios").session(login(admin)).with(csrf()).contentType("application/json")
            .content("{\"nombre\":\"Repetido\",\"correo\":\""+consulta.getCorreo().toUpperCase(Locale.ROOT)+"\",\"contrasenaTemporal\":\""+clave+"\",\"rol_id\":"+consulta.getRol().getId()+"}"))
            .andExpect(status().isConflict());
    }
    @Test void sesionPersisteSinExponerHashYLogoutLaCierra() throws Exception {
        var session=login(admin);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(admin.getId())).andExpect(jsonPath("$.permisos").isArray())
            .andExpect(jsonPath("$.contrasena_hash").doesNotExist());
        mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].contrasena_hash").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void sinCsrfNoSeEscribeNiSeIniciaSesion() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/umbrales/cpu_uso_ghz").session(login(admin)).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void desactivarRevocaSesionExistente() throws Exception {
        var session=login(consulta);consulta.setActivo(false);usuarios.saveAndFlush(consulta);
        mvc.perform(get("/api/servidores").session(session)).andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
    }
    @Test void permisosDeSesionSeActualizanEnCadaSolicitud() throws Exception {
        var session=login(operador);
        operador.setRol(consulta.getRol());usuarios.saveAndFlush(operador);
        mvc.perform(post("/api/conexiones-snmp/probar").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void permiteOtroRolAdministradorSinBloqueoPorNombre() {
        var r=new Rol();r.setNombre("Delegado "+sufijo);r.aplicarPermisos(admin.getRol().permisos());roles.saveAndFlush(r);
        var otro=crear("delegado","Consulta");otro.setRol(r);usuarios.saveAndFlush(otro);
        var permisos=new LinkedHashMap<>(admin.getRol().permisos());permisos.put("administrar_usuarios",false);
        assertFalse(acceso.cambiarPermisos(admin.getRol().getId(),permisos).permisos().get("administrar_usuarios"));
    }
    @Test void rolNuevoSoloPermiteVerInventario() {
        var r=acceso.crearRol(new CrearRolSolicitud("CP "+sufijo));
        assertEquals(1,r.permisos().values().stream().filter(Boolean::booleanValue).count());
        assertTrue(r.permisos().get("ver_inventario"));
    }
    @Test void bootstrapIncompletoNoCreaYNoSeRepiteSiYaHayUsuarios() {
        long antes=usuarios.count();
        assertFalse(acceso.crearPrimerAdministrador(null,"Admin",clave));
        assertFalse(acceso.crearPrimerAdministrador("nuevo@example.org","Admin",clave));
        assertEquals(antes,usuarios.count());
    }
    @Test void corsPermiteCredencialesDelFrontend() throws Exception {
        mvc.perform(options("/api/usuarios").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type,x-xsrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"))
            .andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }
}
