package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Servidor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServidorRepository extends JpaRepository<Servidor, Long>, JpaSpecificationExecutor<Servidor> {

    /**
     * RF-015 (filtros combinables) y RF-016 (busqueda por serie, hostname o
     * proyecto/cluster). Cualquier parametro en null se ignora. Aplica igual
     * para filas tipo FISICO o BLADE.
     */
    default List<Servidor> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q) {
        return findAll(ActivoSpecifications.filtros(estado, ubicacion, proyecto, fabricante, q), Sort.by("id"));
    }

    /** Busqueda inversa para ChasisSlot.servidorId: servidores blade alojados en esos slots. */
    List<Servidor> findByIdChasisSlotIn(Collection<Long> idsChasisSlot);

    default Optional<Servidor> buscarPorId(Long id) {
        return findById(id);
    }
}
