package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolingInventoryDiffRepository extends JpaRepository<ToolingInventoryDiff, Long> {

    List<ToolingInventoryDiff> findByToolingCodeOrderByCheckTimeDesc(String toolingCode);

    List<ToolingInventoryDiff> findByCheckMonth(String checkMonth);

    long deleteByToolingCode(String toolingCode);
}
