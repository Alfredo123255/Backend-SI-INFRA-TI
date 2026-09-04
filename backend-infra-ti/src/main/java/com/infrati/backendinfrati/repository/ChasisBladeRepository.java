package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.ChasisBlade;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz pura: hoy la implementa ChasisBladeExcelRepository (lee un .xlsx),
 * mas adelante puede implementarla una version con JpaRepository sin que
 * ChasisBladeService ni ChasisBladeController se enteren del cambio.
 */
public interface ChasisBladeRepository {

    /**
     * Filtros combinables y busqueda por serie, hostname o cluster. Cualquier
     * parametro en null se ignora.
     */
    List<ChasisBlade> buscar(String estado, String ubicacion, String cluster, String fabricante, String q);

    Optional<ChasisBlade> buscarPorId(Long id);
}
