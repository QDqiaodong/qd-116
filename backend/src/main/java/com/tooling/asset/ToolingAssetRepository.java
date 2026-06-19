package com.tooling.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ToolingAssetRepository extends JpaRepository<ToolingAsset, Long>, JpaSpecificationExecutor<ToolingAsset> {

    Optional<ToolingAsset> findByToolingCode(String toolingCode);

    List<ToolingAsset> findByStatus(ToolingStatus status);

    List<ToolingAsset> findByWorkstation(String workstation);

    List<ToolingAsset> findByProductNameAndWorkstationAndEntryDateBetween(
            String productName,
            String workstation,
            LocalDate startDate,
            LocalDate endDate
    );
}
