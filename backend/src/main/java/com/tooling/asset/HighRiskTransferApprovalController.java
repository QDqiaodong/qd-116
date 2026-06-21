package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/high-risk-transfer-approval")
@RequiredArgsConstructor
public class HighRiskTransferApprovalController {

    private final HighRiskTransferApprovalService approvalService;

    @PostMapping("/check")
    public Result<Boolean> checkHighRisk(
            @RequestParam String fromWorkstation,
            @RequestParam String toWorkstation) {
        boolean isHighRisk = approvalService.isHighRiskTransfer(fromWorkstation, toWorkstation);
        return Result.ok(isHighRisk);
    }

    @PostMapping("/apply")
    public Result<HighRiskTransferApproval> apply(
            @RequestParam String toolingCode,
            @RequestParam(required = false) String fromWorkstation,
            @RequestParam String toWorkstation,
            @RequestParam String applicant,
            @RequestParam String applyReason,
            @RequestParam(required = false) String remark) {
        HighRiskTransferApproval approval = approvalService.submitApplication(
                toolingCode, fromWorkstation, toWorkstation, applicant, applyReason, remark);
        return Result.ok(approval);
    }

    @PutMapping("/{id}/approve")
    public Result<HighRiskTransferApproval> approve(
            @PathVariable Long id,
            @RequestParam String approver,
            @RequestParam boolean approved,
            @RequestParam(required = false) String approveRemark) {
        HighRiskTransferApproval approval = approvalService.approve(id, approver, approved, approveRemark);
        return Result.ok(approval);
    }

    @PostMapping("/{id}/execute")
    public Result<HighRiskTransferApproval> execute(
            @PathVariable Long id,
            @RequestParam String executor) {
        HighRiskTransferApproval approval = approvalService.executeApprovedTransfer(id, executor);
        return Result.ok(approval);
    }

    @PutMapping("/{id}/cancel")
    public Result<HighRiskTransferApproval> cancel(
            @PathVariable Long id,
            @RequestParam String canceller) {
        HighRiskTransferApproval approval = approvalService.cancelApplication(id, canceller);
        return Result.ok(approval);
    }

    @GetMapping("/{id}")
    public Result<HighRiskTransferApproval> getById(@PathVariable Long id) {
        return Result.ok(approvalService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<HighRiskTransferApproval>> listAll() {
        return Result.ok(approvalService.listAll());
    }

    @GetMapping("/search")
    public Result<List<HighRiskTransferApproval>> search(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String toolingCode,
            @RequestParam(required = false) String applicant,
            @RequestParam(required = false) String fromRegion,
            @RequestParam(required = false) String toRegion) {
        return Result.ok(approvalService.listByConditions(status, toolingCode, applicant, fromRegion, toRegion));
    }

    @GetMapping("/tooling/{toolingCode}")
    public Result<List<HighRiskTransferApproval>> listByTooling(@PathVariable String toolingCode) {
        return Result.ok(approvalService.listByTooling(toolingCode));
    }

    @GetMapping("/pending")
    public Result<List<HighRiskTransferApproval>> listPending() {
        return Result.ok(approvalService.listPending());
    }
}
