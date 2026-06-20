package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkstationStayVO {

    private String workstation;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long stayDays;

    private String lastOperator;

    private Integer sequence;
}
