package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.MonitoreoSnmp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface MonitoreoSnmpRepository extends JpaRepository<MonitoreoSnmp, Long> {
    List<MonitoreoSnmp> findByActivoId(Long activoId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MonitoreoSnmp> findOneByActivoId(Long activoId);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM monitoreo_snmp WHERE id = :id AND activo_id IS NULL", nativeQuery = true)
    int eliminarSiNoVinculada(@Param("id") Long id);
}
