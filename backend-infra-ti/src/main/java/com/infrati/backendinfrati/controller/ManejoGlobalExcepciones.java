package com.infrati.backendinfrati.controller;

import com.infrati.backendinfrati.service.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ManejoGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejoGlobalExcepciones.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException excepcion,
            HttpServletRequest request) {
        log.info("Recurso no encontrado en {}: {}", request.getRequestURI(), excepcion.getMessage());
        return respuesta(HttpStatus.NOT_FOUND, excepcion.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> manejarFormatoInvalido(
            MethodArgumentTypeMismatchException excepcion,
            HttpServletRequest request) {
        log.warn("Parametro con formato invalido en {}: {}", request.getRequestURI(), excepcion.getName());
        return respuesta(HttpStatus.BAD_REQUEST, "El formato del parametro es invalido", request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<RespuestaError> manejarEndpointInexistente(
            Exception excepcion,
            HttpServletRequest request) {
        log.info("Endpoint no encontrado: {}", request.getRequestURI());
        return respuesta(HttpStatus.NOT_FOUND, "Endpoint no encontrado", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarErrorInterno(
            Exception excepcion,
            HttpServletRequest request) {
        log.error("Error interno inesperado al procesar {}", request.getRequestURI(), excepcion);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno inesperado", request);
    }

    private ResponseEntity<RespuestaError> respuesta(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest request) {
        RespuestaError cuerpo = new RespuestaError(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                request.getRequestURI());
        return ResponseEntity.status(estado).body(cuerpo);
    }

    public record RespuestaError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
