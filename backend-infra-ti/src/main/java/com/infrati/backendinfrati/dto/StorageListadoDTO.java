package com.infrati.backendinfrati.dto;

import lombok.Builder;
import lombok.Data;

/** RF-023/HU-22: hostname, ubicacion, estado, proyecto relacionado, capacidad utilizada, protocolo. */
@Data
@Builder
public class StorageListadoDTO {
    private Long id;
    private String hostname;
    private String ubicacion;
    private String estadoOperativo;
    private String cluster;
    private Double capacidadUsadaTB;
    private String protocoloComunicacion;
}
