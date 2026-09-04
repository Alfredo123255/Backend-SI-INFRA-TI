package com.infrati.backendinfrati.dto;

import lombok.Builder;
import lombok.Data;

/** RF-024/HU-23: hostname, ubicacion, estado, tipo de red, cantidad de puertos utilizados, ip de gestion. */
@Data
@Builder
public class SwitchListadoDTO {
    private Long id;
    private String modelo;
    private String hostname;
    private String ubicacion;
    private String estadoOperativo;
    private String tipoRed;
    private String velocidad;
    private Integer cantidadPuertosOcupados;
    private Integer cantidadPuertosTotales;
    private String ipGestion;
}
