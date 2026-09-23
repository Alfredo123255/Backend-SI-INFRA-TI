package com.infrati.backendinfrati.model.Converter;

import com.infrati.backendinfrati.model.Enum.TipoServidorEnum;
import jakarta.persistence.Converter;

@Converter
public class TipoServidorConverter extends EnumTextoConverter<TipoServidorEnum> {
    public TipoServidorConverter() {
        super(TipoServidorEnum.class);
    }
}
