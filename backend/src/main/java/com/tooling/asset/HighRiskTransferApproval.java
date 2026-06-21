package com.tooling.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "high_risk_transfer_approval")
public class HighRiskTransferApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String toolingCode;

    private String fromWorkstation;

    private String fromRegion;

    private String toWorkstation;

    private String toRegion;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    private String applicant;

    private LocalDateTime applyTime;

    private String applyReason;

    private String approver;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String executor;

    private LocalDateTime executeTime;

    private Long transferRecordId;

    private String remark;
}
