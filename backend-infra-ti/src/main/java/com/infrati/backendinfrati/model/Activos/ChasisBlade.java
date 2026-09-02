package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor
public class ChasisBlade extends Activo{
    private int cantidad_slots;
    private List<ChasisSlot> chasisSlots;
    private List<Ventilador> ventiladores;
    private List<TarjetaRed> tarjetasRED;
    private List<FuentePoder> fuentesEnergia;
}
