package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.StorageListadoDTO;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.repository.StorageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageService {

    private final StorageRepository repository;

    public StorageService(StorageRepository repository) {
        this.repository = repository;
    }

    public List<StorageListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public Storage detalle(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Storage " + id + " no encontrado"));
    }

    private StorageListadoDTO aListadoDTO(Storage s) {
        return StorageListadoDTO.builder()
                .id(s.getId())
                .modelo(s.getModelo())
                .hostname(s.getHostname())
                .ubicacion(s.getUbicacion())
                .estadoOperativo(s.getEstado_operativo() == null ? null : s.getEstado_operativo().name())
                .cluster(s.getCluster())
                .capacidadUsadaTB(s.getCapacidad_usada_TB())
                .protocoloComunicacion(s.getProtocolo_comunicacion())
                .build();
    }
}
