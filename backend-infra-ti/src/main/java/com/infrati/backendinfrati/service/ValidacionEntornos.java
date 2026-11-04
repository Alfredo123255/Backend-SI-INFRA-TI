package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import java.math.BigDecimal;

final class ValidacionEntornos {
    private ValidacionEntornos() { }

    static String requerido(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo
                || valor.chars().anyMatch(Character::isISOControl)) {
            throw new SolicitudInvalidaException("El campo " + campo + " es obligatorio y debe ser válido.");
        }
        return valor.trim();
    }

    static String coordenada(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) return null;
        String texto = valor.trim();
        try {
            if (texto.length() <= 255 && new BigDecimal(texto).abs().compareTo(BigDecimal.valueOf(maximo)) <= 0) {
                return texto;
            }
        } catch (NumberFormatException ignored) { }
        throw new SolicitudInvalidaException("La " + campo + " debe ser un número entre -" + maximo + " y " + maximo + ".");
    }
}
