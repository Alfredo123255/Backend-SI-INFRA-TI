package com.infrati.backendinfrati.dto;

public record RegistroConexionRequest(String ipGestion, String usuario, String clave,
        Integer frecuenciaActualizacion, String clusterId) {
}
