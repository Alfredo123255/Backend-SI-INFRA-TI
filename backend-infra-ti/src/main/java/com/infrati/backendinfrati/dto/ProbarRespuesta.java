package com.infrati.backendinfrati.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProbarRespuesta(boolean ok, String descripcion, String sysObjectId, String sysName,
        String fabricante, String tipoDetectado, Integer milisegundos, String errorTipo, String mensaje) {
}
