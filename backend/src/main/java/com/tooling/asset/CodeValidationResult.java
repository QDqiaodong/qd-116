package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeValidationResult {

    private boolean valid;

    private boolean formatValid;

    private boolean exists;

    private String message;

    private String suggestedCode;
}
