package com.infrati.backendinfrati.repository;
import com.infrati.backendinfrati.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;
public interface UsuarioRepository extends JpaRepository<Usuario,Integer> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    long countByRolId(Integer id);
    @Query("select count(u) from Usuario u where u.activo=true and u.rol.administrar_usuarios=true")
    long contarAdministradoresActivos();
    List<Usuario> findAllByOrderByIdAsc();
}
