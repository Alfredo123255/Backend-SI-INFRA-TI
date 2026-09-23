package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModeloRepository extends JpaRepository<Modelo, String> {
}
