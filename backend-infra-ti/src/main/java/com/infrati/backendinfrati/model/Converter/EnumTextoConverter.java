package com.infrati.backendinfrati.model.Converter;

import jakarta.persistence.AttributeConverter;

/**
 * La BD guarda algunos enums en MAYUSCULAS (BLADE, OCUPADO) mientras que las
 * constantes Java tienen mayuscula inicial (Blade, Ocupado). Se lee sin
 * distinguir mayusculas y se escribe en MAYUSCULAS, como esta en la BD.
 */
public abstract class EnumTextoConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> tipo;

    protected EnumTextoConverter(Class<E> tipo) {
        this.tipo = tipo;
    }

    @Override
    public String convertToDatabaseColumn(E valor) {
        return valor == null ? null : valor.name().toUpperCase();
    }

    @Override
    public E convertToEntityAttribute(String texto) {
        if (texto == null) {
            return null;
        }
        for (E e : tipo.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(texto)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Valor " + texto + " no valido para " + tipo.getSimpleName());
    }
}
