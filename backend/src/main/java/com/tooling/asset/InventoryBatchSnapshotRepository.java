package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryBatchSnapshotRepository extends JpaRepository<InventoryBatchSnapshot, Long> {

    List<InventoryBatchSnapshot> findByBatchId(Long batchId);

    List<InventoryBatchSnapshot> findByBatchMonth(String batchMonth);

    Optional<InventoryBatchSnapshot> findByBatchIdAndToolingCode(Long batchId, String toolingCode);

    List<InventoryBatchSnapshot> findByBatchIdAndBookWorkstation(Long batchId, String bookWorkstation);

    long countByBatchId(Long batchId);

    long countByBatchIdAndBookStatus(Long batchId, ToolingStatus bookStatus);

    void deleteByBatchId(Long batchId);
}
