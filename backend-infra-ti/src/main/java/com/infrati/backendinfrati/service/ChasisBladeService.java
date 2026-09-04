package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ChasisBladeListadoDTO;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.repository.ChasisBladeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChasisBladeService {

    private final ChasisBladeRepository repository;

    public ChasisBladeService(ChasisBladeRepository repository) {
        this.repository = repository;
    }

    public List<ChasisBladeListadoDTO> listar(String estado, String ubicacion, String cluster, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, cluster, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public ChasisBlade obtenerDetalle(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("ChasisBlade " + id + " no encontrado"));
    }

    private ChasisBladeListadoDTO aListadoDTO(ChasisBlade c) {
        return ChasisBladeListadoDTO.builder()
                .id(c.getId())
                .hostname(c.getHostname())
                .cluster(c.getCluster())
                .ubicacion(c.getUbicacion())
                .modelo(c.getModelo())
                .ipGestion(c.getIp_gestion())
                .estadoOperativo(c.getEstado_operativo() == null ? null : c.getEstado_operativo().name())
                .build();
    }
}
