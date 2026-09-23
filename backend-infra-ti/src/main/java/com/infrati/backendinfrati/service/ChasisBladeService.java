package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ChasisBladeListadoDTO;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Componentes.ChasisSlot;
import com.infrati.backendinfrati.repository.ChasisBladeRepository;
import com.infrati.backendinfrati.repository.ServidorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChasisBladeService {

    private final ChasisBladeRepository repository;
    private final ServidorRepository servidorRepository;
    private final DatosDerivadosActivo datosDerivados;

    public ChasisBladeService(ChasisBladeRepository repository, ServidorRepository servidorRepository,
                              DatosDerivadosActivo datosDerivados) {
        this.repository = repository;
        this.servidorRepository = servidorRepository;
        this.datosDerivados = datosDerivados;
    }

    public List<ChasisBladeListadoDTO> listar(String estado, String ubicacion, String cluster, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, cluster, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public ChasisBlade obtenerDetalle(Long id) {
        return repository.buscarPorId(id)
                .map(this::completarFechaEol)
                .map(this::completarServidoresEnSlots)
                .orElseThrow(() -> new RecursoNoEncontradoException("ChasisBlade " + id + " no encontrado"));
    }

    private ChasisBlade completarFechaEol(ChasisBlade c) {
        datosDerivados.resolverFechaEol(c);
        return c;
    }

    /**
     * chasis_slot.servidor_id no se llena: el vinculo real vive en
     * servidor.id_chasis_slot, asi que se resuelve por busqueda inversa.
     */
    private ChasisBlade completarServidoresEnSlots(ChasisBlade c) {
        if (c.getChasisSlots() == null || c.getChasisSlots().isEmpty()) {
            return c;
        }
        List<Long> idsSlot = c.getChasisSlots().stream().map(ChasisSlot::getId).toList();
        Map<Long, Long> servidorPorSlot = servidorRepository.findByIdChasisSlotIn(idsSlot).stream()
                .collect(Collectors.toMap(Servidor::getIdChasisSlot, Servidor::getId, (a, b) -> a));
        for (ChasisSlot slot : c.getChasisSlots()) {
            Long servidorId = servidorPorSlot.get(slot.getId());
            if (slot.getServidorId() == null && servidorId != null) {
                slot.setServidorId(servidorId.intValue());
            }
        }
        return c;
    }

    private ChasisBladeListadoDTO aListadoDTO(ChasisBlade c) {
        return ChasisBladeListadoDTO.builder()
                .id(c.getId())
                .numeroSerial(c.getNumero_serie())
                .fabricante(c.getFabricante())
                .hostname(c.getHostname())
                .cluster(c.getCluster())
                .ubicacion(c.getUbicacion())
                .modelo(c.getModelo())
                .ipGestion(c.getIp_gestion())
                .estadoOperativo(c.getEstado_operativo() == null ? null : c.getEstado_operativo().name())
                .build();
    }
}
