package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.dto.ServidorListadoDTO;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.repository.ServidorRepository;
import com.infrati.backendinfrati.repository.InventarioResumenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServidorService {

    private final ServidorRepository repository;
    private final DatosDerivadosActivo datosDerivados;
    private final InventarioResumenRepository resumen;

    public ServidorService(ServidorRepository repository, DatosDerivadosActivo datosDerivados, InventarioResumenRepository resumen) {
        this.resumen=resumen;
        this.repository = repository;
        this.datosDerivados = datosDerivados;
    }

    public List<ServidorListadoDTO> listar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        var equipos=repository.buscar(estado, ubicacion, proyecto, fabricante, q);
        var calculos=resumen.servidores(equipos.stream().map(Servidor::getId).toList());
        return equipos.stream().map(s->{
            var datos=calculos.get(s.getId());
            s.setCpuTotalGhz(redondear(datos.cpu()));s.setRamTotalGb(redondear(datos.ram()));
            s.setCpuUsoGhz(datos.usoCpu());s.setRamUsoGb(datos.usoRam());
            return aListadoDTO(s);
        }).toList();
    }

    public Servidor detalle(Long id) {
        return repository.buscarPorId(id)
                .map(this::completarTotalesYUso)
                .map(this::completarFechaEol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servidor " + id + " no encontrado"));
    }

    private Servidor completarFechaEol(Servidor s) {
        datosDerivados.resolverFechaEol(s);
        return s;
    }

    private Servidor completarTotalesYUso(Servidor s) {
        s.setCpuTotalGhz(TotalesComponentes.sumarCpuGhz(s.getCpus()));
        s.setRamTotalGb(TotalesComponentes.sumarRamGb(s.getMemoriaRAM()));
        s.setCpuUsoGhz(datosDerivados.ultimaMetrica(s.getId(), DatosDerivadosActivo.METRICA_CPU_USO_GHZ));
        s.setRamUsoGb(datosDerivados.ultimaMetrica(s.getId(), DatosDerivadosActivo.METRICA_RAM_USO_GB));
        s.setCapacidadDiscosGb(TotalesComponentes.sumarCapacidadDiscos(s.getDiscos()));
        return s;
    }

    private ServidorListadoDTO aListadoDTO(Servidor s) {
        return ServidorListadoDTO.builder()
                .id(s.getId())
                .numeroSerial(s.getNumero_serie())
                .tipo(s.getTipo())
                .fabricante(s.getFabricante())
                .modelo(s.getModelo())
                .hostname(s.getHostname())
                .ubicacion(s.getUbicacion())
                .estadoOperativo(s.getEstado_operativo() == null ? null : s.getEstado_operativo().name())
                .cluster(s.getCluster())
                .usoCpuPct(porcentaje(s.getCpuUsoGhz(), s.getCpuTotalGhz()))
                .usoRamPct(porcentaje(s.getRamUsoGb(), s.getRamTotalGb()))
                .build();
    }

    private static Double redondear(Double valor) { return valor==null?null:Math.round(valor*100.0)/100.0; }

    private static Double porcentaje(Double uso, Double total) {
        if (uso == null || total == null || total == 0) {
            return null;
        }
        return Math.round(100.0 * uso / total * 100.0) / 100.0;
    }
}
