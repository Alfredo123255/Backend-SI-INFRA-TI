package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.AlertaActivo;
import com.infrati.backendinfrati.repository.AlertaActivoRepository;
import com.infrati.backendinfrati.repository.EvaluacionUmbralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AlertaActivoService {
    private final AlertaActivoRepository repository;
    private final EvaluacionUmbralRepository evaluacion;

    public AlertaActivoService(AlertaActivoRepository repository, EvaluacionUmbralRepository evaluacion) {
        this.repository = repository;
        this.evaluacion = evaluacion;
    }

    @Transactional
    public List<AlertaActivo> listar() {
        var alertasUmbral = evaluacion.evaluarYRegistrar();
        return Stream.concat(repository.listarActuales().stream(), alertasUmbral.stream())
                .sorted(Comparator.comparing(AlertaActivo::fechaCambio,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
