package com.infrati.backendinfrati.exception;

import org.springframework.http.HttpStatus;

public class EtlException extends RuntimeException {
    private final HttpStatus estado;

    public EtlException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
