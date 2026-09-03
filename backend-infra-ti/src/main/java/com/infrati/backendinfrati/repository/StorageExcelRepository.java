package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Componentes.ControladoraRAID;
import com.infrati.backendinfrati.model.Componentes.Cpu;
import com.infrati.backendinfrati.model.Componentes.Disco;
import com.infrati.backendinfrati.model.Componentes.FuentePoder;
import com.infrati.backendinfrati.model.Componentes.Ram;
import com.infrati.backendinfrati.model.Componentes.TarjetaRed;
import com.infrati.backendinfrati.model.Componentes.Ventilador;
import com.infrati.backendinfrati.model.Activos.Storage;
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

import static com.infrati.backendinfrati.repository.ExcelUtils.getDouble;
import static com.infrati.backendinfrati.repository.ExcelUtils.getInteger;
import static com.infrati.backendinfrati.repository.ExcelUtils.getString;

/**
 * Mismo patron que ServidorExcelRepository: une ActivosTI (tipo_activo =
 * STORAGE) con Storage (exclusivas). Reutiliza los loaders compartidos de
 * ServidorExcelRepository, incluida cargarControladorasRaid.
 */
@Repository
public class StorageExcelRepository implements StorageRepository {

    @Value("classpath:data/inventario.xlsx")
    private Resource archivo;

    private List<Storage> cache = new ArrayList<>();
    private Map<Long, Storage> cachePorId = new LinkedHashMap<>();

    @PostConstruct
    void cargar() throws Exception {
        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {

            var comunPorId = ServidorExcelRepository.cargarActivosTI(wb.getSheet("ActivosTI"), EnumSet.of(TipoActivoEnum.STORAGE));
            Map<Long, List<Cpu>> cpuPorActivo = ServidorExcelRepository.cargarCpu(wb.getSheet("Cpu"));
            Map<Long, List<Ram>> ramPorActivo = ServidorExcelRepository.cargarRam(wb.getSheet("Ram"));
            Map<Long, List<Disco>> discosPorActivo = ServidorExcelRepository.cargarDiscos(wb.getSheet("Discos"));
            Map<Long, List<TarjetaRed>> redPorActivo = ServidorExcelRepository.cargarTarjetasRed(wb.getSheet("TarjetasRed"), wb.getSheet("PuertoTarjetaRed"));
            Map<Long, List<ControladoraRAID>> raidPorActivo = ServidorExcelRepository.cargarControladorasRaid(wb.getSheet("ControladoraRaid"));
            Map<Long, List<FuentePoder>> fuentesPorActivo = ServidorExcelRepository.cargarFuentesPoder(wb.getSheet("FuentesPoder"));
            Map<Long, List<Ventilador>> ventiladoresPorActivo = ServidorExcelRepository.cargarVentiladores(wb.getSheet("Ventiladores"));

            Sheet hoja = wb.getSheet("Storage");
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

                Storage storage = Storage.builder()
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
                        .protocolo_comunicacion(getString(fila, 1))
                        .capacidad_total_TB(getDouble(fila, 2))
                        .capacidad_usada_TB(getDouble(fila, 3))
                        .iops(getInteger(fila, 4) == null ? 0 : getInteger(fila, 4))
                        .cpuUsoGhz(getDouble(fila, 5))
                        .ramUsoGb(getDouble(fila, 6))
                        .cpuTotalGhz(ServidorExcelRepository.sumarCpuGhz(cpus))
                        .ramTotalGb(ServidorExcelRepository.sumarRamGb(rams))
                        .cpus(cpus)
                        .memoriaRAM(rams)
                        .discos(discosPorActivo.getOrDefault(id, List.of()))
                        .tarjetasRED(redPorActivo.getOrDefault(id, List.of()))
                        .controladorasRAID(raidPorActivo.getOrDefault(id, List.of()))
                        .fuentesEnergia(fuentesPorActivo.getOrDefault(id, List.of()))
                        .ventiladores(ventiladoresPorActivo.getOrDefault(id, List.of()))
                        .build();

                cache.add(storage);
                cachePorId.put(id, storage);
            }
        }
    }

    @Override
    public List<Storage> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        String texto = q == null ? null : q.toLowerCase();
        return cache.stream()
                .filter(s -> estado == null || (s.getEstado_operativo() != null && estado.equalsIgnoreCase(s.getEstado_operativo().name())))
                .filter(s -> ubicacion == null || ubicacion.equalsIgnoreCase(s.getUbicacion()))
                .filter(s -> proyecto == null || proyecto.equalsIgnoreCase(s.getCluster()))
                .filter(s -> fabricante == null || fabricante.equalsIgnoreCase(s.getFabricante()))
                .filter(s -> texto == null || coincide(s, texto))
                .toList();
    }

    private boolean coincide(Storage s, String texto) {
        return contiene(s.getHostname(), texto) || contiene(s.getCluster(), texto);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    @Override
    public Optional<Storage> buscarPorId(Long id) {
        return Optional.ofNullable(cachePorId.get(id));
    }
}
