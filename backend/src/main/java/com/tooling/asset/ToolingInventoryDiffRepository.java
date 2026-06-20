package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolingInventoryDiffRepository extends JpaRepository<ToolingInventoryDiff, Long> {

    List<ToolingInventoryDiff> findByToolingCodeOrderByCheckTimeDesc(String toolingCode);

    List<ToolingInventoryDiff> findByCheckMonth(String checkMonth);

    long deleteByToolingCode(String toolingCode);

    List<ToolingInventoryDiff> findByHandleStatusOrderByCheckTimeDesc(String handleStatus);

    List<ToolingInventoryDiff> findByDiffTypeAndHandleStatusOrderByCheckTimeDesc(String diffType, String handleStatus);

    List<ToolingInventoryDiff> findByCheckMonthAndHandleStatus(String checkMonth, String handleStatus);

    long countByHandleStatus(String handleStatus);

    long countByDiffTypeAndHandleStatus(String diffType, String handleStatus);

    Optional<ToolingInventoryDiff> findByCheckMonthAndToolingCode(String checkMonth, String toolingCode);
}
