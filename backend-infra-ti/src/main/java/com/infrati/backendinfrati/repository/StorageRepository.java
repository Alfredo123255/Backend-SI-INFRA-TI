package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Activos.Storage;

import java.util.List;
import java.util.Optional;

public interface StorageRepository {

    List<Storage> buscar(String estado, String ubicacion, String proyecto, String fabricante, String q);

    Optional<Storage> buscarPorId(Long id);
}
