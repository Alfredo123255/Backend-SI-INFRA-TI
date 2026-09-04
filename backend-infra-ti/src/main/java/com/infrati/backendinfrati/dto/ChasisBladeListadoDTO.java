package com.infrati.backendinfrati.dto;

import lombok.Builder;
import lombok.Data;

/** Solo lo que la vista inicial del listado de chasis-blades necesita, sin las listas de componentes ni slots. */
@Data
@Builder
public class ChasisBladeListadoDTO {
    private Long id;
    private String hostname;
    private String cluster;
    private String ubicacion;
    private String modelo;
    private String ipGestion;
    private String estadoOperativo;
}
