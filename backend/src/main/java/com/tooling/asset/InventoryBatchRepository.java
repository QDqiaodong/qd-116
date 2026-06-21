package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    Optional<InventoryBatch> findByBatchMonth(String batchMonth);

    List<InventoryBatch> findAllByOrderByBatchMonthDesc();

    Optional<InventoryBatch> findTopByOrderByBatchMonthDescCreateTimeDesc();

    boolean existsByBatchMonth(String batchMonth);
}
