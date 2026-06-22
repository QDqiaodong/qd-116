package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapSummaryVO {

    private String toolingCode;

    private String productName;

    private String workstation;

    private String imageUrl;

    private LocalDate entryDate;

    private LocalDateTime lastTransferTime;

    private String lastTransferOperator;

    private ToolingStatus status;
}
