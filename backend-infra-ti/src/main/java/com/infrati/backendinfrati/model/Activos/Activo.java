package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.Enum.TipoActivoEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Activo {
    private Long id;
    private String numero_serie;
    private String hostname;
    private String fabricante;
    private String modelo;
    private String generacion;
    private String ubicacion;
    private String ip_gestion;
    private TipoActivoEnum tipo_activo;
    private EstadoEnum estado_operativo;
    private String responsable;
    private String orden_compra;
    private LocalDate fecha_eos;
    private String version_firmware;
    private LocalDate ultima_actualizacion;
    private String cluster;
    private Double temperatura;
    private Double consumo_electico_w;

}
