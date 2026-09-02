package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoSlotEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChasisSlot {
    private Long id;
    private Long chasisID;
    private Integer numeroSlot;
    private EstadoSlotEnum estado;
    private String hostanameServidor;
}
