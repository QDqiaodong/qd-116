package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkstationCapacityRepository extends JpaRepository<WorkstationCapacity, Long> {

    Optional<WorkstationCapacity> findByWorkstation(String workstation);

    List<WorkstationCapacity> findByRegion(String region);

    boolean existsByWorkstation(String workstation);
}
