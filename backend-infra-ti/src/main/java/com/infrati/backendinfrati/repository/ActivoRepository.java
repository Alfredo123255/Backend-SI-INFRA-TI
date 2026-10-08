package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Activo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ActivoRepository extends JpaRepository<Activo, Long> {
    List<Activo> findByMantenimientoActivoTrue();
    @Modifying
    @Query(value = """
            UPDATE activo SET mantenimiento_activo = :mantenimiento
            WHERE id = :id
              AND COALESCE(mantenimiento_activo, false) <> :mantenimiento
              AND (NOT :mantenimiento OR estado_operativo IS DISTINCT FROM 'Baja')
            """, nativeQuery = true)
    int cambiarMantenimientoSiCorresponde(@Param("id") Long id,
            @Param("mantenimiento") boolean mantenimiento);
}
