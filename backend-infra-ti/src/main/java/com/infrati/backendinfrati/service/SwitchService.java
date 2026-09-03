package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.SwitchListadoDTO;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.repository.SwitchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SwitchService {

    private final SwitchRepository repository;

    public SwitchService(SwitchRepository repository) {
        this.repository = repository;
    }

    public List<SwitchListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public Switch detalle(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Switch " + id + " no encontrado"));
    }

    public List<Switch> buscarParaExportar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q);
    }

    private SwitchListadoDTO aListadoDTO(Switch equipo) {
        return SwitchListadoDTO.builder()
                .id(equipo.getId())
                .hostname(equipo.getHostname())
                .ubicacion(equipo.getUbicacion())
                .estadoOperativo(equipo.getEstado_operativo() == null ? null : equipo.getEstado_operativo().name())
                .tipoRed(equipo.getTipoRED())
                .cantidadPuertosOcupados(equipo.getCantidad_puertos_ocupados())
                .cantidadPuertosTotales(equipo.getCantidad_puertos())
                .ipGestion(equipo.getIp_gestion())
                .build();
    }
}
