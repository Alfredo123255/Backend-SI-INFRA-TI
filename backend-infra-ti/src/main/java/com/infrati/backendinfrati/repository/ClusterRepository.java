package com.infrati.backendinfrati.repository;

import com.infrati.backendinfrati.model.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClusterRepository extends JpaRepository<Cluster, String> {
}
