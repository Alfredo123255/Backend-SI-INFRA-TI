package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.MetricaHistorica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetricaHistoricaRepository extends JpaRepository<MetricaHistorica, Long> {

    /** Lectura mas reciente de una metrica (ej. 'cpu_uso_ghz') para un activo. */
    Optional<MetricaHistorica> findFirstByActivoIdAndNombreMetricaOrderByFechaMedicionDesc(Long activoId, String nombreMetrica);
}
