package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Componentes.ChasisSlot;
import com.infrati.backendinfrati.model.Componentes.FuentePoder;
import com.infrati.backendinfrati.model.Componentes.TarjetaRed;
import com.infrati.backendinfrati.model.Componentes.Ventilador;
import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import com.infrati.backendinfrati.model.Enum.EstadoSlotEnum;
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
 * Mismo patron que ServidorExcelRepository: une ActivosTI (tipo_activo =
 * CHASIS) con ChasisBlade (exclusivas: cantidad_slots) y ChasisSlot (varios
 * slots por chasis via chasisID). Reutiliza los loaders compartidos de
 * ServidorExcelRepository para ventiladores, tarjetasRed y fuentesEnergia.
 */
@Repository
public class ChasisBladeExcelRepository implements ChasisBladeRepository {

    @Value("classpath:data/inventario.xlsx")
    private Resource archivo;

    private List<ChasisBlade> cache = new ArrayList<>();
    private Map<Long, ChasisBlade> cachePorId = new LinkedHashMap<>();

    @PostConstruct
    void cargar() throws Exception {
        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {

            var comunPorId = ServidorExcelRepository.cargarActivosTI(wb.getSheet("ActivosTI"), EnumSet.of(TipoActivoEnum.CHASIS));
            Map<Long, List<ChasisSlot>> slotsPorChasis = cargarChasisSlots(wb.getSheet("ChasisSlot"));
            Map<Long, List<TarjetaRed>> redPorActivo = ServidorExcelRepository.cargarTarjetasRed(wb.getSheet("TarjetasRed"), wb.getSheet("PuertoTarjetaRed"));
            Map<Long, List<FuentePoder>> fuentesPorActivo = ServidorExcelRepository.cargarFuentesPoder(wb.getSheet("FuentesPoder"));
            Map<Long, List<Ventilador>> ventiladoresPorActivo = ServidorExcelRepository.cargarVentiladores(wb.getSheet("Ventiladores"));

            Sheet hoja = wb.getSheet("ChasisBlade");
            for (Row fila : hoja) {
                if (fila.getRowNum() == 0 || getInteger(fila, 0) == null) {
                    continue;
                }
                Long id = getInteger(fila, 0).longValue();
                var comun = comunPorId.get(id);
                if (comun == null) {
                    continue;
                }

                ChasisBlade chasis = ChasisBlade.builder()
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
                        .cantidad_slots(getInteger(fila, 1) == null ? 0 : getInteger(fila, 1))
                        .chasisSlots(slotsPorChasis.getOrDefault(id, List.of()))
                        .tarjetasRED(redPorActivo.getOrDefault(id, List.of()))
                        .fuentesEnergia(fuentesPorActivo.getOrDefault(id, List.of()))
                        .ventiladores(ventiladoresPorActivo.getOrDefault(id, List.of()))
                        .build();

                cache.add(chasis);
                cachePorId.put(id, chasis);
            }
        }
    }

    private Map<Long, List<ChasisSlot>> cargarChasisSlots(Sheet hoja) {
        Map<Long, List<ChasisSlot>> resultado = new LinkedHashMap<>();
        if (hoja == null) {
            return resultado;
        }
        for (Row fila : hoja) {
            if (fila.getRowNum() == 0 || getInteger(fila, 1) == null) {
                continue;
            }
            Long chasisId = getInteger(fila, 1).longValue();
            ChasisSlot slot = ChasisSlot.builder()
                    .id(getInteger(fila, 0) == null ? null : getInteger(fila, 0).longValue())
                    .chasisID(chasisId)
                    .numeroSlot(getInteger(fila, 2))
                    .estado(parseEstadoSlot(getString(fila, 3)))
                    .hostanameServidor(getString(fila, 4))
                    .build();
            resultado.computeIfAbsent(chasisId, k -> new ArrayList<>()).add(slot);
        }
        return resultado;
    }

    /** EstadoSlotEnum tiene valores con mayuscula inicial (Ocupado/Libre/Degradado): valueOf() estricto no sirve para texto libre del Excel. */
    private static EstadoSlotEnum parseEstadoSlot(String texto) {
        if (texto == null) {
            return null;
        }
        for (EstadoSlotEnum e : EstadoSlotEnum.values()) {
            if (e.name().equalsIgnoreCase(texto)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public List<ChasisBlade> buscar(String estado, String ubicacion, String cluster, String fabricante, String q) {
        String texto = q == null ? null : q.toLowerCase();
        return cache.stream()
                .filter(c -> estado == null || (c.getEstado_operativo() != null && estado.equalsIgnoreCase(c.getEstado_operativo().name())))
                .filter(c -> ubicacion == null || ubicacion.equalsIgnoreCase(c.getUbicacion()))
                .filter(c -> cluster == null || cluster.equalsIgnoreCase(c.getCluster()))
                .filter(c -> fabricante == null || fabricante.equalsIgnoreCase(c.getFabricante()))
                .filter(c -> texto == null || coincide(c, texto))
                .toList();
    }

    private boolean coincide(ChasisBlade c, String texto) {
        return contiene(c.getNumero_serie(), texto)
                || contiene(c.getHostname(), texto)
                || contiene(c.getCluster(), texto);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    @Override
    public Optional<ChasisBlade> buscarPorId(Long id) {
        return Optional.ofNullable(cachePorId.get(id));
    }
}
