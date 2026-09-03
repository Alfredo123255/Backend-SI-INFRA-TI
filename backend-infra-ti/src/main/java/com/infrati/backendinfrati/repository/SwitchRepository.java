package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Switch;

import java.util.List;
import java.util.Optional;

public interface SwitchRepository {

    List<Switch> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q);

    Optional<Switch> buscarPorId(Long id);
}
