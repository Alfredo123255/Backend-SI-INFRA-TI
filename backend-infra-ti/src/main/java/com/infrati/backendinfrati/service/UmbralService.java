package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ActualizarUmbralRequest;
import com.infrati.backendinfrati.dto.UmbralRespuesta;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.model.Umbral;
import com.infrati.backendinfrati.repository.UmbralRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
public class UmbralService {
    private static final Set<String> PORCENTAJES = Set.of("cpu_uso_ghz", "ram_uso_gb",
            "disco_uso_gb", "almacenamiento_uso_tb", "red_utilizacion_pct");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private final UmbralRepository repository;

    public UmbralService(UmbralRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UmbralRespuesta> listar() {
        return repository.findAll(Sort.by("nombre")).stream().map(UmbralService::respuesta).toList();
    }

    @Transactional
    public UmbralRespuesta actualizar(String nombre, ActualizarUmbralRequest solicitud) {
        Umbral umbral = repository.findById(nombre).orElseThrow(
                () -> new RecursoNoEncontradoException("Umbral " + nombre + " no encontrado."));
        if ("red_ancho_banda_mbps".equals(nombre)) {
            throw new SolicitudInvalidaException("El ancho de banda no admite umbrales.");
        }
        if (solicitud == null) throw new SolicitudInvalidaException("Debe enviar los límites del umbral.");

        BigDecimal advertencia = solicitud.advertencia();
        BigDecimal critico = solicitud.critico();
        if ("dias_para_eos".equals(nombre)) {
            if (advertencia == null || advertencia.signum() < 0 || advertencia.stripTrailingZeros().scale() > 0) {
                throw new SolicitudInvalidaException("Los días de aviso deben ser un entero mayor o igual a cero.");
            }
            if (critico != null) throw new SolicitudInvalidaException("El límite crítico de EOS es fijo.");
        } else {
            validarMetrica(nombre, advertencia, critico);
        }
        umbral.setAdvertencia(advertencia);
        umbral.setCritico(critico);
        umbral.setFechaModificacion(LocalDateTime.now(ZoneOffset.UTC));
        return respuesta(umbral);
    }

    private static void validarMetrica(String nombre, BigDecimal advertencia, BigDecimal critico) {
        if (advertencia == null && critico == null) return;
        if (advertencia == null || critico == null) {
            throw new SolicitudInvalidaException("Indica ambos límites o deja ambos vacíos.");
        }
        if (advertencia.signum() < 0 || critico.signum() < 0) {
            throw new SolicitudInvalidaException("Los valores deben ser mayores o iguales a cero.");
        }
        if (critico.compareTo(advertencia) < 0) {
            throw new SolicitudInvalidaException("El valor crítico debe ser mayor o igual al de advertencia.");
        }
        if (PORCENTAJES.contains(nombre) && critico.compareTo(CIEN) > 0) {
            throw new SolicitudInvalidaException("Los porcentajes no pueden superar 100 %.");
        }
    }

    private static UmbralRespuesta respuesta(Umbral umbral) {
        return new UmbralRespuesta(umbral.getNombre(), umbral.getAdvertencia(), umbral.getCritico(),
                umbral.getFechaModificacion());
    }
}
