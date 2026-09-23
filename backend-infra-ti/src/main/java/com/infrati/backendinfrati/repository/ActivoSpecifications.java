package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Activo;
import com.infrati.backendinfrati.model.Enum.EstadoEnum;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros combinables comunes a Servidor/Storage/Switch/ChasisBlade: cualquier
 * parametro en null se ignora, las comparaciones ignoran mayusculas y q busca
 * (contiene) en numero de serie, hostname o cluster.
 */
final class ActivoSpecifications {

    private ActivoSpecifications() {
    }

    static <T extends Activo> Specification<T> filtros(String estado, String ubicacion, String cluster, String fabricante, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (estado != null) {
                EstadoEnum estadoEnum = parseEstado(estado);
                if (estadoEnum == null) {
                    return cb.disjunction();
                }
                predicados.add(cb.equal(root.get("estado_operativo"), estadoEnum));
            }
            if (ubicacion != null) {
                predicados.add(cb.equal(cb.lower(root.get("ubicacion")), ubicacion.toLowerCase()));
            }
            if (cluster != null) {
                predicados.add(cb.equal(cb.lower(root.get("cluster")), cluster.toLowerCase()));
            }
            if (fabricante != null) {
                predicados.add(cb.equal(cb.lower(root.get("fabricante")), fabricante.toLowerCase()));
            }
            if (q != null) {
                String patron = "%" + q.toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("numero_serie")), patron),
                        cb.like(cb.lower(root.get("hostname")), patron),
                        cb.like(cb.lower(root.get("cluster")), patron)));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /** EstadoEnum tiene valores con mayuscula inicial (Encendido/Apagado/Degradado): valueOf() estricto no sirve para el query param. */
    private static EstadoEnum parseEstado(String texto) {
        for (EstadoEnum e : EstadoEnum.values()) {
            if (e.name().equalsIgnoreCase(texto)) {
                return e;
            }
        }
        return null;
    }
}
