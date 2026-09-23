package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.ChasisBlade;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ChasisBladeRepository extends JpaRepository<ChasisBlade, Long>, JpaSpecificationExecutor<ChasisBlade> {

    /**
     * Filtros combinables y busqueda por serie, hostname o cluster. Cualquier
     * parametro en null se ignora.
     */
    default List<ChasisBlade> buscar(String estado, String ubicacion, String cluster, String fabricante, String q) {
        return findAll(ActivoSpecifications.filtros(estado, ubicacion, cluster, fabricante, q), Sort.by("id"));
    }

    default Optional<ChasisBlade> buscarPorId(Long id) {
        return findById(id);
    }
}
