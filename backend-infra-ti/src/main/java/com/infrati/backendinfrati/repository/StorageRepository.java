package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Storage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StorageRepository extends JpaRepository<Storage, Long>, JpaSpecificationExecutor<Storage> {

    default List<Storage> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return findAll(ActivoSpecifications.filtros(estado, ubicacion, proyecto, fabricante, q), Sort.by("id"));
    }

    default Optional<Storage> buscarPorId(Long id) {
        return findById(id);
    }
}
