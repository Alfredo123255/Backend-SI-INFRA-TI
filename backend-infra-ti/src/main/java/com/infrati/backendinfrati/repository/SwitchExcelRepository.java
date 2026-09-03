package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Componentes.Cpu;
import com.infrati.backendinfrati.model.Componentes.FuentePoder;
import com.infrati.backendinfrati.model.Componentes.Ram;
import com.infrati.backendinfrati.model.Componentes.Ventilador;
import com.infrati.backendinfrati.model.Componentes.PuertoSwitch;
import com.infrati.backendinfrati.model.Activos.Switch;
import com.infrati.backendinfrati.model.Enum.TipoActivoEnum;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.infrati.backendinfrati.repository.ExcelUtils.getInteger;
import static com.infrati.backendinfrati.repository.ExcelUtils.getString;

/**
 * Mismo patron: une ActivosTI (tipo_activo = SWITCH) + Switches (exclusivas)
 * + PuertoSwitch. Sin columnas de cpu/ram propias: salen de las hojas
 * compartidas, igual que Servidor/Storage.
 */
@Repository
public class SwitchExcelRepository implements SwitchRepository {

    @Value("classpath:data/inventario.xlsx")
    private Resource archivo;

    private List<Switch> cache = new ArrayList<>();
    private Map<Long, Switch> cachePorId = new LinkedHashMap<>();

    @PostConstruct
    void cargar() throws Exception {
        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {

            var comunPorId = ServidorExcelRepository.cargarActivosTI(wb.getSheet("ActivosTI"), EnumSet.of(TipoActivoEnum.SWITCH));
            Map<Long, List<Cpu>> cpuPorActivo = ServidorExcelRepository.cargarCpu(wb.getSheet("Cpu"));
            Map<Long, List<Ram>> ramPorActivo = ServidorExcelRepository.cargarRam(wb.getSheet("Ram"));
            Map<Long, List<PuertoSwitch>> puertosPorSwitch = cargarPuertos(wb.getSheet("PuertoSwitch"));
            Map<Long, List<FuentePoder>> fuentesPorActivo = ServidorExcelRepository.cargarFuentesPoder(wb.getSheet("FuentesPoder"));
            Map<Long, List<Ventilador>> ventiladoresPorActivo = ServidorExcelRepository.cargarVentiladores(wb.getSheet("Ventiladores"));

            Sheet hoja = wb.getSheet("Switches");
            for (Row fila : hoja) {
                if (fila.getRowNum() == 0 || getInteger(fila, 0) == null) {
                    continue;
                }
                Long id = getInteger(fila, 0).longValue();
                var comun = comunPorId.get(id);
                if (comun == null) {
                    continue;
                }
                List<Cpu> cpus = cpuPorActivo.getOrDefault(id, List.of());
                List<Ram> rams = ramPorActivo.getOrDefault(id, List.of());

                Switch equipo = Switch.builder()
                        .id(id)
                        .numero_serie(comun.numeroSerie())
                        .hostname(comun.hostname())
                        .fabricante(comun.fabricante())
                        .modelo(comun.modelo())
                        .generacion(comun.generacion())
                        .ubicacion(comun.ubicacion())
                        .ip_gestion(comun.ipGestion())
                        .tipo_activo(comun.tipoActivo())
                        .estado_operativo(comun.estadoOperativo())
                        .cluster(comun.cluster())
                        .responsable(comun.responsable())
                        .orden_compra(comun.ordenCompra())
                        .fecha_eos(comun.fechaEos())
                        .version_firmware(comun.versionFirmware())
                        .ultima_actualizacion(comun.ultimaActualizacion())
                        .temperatura(comun.temperatura())
                        .consumo_electico_w(comun.consumoElectricoW())
                        .tipoRED(getString(fila, 1))
                        .modo_operacion(getString(fila, 2))
                        .cantidad_puertos(getInteger(fila, 3) == null ? 0 : getInteger(fila, 3))
                        .cantidad_puertos_ocupados(getInteger(fila, 4) == null ? 0 : getInteger(fila, 4))
                        .puertos(puertosPorSwitch.getOrDefault(id, List.of()))
                        .cpuTotalGhz(ServidorExcelRepository.sumarCpuGhz(cpus))
                        .ramTotalGb(ServidorExcelRepository.sumarRamGb(rams))
                        .cpus(cpus)
                        .memoriaRAM(rams)
                        .fuentesEnergia(fuentesPorActivo.getOrDefault(id, List.of()))
                        .ventiladores(ventiladoresPorActivo.getOrDefault(id, List.of()))
                        .build();

                cache.add(equipo);
                cachePorId.put(id, equipo);
            }
        }
    }

    private Map<Long, List<PuertoSwitch>> cargarPuertos(Sheet hoja) {
        Map<Long, List<PuertoSwitch>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long switchId = getInteger(fila, 1).longValue();
            PuertoSwitch puerto = PuertoSwitch.builder()
                    .id(getString(fila, 0))
                    .numero_puerto(getString(fila, 2))
                    .velocidad(getString(fila, 3))
                    .estado(ServidorExcelRepository.parseEstado(getString(fila, 6)))
                    .build();
            resultado.computeIfAbsent(switchId, k -> new ArrayList<>()).add(puerto);
        }
        return resultado;
    }

    @Override
    public List<Switch> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        String texto = q == null ? null : q.toLowerCase();
        return cache.stream()
                .filter(s -> estado == null || (s.getEstado_operativo() != null && estado.equalsIgnoreCase(s.getEstado_operativo().name())))
                .filter(s -> ubicacion == null || ubicacion.equalsIgnoreCase(s.getUbicacion()))
                .filter(s -> proyecto == null || proyecto.equalsIgnoreCase(s.getCluster()))
                .filter(s -> fabricante == null || fabricante.equalsIgnoreCase(s.getFabricante()))
                .filter(s -> texto == null || coincide(s, texto))
                .toList();
    }

    private boolean coincide(Switch equipo, String texto) {
        return contiene(equipo.getHostname(), texto) || contiene(equipo.getIp_gestion(), texto);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    @Override
    public Optional<Switch> buscarPorId(Long id) {
        return Optional.ofNullable(cachePorId.get(id));
    }
}
