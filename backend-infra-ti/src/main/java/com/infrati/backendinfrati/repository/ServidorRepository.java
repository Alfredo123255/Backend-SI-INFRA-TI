package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Servidor;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz pura: hoy la implementa ServidorExcelRepository (lee un .xlsx),
 * mas adelante puede implementarla una version con JpaRepository sin que
 * ServidorService ni ServidorController se enteren del cambio.
 */
public interface ServidorRepository {

    /**
     * RF-015 (filtros combinables) y RF-016 (busqueda por serie, hostname o
     * proyecto/cluster). Cualquier parametro en null se ignora. Aplica igual
     * para filas tipo FISICO o BLADE.
     */
    List<Servidor> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q);

    Optional<Servidor> buscarPorId(Long id);
}
