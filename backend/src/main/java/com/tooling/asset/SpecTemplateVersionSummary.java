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
public class SpecTemplateVersionSummary {

    private String category;

    private Integer version;

    private LocalDateTime updateTime;

    private Integer fieldCount;
}
