package com.infrati.backendinfrati.dto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.*;
public final class AccesoDto {
    private AccesoDto() {}
    public record RolResumen(Integer id,String nombre) {}
    public record UsuarioSesion(Integer id,String nombre,String correo,RolResumen rol,List<String> permisos) {}
    public record UsuarioRespuesta(Integer id,String nombre,String correo,RolResumen rol,boolean activo,
        @JsonProperty("fecha_creacion") LocalDateTime fechaCreacion) {}
    public record RolRespuesta(Integer id,String nombre,Map<String,Boolean> permisos,long usuarios) {}
    public record LoginSolicitud(String correo,@JsonAlias({"contraseña","clave"}) String contrasena) {
        @Override public String toString() { return "LoginSolicitud[credenciales reservadas]"; }
    }
    public record UsuarioCrearSolicitud(String nombre,String correo,
        @JsonAlias({"contrasena","contraseña","clave"}) String contrasenaTemporal,@JsonAlias("rol_id") Integer rolId) {
        @Override public String toString() { return "UsuarioCrearSolicitud[datos reservados]"; }
    }
    public record UsuarioActualizarSolicitud(String nombre,@JsonAlias("rol_id") Integer rolId) {}
    public record EstadoUsuarioSolicitud(Boolean activo) {}
    public record CrearRolSolicitud(String nombre) {}
}
