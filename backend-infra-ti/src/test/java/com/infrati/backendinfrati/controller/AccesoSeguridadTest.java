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

    private ResultActions editarClave(Usuario solicitante,String campo) throws Exception {
        return mvc.perform(put("/api/usuarios/"+consulta.getId()).session(login(solicitante)).with(csrf())
            .contentType("application/json").content("{\"nombre\":\"Consulta editada\",\"rolId\":"+consulta.getRol().getId()+campo+"}"));
    }
    @Test void editarContrasenaPermiteLoginNuevoYRechazaAnterior() throws Exception {
        String nueva=UUID.randomUUID().toString();
        editarClave(admin,",\"contrasena\":\""+nueva+"\"").andExpect(status().isOk())
            .andExpect(jsonPath("$.contrasena").doesNotExist()).andExpect(jsonPath("$.contrasena_hash").doesNotExist());
        var hash=usuarios.findById(consulta.getId()).orElseThrow().getContrasenaHash();
        assertNotEquals(nueva,hash);assertTrue(encoder.matches(nueva,hash));assertFalse(encoder.matches(clave,hash));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"correo\":\""+consulta.getCorreo()+"\",\"contrasena\":\""+clave+"\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"correo\":\""+consulta.getCorreo()+"\",\"contrasena\":\""+nueva+"\"}"))
            .andExpect(status().isOk());
    }
    @Test void editarSinContrasenaNulaOVaciaConservaHash() throws Exception {
        String hash=consulta.getContrasenaHash();
        for(String campo:List.of("",",\"contrasena\":null",",\"contrasena\":\"\"")) {
            editarClave(admin,campo).andExpect(status().isOk());
            assertEquals(hash,usuarios.findById(consulta.getId()).orElseThrow().getContrasenaHash());
        }
        login(consulta);
    }
    @Test void editarContrasenaCortaRechazaSinCambiarHash() throws Exception {
        String hash=consulta.getContrasenaHash();
        editarClave(admin,",\"contrasena\":\"corta\"").andExpect(status().isBadRequest());
        assertEquals(hash,consulta.getContrasenaHash());
    }
    @Test void editarContrasenaQueExcede72BytesRechaza() throws Exception {
        editarClave(admin,",\"contrasena\":\""+"á".repeat(37)+"\"").andExpect(status().isBadRequest());
    }
    @Test void editarContrasenaDeOchoCaracteresSeAcepta() throws Exception {
        editarClave(admin,",\"contrasena\":\"Abc@1234\"").andExpect(status().isOk());
        assertTrue(encoder.matches("Abc@1234",consulta.getContrasenaHash()));
    }
    @Test void usuarioSinPermisoNoCambiaContrasenas() throws Exception {
        String hash=consulta.getContrasenaHash();
        editarClave(operador,",\"contrasena\":\"Abc@1234\"").andExpect(status().isForbidden());
        assertEquals(hash,consulta.getContrasenaHash());
    }
    @Test void solicitudDeEdicionNoExponeClaveEnToString() {
        assertFalse(new UsuarioActualizarSolicitud("Usuario",1,clave).toString().contains(clave));
    }

    private Usuario usuarioConPermiso(String permiso) {
        var r=new Rol();r.setNombre("Prueba "+permiso+" "+sufijo);r.aplicarPermisos(Map.of(permiso,true));roles.saveAndFlush(r);
        var u=crear("aislado","Consulta");u.setRol(r);return usuarios.saveAndFlush(u);
    }
    @Test void consultaNoVeConfiguracionUmbralesPeroConservaColoresDeMetricas() throws Exception {
        var session=login(consulta);
        mvc.perform(get("/api/umbrales").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/metricas/limites").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").exists()).andExpect(jsonPath("$[0].fechaModificacion").doesNotExist())
            .andExpect(jsonPath("$[?(@.nombre == 'dias_para_eos')]").isEmpty());
    }
    @Test void permisoDeUmbralesPermiteConsultarYConfigurarSinInventario() throws Exception {
        var session=login(usuarioConPermiso("configurar_umbrales"));
        mvc.perform(get("/api/umbrales").session(session)).andExpect(status().isOk());
        mvc.perform(put("/api/umbrales/cpu_uso_ghz").session(session).with(csrf()).contentType("application/json")
            .content("{\"advertencia\":80,\"critico\":90}")).andExpect(status().isOk());
        mvc.perform(get("/api/servidores").session(session)).andExpect(status().isForbidden());
    }
    @Test void mantenimientoSinInventarioPuedeListarYUsarSoloSuAccion() throws Exception {
        var session=login(usuarioConPermiso("gestionar_mantenimiento"));
        mvc.perform(get("/api/activos/ciclo-vida").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/activos/mantenimiento").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/servidores").session(session)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/activos/0/mantenimiento").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/activos/0/baja").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void bajaSinInventarioPuedeListarYUsarSoloSuAccion() throws Exception {
        var session=login(usuarioConPermiso("dar_baja"));
        mvc.perform(get("/api/activos/ciclo-vida").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/activos/mantenimiento").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/servidores").session(session)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/activos/0/baja").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/activos/0/mantenimiento").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void consultaPuedeVerCicloVidaPeroNoEjecutaAcciones() throws Exception {
        var session=login(consulta);
        mvc.perform(get("/api/activos/ciclo-vida").session(session)).andExpect(status().isOk());
        for(String accion:List.of("baja","mantenimiento"))
            mvc.perform(patch("/api/activos/0/"+accion).session(session).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
    @Test void listaOperativaSoloExponeIdentificacionYEstado() throws Exception {
        jdbc.update("INSERT INTO datacenters(nombre) VALUES (?)",sufijo);
        jdbc.update("INSERT INTO activo(numero_serie,hostname,ubicacion,tipo_activo,estado_operativo) VALUES(?,?,?,'STORAGE','Encendido')",sufijo,"Equipo permisos "+sufijo,sufijo);
        String resultado=mvc.perform(get("/api/activos/ciclo-vida").session(login(consulta))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertTrue(resultado.contains("Equipo permisos "+sufijo));
        assertFalse(resultado.contains("numero_serie"));assertFalse(resultado.contains("ip_gestion"));assertFalse(resultado.contains("contrasena"));
    }
    @Test void sinPermisosNoPuedeLeerCicloVidaNiUmbrales() throws Exception {
        var session=login(usuarioConPermiso("exportar"));
        for(String ruta:List.of("/api/activos/ciclo-vida","/api/activos/mantenimiento","/api/umbrales","/api/metricas/limites"))
            mvc.perform(get(ruta).session(session)).andExpect(status().isForbidden());
    }
}
