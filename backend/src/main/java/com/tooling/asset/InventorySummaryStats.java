package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryStats {

    private String checkMonth;

    private Integer totalBook;

    private Integer totalActualConfirmed;

    private Integer totalMissing;

    private Integer totalMisplaced;

    private Integer totalScrappedExcluded;

    private Integer totalExtra;

    private Integer totalChecked;

    private Integer totalPending;

    private List<InventoryRegionStats> regions;
}
