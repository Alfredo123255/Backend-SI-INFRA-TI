package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.AlertaActivo;
import com.infrati.backendinfrati.exception.SolicitudInvalidaException;
import com.infrati.backendinfrati.repository.AlertaActivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service
public class AlertaActivoService {
    private final AlertaActivoRepository repository;
    public AlertaActivoService(AlertaActivoRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<AlertaActivo> listar() { return listar(null,null,null); }

    @Transactional(readOnly = true)
    public List<AlertaActivo> listar(Long activoId,String severidad,String categoria) {
        if (activoId!=null && activoId<=0) throw new SolicitudInvalidaException("activoId debe ser positivo.");
        if (severidad!=null && !Set.of("Media","Crítica").contains(severidad))
            throw new SolicitudInvalidaException("La severidad debe ser Media o Crítica.");
        if (categoria!=null && !Set.of("eventos","metricas","eos").contains(categoria))
            throw new SolicitudInvalidaException("La categoría debe ser eventos, metricas o eos.");
        return repository.listarActuales().stream()
                .filter(a -> activoId==null || activoId.equals(a.activoId()))
                .filter(a -> severidad==null || severidad.equals(a.severidad()))
                .filter(a -> categoria==null || categoria.equals(categoria(a.campo())))
                .toList();
    }
    private static String categoria(String campo) {
        return "umbral_dias_para_eos".equals(campo)?"eos":campo.startsWith("umbral_")?"metricas":"eventos";
    }
}
