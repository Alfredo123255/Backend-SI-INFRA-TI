package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Activos.Modelo;
import com.infrati.backendinfrati.model.MetricaHistorica;
import com.infrati.backendinfrati.repository.MetricaHistoricaRepository;
import com.infrati.backendinfrati.repository.ModeloRepository;
import org.springframework.stereotype.Component;

/**
 * Datos que no son columnas del activo: fechaEol (tabla modelos) y el uso
 * de cpu/ram (lectura mas reciente en metrica_historica).
 */
@Component
class DatosDerivadosActivo {

    static final String METRICA_CPU_USO_GHZ = "cpu_uso_ghz";
    static final String METRICA_RAM_USO_GB = "ram_uso_gb";

    private final ModeloRepository modeloRepository;
    private final MetricaHistoricaRepository metricaRepository;

    DatosDerivadosActivo(ModeloRepository modeloRepository, MetricaHistoricaRepository metricaRepository) {
        this.modeloRepository = modeloRepository;
        this.metricaRepository = metricaRepository;
    }

    void resolverFechaEol(Activo activo) {
        if (activo.getModelo() == null) {
            return;
        }
        activo.setFechaEol(modeloRepository.findById(activo.getModelo())
                .map(Modelo::getFechaEol)
                .orElse(null));
    }

    Double ultimaMetrica(Long activoId, String nombreMetrica) {
        return metricaRepository.findFirstByActivoIdAndNombreMetricaOrderByFechaMedicionDesc(activoId, nombreMetrica)
                .map(MetricaHistorica::getValor)
                .orElse(null);
    }
}
