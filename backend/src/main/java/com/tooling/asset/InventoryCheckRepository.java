package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {

    Optional<InventoryCheck> findTopByCheckMonthOrderByCheckTimeDesc(String checkMonth);

    Optional<InventoryCheck> findTopByOrderByCheckMonthDescCheckTimeDesc();
}
