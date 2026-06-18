package com.tooling.asset;

import lombok.Getter;

@Getter
public enum ToolingStatus {

    IN_USE("在用"),
    TRANSFERRED("已移位"),
    SCRAPPED("已报废");

    private final String description;

    ToolingStatus(String description) {
        this.description = description;
    }
}
