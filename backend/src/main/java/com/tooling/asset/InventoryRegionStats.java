package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRegionStats {

    private String region;

    private Integer bookCount;

    private Integer actualConfirmedCount;

    private Integer missingCount;

    private Integer misplacedCount;

    private Integer scrappedExcludedCount;

    private Integer extraCount;

    private Integer checkedCount;

    private Integer pendingCount;
}
