package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.StorageListadoDTO;
import com.infrati.backendinfrati.model.Activos.Storage;
import com.infrati.backendinfrati.repository.StorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StorageService {

    private final StorageRepository repository;
    private final DatosDerivadosActivo datosDerivados;

    public StorageService(StorageRepository repository, DatosDerivadosActivo datosDerivados) {
        this.repository = repository;
        this.datosDerivados = datosDerivados;
    }

    public List<StorageListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public Storage detalle(Long id) {
        return repository.buscarPorId(id)
                .map(this::completarTotalesYUso)
                .map(this::completarFechaEol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Storage " + id + " no encontrado"));
    }

    private Storage completarFechaEol(Storage s) {
        datosDerivados.resolverFechaEol(s);
        return s;
    }

    private Storage completarTotalesYUso(Storage s) {
        s.setCpuTotalGhz(TotalesComponentes.sumarCpuGhz(s.getCpus()));
        s.setRamTotalGb(TotalesComponentes.sumarRamGb(s.getMemoriaRAM()));
        s.setCpuUsoGhz(datosDerivados.ultimaMetrica(s.getId(), DatosDerivadosActivo.METRICA_CPU_USO_GHZ));
        s.setRamUsoGb(datosDerivados.ultimaMetrica(s.getId(), DatosDerivadosActivo.METRICA_RAM_USO_GB));
        return s;
    }

    private StorageListadoDTO aListadoDTO(Storage s) {
        return StorageListadoDTO.builder()
                .id(s.getId())
                .numeroSerial(s.getNumero_serie())
                .fabricante(s.getFabricante())
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
