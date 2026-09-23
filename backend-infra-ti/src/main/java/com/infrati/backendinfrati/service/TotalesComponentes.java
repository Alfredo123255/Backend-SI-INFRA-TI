package com.infrati.backendinfrati.service;

import com.infrati.backendinfrati.model.Componentes.Cpu;
import com.infrati.backendinfrati.model.Componentes.Disco;
import com.infrati.backendinfrati.model.Componentes.Ram;

import java.util.List;

/**
 * cpuTotalGhz, ramTotalGb y capacidadDiscosGb son @Transient: no existen como
 * columnas, se calculan sumando los componentes cargados via @OneToMany.
 */
final class TotalesComponentes {

    private TotalesComponentes() {
    }

    /**
     * Capacidad total = suma de velocidad_ghz x cantidad_nucleos de cada CPU,
     * para que sea comparable con la metrica cpu_uso_ghz (uso sobre todos los
     * nucleos). Las CPU sin velocidad o sin nucleos no suman.
     */
    static Double sumarCpuGhz(List<Cpu> cpus) {
        if (cpus == null || cpus.isEmpty()) {
            return null;
        }
        double total = 0;
        boolean algunDato = false;
        for (Cpu c : cpus) {
            if (c.getVelocidad_ghz() != null && c.getCantidad_nucleos() != null && c.getCantidad_nucleos() > 0) {
                total += c.getVelocidad_ghz() * c.getCantidad_nucleos();
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
        boolean algunDato = false;
        for (Ram r : rams) {
            if (r.getCapacidad_gb() != null) {
                total += r.getCapacidad_gb();
                algunDato = true;
            }
        }
        return algunDato ? redondear(total) : null;
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
}
