package com.infrati.backendinfrati.model.Componentes;

import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaRed {
    private Long id;
    private String numero_serial;
    private String marca;
    private String modelo;
    private EstadoEnum estado;
    private int cantidad_puertos;
    private List<PuertoTarjetaRed> puertos;
}
