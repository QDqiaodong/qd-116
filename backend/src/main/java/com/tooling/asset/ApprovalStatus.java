package com.tooling.asset;

import lombok.Getter;

@Getter
public enum ApprovalStatus {

    PENDING("待审批"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    EXECUTED("已执行"),
    CANCELLED("已取消");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }
}
