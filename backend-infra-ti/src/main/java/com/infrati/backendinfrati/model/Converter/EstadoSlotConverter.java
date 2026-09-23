package com.infrati.backendinfrati.model.Converter;

import com.infrati.backendinfrati.model.Enum.EstadoSlotEnum;
import jakarta.persistence.Converter;

@Converter
public class EstadoSlotConverter extends EnumTextoConverter<EstadoSlotEnum> {
    public EstadoSlotConverter() {
        super(EstadoSlotEnum.class);
    }
}
