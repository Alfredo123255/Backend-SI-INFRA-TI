package com.infrati.backendinfrati.dto;

public record RegistroConexionRequest(String ipGestion, String usuario, String clave,
        String clavePrivacidad, Integer frecuenciaActualizacion, String clusterId) {
}
