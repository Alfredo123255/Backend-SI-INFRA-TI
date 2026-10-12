package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.AlertaActivo;
import com.infrati.backendinfrati.repository.AlertaActivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaActivoService {
    private final AlertaActivoRepository repository;

    public AlertaActivoService(AlertaActivoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AlertaActivo> listar() {
        return repository.listarActuales();
    }
}
