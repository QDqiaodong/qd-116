package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecTemplateVersion {

    private String category;

    private Integer version;

    private List<SpecField> fields;

    private LocalDateTime updateTime;
}
