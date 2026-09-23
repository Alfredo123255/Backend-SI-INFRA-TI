package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Switch;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SwitchRepository extends JpaRepository<Switch, Long>, JpaSpecificationExecutor<Switch> {

    default List<Switch> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return findAll(ActivoSpecifications.filtros(estado, ubicacion, proyecto, fabricante, q), Sort.by("id"));
    }

    default Optional<Switch> buscarPorId(Long id) {
        return findById(id);
    }
}
