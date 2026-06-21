package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapReasonRepository extends JpaRepository<ScrapReason, Long> {

    Optional<ScrapReason> findByReasonCode(String reasonCode);

    List<ScrapReason> findByEnabledTrueOrderBySortOrderAsc();

    List<ScrapReason> findByCategoryOrderBySortOrderAsc(String category);

    List<ScrapReason> findByCategoryAndEnabledTrueOrderBySortOrderAsc(String category);

    boolean existsByReasonCode(String reasonCode);

    List<ScrapReason> findAllByOrderBySortOrderAsc();
}
