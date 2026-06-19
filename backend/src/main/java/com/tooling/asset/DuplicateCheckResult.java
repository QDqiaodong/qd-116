package com.tooling.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResult {

    private boolean duplicate;

    private boolean codeDuplicate;

    private List<ToolingAsset> similarAssets;

    private String message;
}
