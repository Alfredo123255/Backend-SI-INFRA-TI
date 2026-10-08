package com.infrati.backendinfrati.dto;

/** Una clave nula o vacía conserva la actual. */
public record ActualizarConexionSnmpRequest(String ipGestion, String usuario, String clave,
        Integer frecuenciaActualizacion) {
}
