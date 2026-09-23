package com.infrati.backendinfrati.model.Activos;

import com.infrati.backendinfrati.model.Componentes.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "chasis_blade")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper= true)
@SuperBuilder
@NoArgsConstructor
public class ChasisBlade extends Activo{
    @Column(name = "cantidad_slots")
    private int cantidad_slots;

    @OneToMany(mappedBy = "chasisBlade", cascade = CascadeType.ALL)
    private List<ChasisSlot> chasisSlots;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<Ventilador> ventiladores;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<TarjetaRed> tarjetasRED;

    @OneToMany(mappedBy = "activo", cascade = CascadeType.ALL)
    private List<FuentePoder> fuentesEnergia;
}
