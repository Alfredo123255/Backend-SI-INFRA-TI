package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Activo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoRepository extends JpaRepository<Activo, Long> {
}
