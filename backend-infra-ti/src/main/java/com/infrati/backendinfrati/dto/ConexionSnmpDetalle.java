package com.infrati.backendinfrati.dto;

import java.time.LocalDateTime;

/** Datos editables y estado de la conexión; nunca incluye las claves SNMP. */
public record ConexionSnmpDetalle(Long id, Long activoId, String ipGestion, String usuario,
        Integer frecuenciaActualizacion, String estadoConexion,
        LocalDateTime fechaUltimaActualizacion) {
}
