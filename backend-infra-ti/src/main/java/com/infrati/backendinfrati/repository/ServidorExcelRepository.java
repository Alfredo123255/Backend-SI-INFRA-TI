package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Componentes.ControladoraRAID;
import com.infrati.backendinfrati.model.Componentes.Cpu;
import com.infrati.backendinfrati.model.Componentes.Disco;
import com.infrati.backendinfrati.model.Componentes.FuentePoder;
import com.infrati.backendinfrati.model.Componentes.PuertoTarjetaRed;
import com.infrati.backendinfrati.model.Componentes.Ram;
import com.infrati.backendinfrati.model.Componentes.TarjetaRed;
import com.infrati.backendinfrati.model.Componentes.Ventilador;
import com.infrati.backendinfrati.model.Activos.Servidor;
import com.infrati.backendinfrati.model.Enum.CorrienteEnum;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import com.infrati.backendinfrati.model.Enum.TipoActivoEnum;
import com.infrati.backendinfrati.model.Enum.TipoServidorEnum;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.infrati.backendinfrati.repository.ExcelUtils.getDouble;
import static com.infrati.backendinfrati.repository.ExcelUtils.getInteger;
import static com.infrati.backendinfrati.repository.ExcelUtils.getLocalDate;
import static com.infrati.backendinfrati.repository.ExcelUtils.getString;

/**
 * Arma cada Servidor uniendo "ActivosTI" (compartida, filtrada por tipo_activo
 * = SERVIDOR) con "Servidores" (exclusivas: ip_sistema_operativo, version_so,
 * idChasisSlot, tipo rackeable/blade). Cpu/Ram/Discos/TarjetasRed/
 * FuentesPoder/Ventiladores/ControladorasRAID vienen de hojas compartidas
 * via activo_id, mismo espacio de id que Storage/Switch. cpuTotalGhz y
 * ramTotalGb se calculan aca sumando los componentes; ya no existe un
 * porcentaje de uso por componente en el modelo actual, asi que
 * cpuUsoGhz/ramUsoGb no se pueblan desde el Excel.
 *
 * Todos los metodos cargarXxx son package-private/static: Storage y Switch
 * los reutilizan para no duplicar la logica de lectura.
 */
@Repository
public class ServidorExcelRepository implements ServidorRepository {

    @Value("classpath:data/inventario.xlsx")
    private Resource archivo;

    private List<Servidor> cache = new ArrayList<>();
    private Map<Long, Servidor> cachePorId = new LinkedHashMap<>();

    @PostConstruct
    void cargar() throws Exception {
        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {

            Map<Long, ActivoTIComun> comunPorId = cargarActivosTI(wb.getSheet("ActivosTI"),
                    EnumSet.of(TipoActivoEnum.SERVIDOR));
            Map<Long, List<Cpu>> cpuPorActivo = cargarCpu(wb.getSheet("Cpu"));
            Map<Long, List<Ram>> ramPorActivo = cargarRam(wb.getSheet("Ram"));
            Map<Long, List<Disco>> discosPorActivo = cargarDiscos(wb.getSheet("Discos"));
            Map<Long, List<TarjetaRed>> redPorActivo = cargarTarjetasRed(wb.getSheet("TarjetasRed"), wb.getSheet("PuertoTarjetaRed"));
            Map<Long, List<FuentePoder>> fuentesPorActivo = cargarFuentesPoder(wb.getSheet("FuentesPoder"));
            Map<Long, List<Ventilador>> ventiladoresPorActivo = cargarVentiladores(wb.getSheet("Ventiladores"));
            Map<Long, List<ControladoraRAID>> raidPorActivo = cargarControladorasRaid(wb.getSheet("ControladoraRaid"));

            Sheet hoja = wb.getSheet("Servidores");
            for (Row fila : hoja) {
                if (fila.getRowNum() == 0 || getInteger(fila, 0) == null) {
                    continue;
                }
                Long id = getInteger(fila, 0).longValue();
                ActivoTIComun comun = comunPorId.get(id);
                if (comun == null) {
                    continue;
                }
                List<Cpu> cpus = cpuPorActivo.getOrDefault(id, List.of());
                List<Ram> rams = ramPorActivo.getOrDefault(id, List.of());
                List<Disco> discos = discosPorActivo.getOrDefault(id, List.of());

                Servidor servidor = Servidor.builder()
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
                        .ip_sistema_operativo(getString(fila, 1))
                        .version_so(getString(fila, 2))
                        .idChasisSlot(getInteger(fila, 3) == null ? null : getInteger(fila, 3).longValue())
                        .tipo(parseTipoServidor(getString(fila, 4)))
                        .cpuTotalGhz(sumarCpuGhz(cpus))
                        .ramTotalGb(sumarRamGb(rams))
                        .capacidadDiscosGb(sumarCapacidadDiscos(discos))
                        .cpus(cpus)
                        .memoriaRAM(rams)
                        .discos(discos)
                        .tarjetasRED(redPorActivo.getOrDefault(id, List.of()))
                        .fuentesEnergia(fuentesPorActivo.getOrDefault(id, List.of()))
                        .ventiladores(ventiladoresPorActivo.getOrDefault(id, List.of()))
                        .controladorasRAID(raidPorActivo.getOrDefault(id, List.of()))
                        .build();

                cache.add(servidor);
                cachePorId.put(id, servidor);
            }
        }
    }

    static Double sumarCpuGhz(List<Cpu> cpus) {
        if (cpus == null || cpus.isEmpty()) {
            return null;
        }
        double total = 0;
        boolean algunDato = false;
        for (Cpu c : cpus) {
            if (c.getVelocidad_ghz() != null) {
                total += c.getVelocidad_ghz();
                algunDato = true;
            }
        }
        return algunDato ? redondear(total) : null;
    }

    static Double sumarRamGb(List<Ram> rams) {
        if (rams == null || rams.isEmpty()) {
            return null;
        }
        double total = 0;
        for (Ram r : rams) {
            total += r.getCapacidad_gb();
        }
        return redondear(total);
    }

    static Double sumarCapacidadDiscos(List<Disco> discos) {
        if (discos == null || discos.isEmpty()) {
            return null;
        }
        double total = 0;
        boolean algunDato = false;
        for (Disco d : discos) {
            if (d.getCapacidad_GB() != null) {
                total += d.getCapacidad_GB();
                algunDato = true;
            }
        }
        return algunDato ? redondear(total) : null;
    }

    private static Double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** EstadoEnum tiene valores con mayuscula inicial (Encendido/Apagado/Degradado): valueOf() estricto no sirve para texto libre del Excel. */
    static EstadoEnum parseEstado(String texto) {
        if (texto == null) {
            return null;
        }
        for (EstadoEnum e : EstadoEnum.values()) {
            if (e.name().equalsIgnoreCase(texto)) {
                return e;
            }
        }
        return null;
    }

    static TipoServidorEnum parseTipoServidor(String texto) {
        if (texto == null) {
            return null;
        }
        for (TipoServidorEnum t : TipoServidorEnum.values()) {
            if (t.name().equalsIgnoreCase(texto)) {
                return t;
            }
        }
        return null;
    }

    /** Campos que ActivosTI comparte entre servidor/storage/switch/chasis. Package-private: los otros repos lo reutilizan. */
    record ActivoTIComun(TipoActivoEnum tipoActivo, String numeroSerie, String hostname, String fabricante, String modelo,
                          String generacion, String ubicacion, String ipGestion, EstadoEnum estadoOperativo,
                          String cluster, String responsable, String ordenCompra,
                          String versionFirmware, LocalDate ultimaActualizacion, LocalDate fechaEos,
                          Double temperatura, Double consumoElectricoW) {
    }

    static Map<Long, ActivoTIComun> cargarActivosTI(Sheet hoja, Set<TipoActivoEnum> tiposAceptados) {
        Map<Long, ActivoTIComun> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 0) == null) {
                continue;
            }
            String tipoTexto = getString(fila, 1);
            if (tipoTexto == null) {
                continue;
            }
            TipoActivoEnum tipo = TipoActivoEnum.valueOf(tipoTexto.toUpperCase());
            if (!tiposAceptados.contains(tipo)) {
                continue;
            }
            Long id = getInteger(fila, 0).longValue();
            resultado.put(id, new ActivoTIComun(
                    tipo,
                    getString(fila, 2),
                    getString(fila, 3),
                    getString(fila, 4),
                    getString(fila, 5),
                    getString(fila, 6),
                    getString(fila, 7),
                    getString(fila, 8),
                    parseEstado(getString(fila, 9)),
                    getString(fila, 10),
                    getString(fila, 11),
                    getString(fila, 12),
                    getString(fila, 13),
                    getLocalDate(fila, 14),
                    getLocalDate(fila, 15),
                    getDouble(fila, 16),
                    getDouble(fila, 17)));
        }
        return resultado;
    }

    static Map<Long, List<Cpu>> cargarCpu(Sheet hoja) {
        Map<Long, List<Cpu>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            Cpu cpu = Cpu.builder()
                    .id(getInteger(fila, 0).longValue())
                    .numero_serial(getString(fila, 2))
                    .marca(getString(fila, 3))
                    .familia(getString(fila, 4))
                    .modelo(getString(fila, 5))
                    .velocidad_ghz(getDouble(fila, 6))
                    .cantidad_nucleos(getInteger(fila, 7) == null ? 0 : getInteger(fila, 7))
                    .cantidad_hilos(getInteger(fila, 8) == null ? 0 : getInteger(fila, 8))
                    .cacheL1Mb(getDouble(fila, 9))
                    .cacheL2Mb(getDouble(fila, 10))
                    .cacheL3Mb(getDouble(fila, 11))
                    .estado(parseEstado(getString(fila, 12)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(cpu);
        }
        return resultado;
    }

    static Map<Long, List<Ram>> cargarRam(Sheet hoja) {
        Map<Long, List<Ram>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            Ram ram = Ram.builder()
                    .id_ram(getInteger(fila, 0).longValue())
                    .numero_serial(getString(fila, 2))
                    .marca(getString(fila, 3))
                    .modelo(getString(fila, 4))
                    .generacion(getString(fila, 5))
                    .valocidad_mhz(getDouble(fila, 6))
                    .capacidad_gb(getInteger(fila, 7) == null ? 0 : getInteger(fila, 7))
                    .estado(parseEstado(getString(fila, 10)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(ram);
        }
        return resultado;
    }

    static Map<Long, List<Disco>> cargarDiscos(Sheet hoja) {
        Map<Long, List<Disco>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            Disco disco = Disco.builder()
                    .id(getInteger(fila, 0).longValue())
                    .numero_serial(getString(fila, 2))
                    .marca(getString(fila, 3))
                    .tipo(getString(fila, 5))
                    .capacidad_GB(getDouble(fila, 6))
                    .velocidad_rpm(getInteger(fila, 7) == null ? 0 : getInteger(fila, 7))
                    .estado(parseEstado(getString(fila, 8)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(disco);
        }
        return resultado;
    }

    static Map<Long, List<TarjetaRed>> cargarTarjetasRed(Sheet hoja, Sheet hojaPuertos) {
        Map<Long, List<PuertoTarjetaRed>> puertosPorTarjeta = cargarPuertosTarjetaRed(hojaPuertos);
        Map<Long, List<TarjetaRed>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            Long tarjetaId = getInteger(fila, 0).longValue();
            TarjetaRed tarjeta = TarjetaRed.builder()
                    .id(tarjetaId)
                    .numero_serial(getString(fila, 2))
                    .marca(getString(fila, 3))
                    .modelo(getString(fila, 4))
                    .cantidad_puertos(getInteger(fila, 6) == null ? 0 : getInteger(fila, 6))
                    .estado(parseEstado(getString(fila, 7)))
                    .puertos(puertosPorTarjeta.getOrDefault(tarjetaId, List.of()))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(tarjeta);
        }
        return resultado;
    }

    private static Map<Long, List<PuertoTarjetaRed>> cargarPuertosTarjetaRed(Sheet hoja) {
        Map<Long, List<PuertoTarjetaRed>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long tarjetaId = getInteger(fila, 1).longValue();
            PuertoTarjetaRed puerto = PuertoTarjetaRed.builder()
                    .numero_puerto(getString(fila, 2))
                    .mac_addess(getString(fila, 3))
                    .velocidad(getString(fila, 4))
                    .estado(parseEstado(getString(fila, 5)))
                    .build();
            resultado.computeIfAbsent(tarjetaId, k -> new ArrayList<>()).add(puerto);
        }
        return resultado;
    }

    static Map<Long, List<FuentePoder>> cargarFuentesPoder(Sheet hoja) {
        Map<Long, List<FuentePoder>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            String corrienteTexto = getString(fila, 5);
            FuentePoder fuente = FuentePoder.builder()
                    .id(getInteger(fila, 0).longValue())
                    .numero_serial(getString(fila, 2))
                    .modelo(getString(fila, 3))
                    .consumo_w(getDouble(fila, 4))
                    .tipo_corriente(corrienteTexto == null ? null : CorrienteEnum.valueOf(corrienteTexto.toUpperCase()))
                    .estado(parseEstado(getString(fila, 6)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(fuente);
        }
        return resultado;
    }

    static Map<Long, List<Ventilador>> cargarVentiladores(Sheet hoja) {
        Map<Long, List<Ventilador>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            Ventilador ventilador = Ventilador.builder()
                    .id(getInteger(fila, 0).longValue())
                    .numero_serial(getString(fila, 2))
                    .velocidad_rpm(getInteger(fila, 3) == null ? 0 : getInteger(fila, 3))
                    .modelo(getString(fila, 4))
                    .estado(parseEstado(getString(fila, 5)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(ventilador);
        }
        return resultado;
    }

    static Map<Long, List<ControladoraRAID>> cargarControladorasRaid(Sheet hoja) {
        Map<Long, List<ControladoraRAID>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long activoId = getInteger(fila, 1).longValue();
            ControladoraRAID controladora = ControladoraRAID.builder()
                    .id(getInteger(fila, 0))
                    .modelo(getString(fila, 2))
                    .raid(getString(fila, 3))
                    .numero_serial(getString(fila, 4))
                    .estado(parseEstado(getString(fila, 5)))
                    .build();
            resultado.computeIfAbsent(activoId, k -> new ArrayList<>()).add(controladora);
        }
        return resultado;
    }

    @Override
    public List<Servidor> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        String texto = q == null ? null : q.toLowerCase();
        return cache.stream()
                .filter(s -> estado == null || (s.getEstado_operativo() != null && estado.equalsIgnoreCase(s.getEstado_operativo().name())))
                .filter(s -> ubicacion == null || ubicacion.equalsIgnoreCase(s.getUbicacion()))
                .filter(s -> proyecto == null || proyecto.equalsIgnoreCase(s.getCluster()))
                .filter(s -> fabricante == null || fabricante.equalsIgnoreCase(s.getFabricante()))
                .filter(s -> texto == null || coincide(s, texto))
                .toList();
    }

    private boolean coincide(Servidor s, String texto) {
        return contiene(s.getNumero_serie(), texto)
                || contiene(s.getHostname(), texto)
                || contiene(s.getCluster(), texto);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    @Override
    public Optional<Servidor> buscarPorId(Long id) {
        return Optional.ofNullable(cachePorId.get(id));
    }
}
