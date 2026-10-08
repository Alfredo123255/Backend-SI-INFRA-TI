package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.HistoricoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HistoricoEstadoRepository extends JpaRepository<HistoricoEstado, Long> {
    List<HistoricoEstado> findByActivo_IdOrderByFechaCambioDescIdDesc(Long activoId);

    Optional<HistoricoEstado> findFirstByActivo_IdAndCampoAndValorNuevoOrderByFechaCambioDescIdDesc(
            Long activoId, String campo, String valorNuevo);
}
