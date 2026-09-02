package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.CorrienteEnum;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class FuentePoder {
    private Long id;
    private String numero_serial;
    private String modelo;
    private EstadoEnum estado;
    private Double consumo_w;
    private CorrienteEnum tipo_corriente;
}
