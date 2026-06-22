package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestInventoryBatchVO {

    private String batchMonth;

    private String batchName;

    private InventoryBatch.BatchStatus status;

    private String statusDescription;

    private Integer totalDiffCount;

    private Integer pendingDiffCount;

    private Integer missingCount;

    private Integer misplacedCount;

    private Integer extraCount;

    private Integer processedCount;
}
