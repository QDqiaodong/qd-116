package com.tooling.asset;

import lombok.Getter;

import java.util.List;

@Getter
public class DuplicateWarningException extends RuntimeException {

    private final List<ToolingAsset> similarAssets;

    public DuplicateWarningException(String message, List<ToolingAsset> similarAssets) {
        super(message);
        this.similarAssets = similarAssets;
    }
}
