package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ServidorListadoDTO;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.repository.ServidorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServidorService {

    private final ServidorRepository repository;

    public ServidorService(ServidorRepository repository) {
        this.repository = repository;
    }

    public List<ServidorListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public Servidor detalle(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servidor " + id + " no encontrado"));
    }

    private ServidorListadoDTO aListadoDTO(Servidor s) {
        return ServidorListadoDTO.builder()
                .id(s.getId())
                .tipo(s.getTipo())
                .modelo(s.getModelo())
                .hostname(s.getHostname())
                .ubicacion(s.getUbicacion())
                .estadoOperativo(s.getEstado_operativo() == null ? null : s.getEstado_operativo().name())
                .cluster(s.getCluster())
                .usoCpuPct(porcentaje(s.getCpuUsoGhz(), s.getCpuTotalGhz()))
                .usoRamPct(porcentaje(s.getRamUsoGb(), s.getRamTotalGb()))
                .build();
    }

    private static Double porcentaje(Double uso, Double total) {
        if (uso == null || total == null || total == 0) {
            return null;
        }
        return Math.round(100.0 * uso / total * 100.0) / 100.0;
    }
}
