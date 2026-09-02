package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PuertoSwitch {
    private String id;
    private String numero_puerto;
    private String velocidad;
    private EstadoEnum estado;
}
