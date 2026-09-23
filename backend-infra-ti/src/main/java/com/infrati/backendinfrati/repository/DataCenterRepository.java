package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.DataCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataCenterRepository extends JpaRepository<DataCenter, String> {
}
