package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.SwitchListadoDTO;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.model.Componentes.PuertoSwitch;
import com.infrati.backendinfrati.repository.SwitchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SwitchService {

    private final SwitchRepository repository;
    private final DatosDerivadosActivo datosDerivados;

    public SwitchService(SwitchRepository repository, DatosDerivadosActivo datosDerivados) {
        this.repository = repository;
        this.datosDerivados = datosDerivados;
    }

    public List<SwitchListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return repository.buscar(estado, ubicacion, proyecto, fabricante, q).stream()
                .map(this::aListadoDTO)
                .toList();
    }

    public Switch detalle(Long id) {
        return repository.buscarPorId(id)
                .map(this::completarTotalesYUso)
                .map(this::completarFechaEol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Switch " + id + " no encontrado"));
    }

    private Switch completarFechaEol(Switch equipo) {
        datosDerivados.resolverFechaEol(equipo);
        return equipo;
    }

    private Switch completarTotalesYUso(Switch equipo) {
        equipo.setCpuTotalGhz(TotalesComponentes.sumarCpuGhz(equipo.getCpus()));
        equipo.setRamTotalGb(TotalesComponentes.sumarRamGb(equipo.getMemoriaRAM()));
        equipo.setCpuUsoGhz(datosDerivados.ultimaMetrica(equipo.getId(), DatosDerivadosActivo.METRICA_CPU_USO_GHZ));
        equipo.setRamUsoGb(datosDerivados.ultimaMetrica(equipo.getId(), DatosDerivadosActivo.METRICA_RAM_USO_GB));
        return equipo;
    }

    private SwitchListadoDTO aListadoDTO(Switch equipo) {
        return SwitchListadoDTO.builder()
                .id(equipo.getId())
                .numeroSerial(equipo.getNumero_serie())
                .fabricante(equipo.getFabricante())
                .modelo(equipo.getModelo())
                .hostname(equipo.getHostname())
                .ubicacion(equipo.getUbicacion())
                .estadoOperativo(equipo.getEstado_operativo() == null ? null : equipo.getEstado_operativo().name())
                .tipoRed(equipo.getTipoRED())
                .velocidad(velocidadesPuertos(equipo))
                .cantidadPuertosOcupados(equipo.getCantidad_puertos_ocupados())
                .cantidadPuertosTotales(equipo.getCantidad_puertos())
                .ipGestion(equipo.getIp_gestion())
                .build();
    }

    /**
     * RF-024 pide "la velocidad de los puertos" como texto, no un
     * numero unico - un switch puede tener puertos a distintas
     * velocidades a la vez (uplinks a 10G, acceso a 1G). Se listan los
     * numeros de todas las velocidades distintas presentes, separados
     * por " / ", con la unidad (Gbps) una sola vez al final -
     * ej. "10 / 50 Gbps" en vez de "10 Gbps / 50 Gbps".
     */
    private String velocidadesPuertos(Switch s) {
        if (s.getPuertos() == null || s.getPuertos().isEmpty()) {
            return null;
        }
        List<String> valores = s.getPuertos().stream()
                .map(PuertoSwitch::getVelocidad)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .toList();
        if (valores.isEmpty()) {
            return null;
        }
        String unidad = valores.get(0).replaceAll("[0-9.]+\\s*", "").trim();
        String numeros = valores.stream()
                .map(v -> v.replaceAll("[^0-9.]", ""))
                .collect(java.util.stream.Collectors.joining(" / "));
        return unidad.isEmpty() ? numeros : numeros + " " + unidad;
    }
}
