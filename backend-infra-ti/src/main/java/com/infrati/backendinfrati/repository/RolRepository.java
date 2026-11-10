package com.infrati.backendinfrati.repository;
import com.infrati.backendinfrati.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RolRepository extends JpaRepository<Rol,Integer> {
    Optional<Rol> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCase(String nombre);
    List<Rol> findAllByOrderByIdAsc();
}
