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
public class TraceEvent {

    private String eventType;

    private String eventLabel;

    private LocalDateTime eventTime;

    private String workstation;

    private String operator;

    private String remark;

    private String detail;
}
