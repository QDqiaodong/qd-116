package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityCheckResult {

    private boolean sufficient;

    private String workstation;

    private Integer currentCount;

    private Integer maxCapacity;

    private Integer availableSlots;

    private String message;
}
