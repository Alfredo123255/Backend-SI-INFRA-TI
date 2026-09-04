package com.infrati.backendinfrati.dto;

import com.infrati.backendinfrati.model.Enum.TipoServidorEnum;
import lombok.Builder;
import lombok.Data;

/** RF-014: solo lo que la vista inicial del listado necesita, sin las listas de componentes. */
@Data
@Builder
public class ServidorListadoDTO {
    private Long id;
    private String numeroSerial;
    private TipoServidorEnum tipo;
    private String fabricante;
    private String modelo;
    private String hostname;
    private String ubicacion;
    private String estadoOperativo;
    private String cluster;
    private Double usoCpuPct;
    private Double usoRamPct;
}
