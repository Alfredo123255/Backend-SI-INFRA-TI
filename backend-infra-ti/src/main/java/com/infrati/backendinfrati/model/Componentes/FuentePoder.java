package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.CorrienteEnum;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;

public class FuentePoder {
    private Long id;
    private String numero_serial;
    private String modelo;
    private EstadoEnum estado;
    private Double consumo_w;
    private CorrienteEnum tipo_corriente;
}
