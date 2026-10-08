package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.EventoHistoricoActivo;
import com.infrati.backendinfrati.repository.ActivoRepository;
import com.infrati.backendinfrati.repository.HistoricoEstadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Service
public class HistorialActivoService {
    private final ActivoRepository activos;
    private final HistoricoEstadoRepository historico;

    public HistorialActivoService(ActivoRepository activos, HistoricoEstadoRepository historico) {
        this.activos = activos;
        this.historico = historico;
    }

    @Transactional(readOnly = true)
    public List<EventoHistoricoActivo> listar(Long activoId) {
        if (!activos.existsById(activoId)) {
            throw new RecursoNoEncontradoException("Activo " + activoId + " no encontrado");
        }
        return historico.findByActivo_IdOrderByFechaCambioDescIdDesc(activoId).stream()
                .map(evento -> new EventoHistoricoActivo(evento.getId(),
                        evento.getComponenteTipo(), evento.getComponenteSn(),
                        evento.getCampo(), evento.getDescripcion(), evento.getValorNuevo(),
                        evento.getFechaCambio() == null ? null
                                : evento.getFechaCambio().atOffset(ZoneOffset.UTC)))
                .toList();
    }
}
