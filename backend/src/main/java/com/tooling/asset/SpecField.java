package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecField {

    private String fieldName;

    private String fieldType;

    private Boolean required;

    private String defaultValue;

    private String label;
}
