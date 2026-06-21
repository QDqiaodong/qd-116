package com.tooling.asset;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class HighRiskTransferApprovalService {

    private final HighRiskTransferApprovalRepository approvalRepository;
    private final WorkstationCapacityRepository workstationCapacityRepository;
    private final ToolingAssetRepository toolingAssetRepository;
    private final TransferService transferService;
    private final TransferRecordRepository transferRecordRepository;

    private static final String REGION_INJECTION = "注塑机区";
    private static final String REGION_REPAIR = "维修区";
    private static final String REGION_MOLD = "模具库";

    private String getRegionOfWorkstation(String workstation) {
        if (workstation == null || workstation.isEmpty()) {
            return "其他";
        }
        Optional<WorkstationCapacity> capacity = workstationCapacityRepository.findByWorkstation(workstation);
        if (capacity.isPresent() && capacity.get().getRegion() != null && !capacity.get().getRegion().isEmpty()) {
            return capacity.get().getRegion();
        }
        if (workstation.startsWith("注塑机")) return REGION_INJECTION;
        if (workstation.startsWith("模具库")) return REGION_MOLD;
        if (workstation.contains("维修")) return REGION_REPAIR;
        if (workstation.equals("待检区") || workstation.startsWith("待检")) return "待检区";
        return "其他";
    }

    public boolean isHighRiskTransfer(String fromWorkstation, String toWorkstation) {
        String fromRegion = getRegionOfWorkstation(fromWorkstation);
        String toRegion = getRegionOfWorkstation(toWorkstation);
        return REGION_INJECTION.equals(fromRegion)
                && (REGION_REPAIR.equals(toRegion) || REGION_MOLD.equals(toRegion));
    }

    public HighRiskTransferApproval submitApplication(String toolingCode, String fromWorkstation,
                                                      String toWorkstation, String applicant,
                                                      String applyReason, String remark) {
        if (applicant == null || applicant.trim().isEmpty()) {
            throw new BusinessException("申请人不能为空");
        }
        if (applyReason == null || applyReason.trim().isEmpty()) {
            throw new BusinessException("申请原因不能为空");
        }

        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new BusinessException("工装不存在: " + toolingCode));

        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new BusinessException("已报废工装不允许移位");
        }

        String actualFromWorkstation = asset.getWorkstation();
        if (fromWorkstation != null && !fromWorkstation.trim().isEmpty()
                && actualFromWorkstation != null && !actualFromWorkstation.equals(fromWorkstation)) {
            throw new BusinessException("源工位与工装当前工位不一致，当前工位: " + actualFromWorkstation);
        }

        if (actualFromWorkstation != null && actualFromWorkstation.equals(toWorkstation)) {
            throw new BusinessException("目标工位与当前工位相同，无需移位");
        }

        if (!isHighRiskTransfer(actualFromWorkstation, toWorkstation)) {
            throw new BusinessException("该移位不属于高风险移位，无需走审批流程，请直接执行移位操作");
        }

        List<ApprovalStatus> pendingStatuses = Arrays.asList(ApprovalStatus.PENDING, ApprovalStatus.APPROVED);
        Optional<HighRiskTransferApproval> existingPending = approvalRepository
                .findFirstByToolingCodeAndStatusInOrderByApplyTimeDesc(toolingCode, pendingStatuses);
        if (existingPending.isPresent()) {
            throw new BusinessException("该工装已有待处理的高风险移位申请，请先处理现有申请");
        }

        String fromRegion = getRegionOfWorkstation(actualFromWorkstation);
        String toRegion = getRegionOfWorkstation(toWorkstation);

        HighRiskTransferApproval approval = HighRiskTransferApproval.builder()
                .toolingCode(toolingCode)
                .fromWorkstation(actualFromWorkstation)
                .fromRegion(fromRegion)
                .toWorkstation(toWorkstation)
                .toRegion(toRegion)
                .status(ApprovalStatus.PENDING)
                .applicant(applicant)
                .applyTime(LocalDateTime.now())
                .applyReason(applyReason)
                .remark(remark)
                .build();

        return approvalRepository.save(approval);
    }

    public HighRiskTransferApproval approve(Long id, String approver, boolean approved, String approveRemark) {
        if (approver == null || approver.trim().isEmpty()) {
            throw new BusinessException("审批人不能为空");
        }

        HighRiskTransferApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审批申请不存在: " + id));

        if (!ApprovalStatus.PENDING.equals(approval.getStatus())) {
            throw new BusinessException("该申请当前状态为" + approval.getStatus().getDescription() + "，不允许审批");
        }

        approval.setApprover(approver);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(approveRemark);
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);

        return approvalRepository.save(approval);
    }

    public HighRiskTransferApproval executeApprovedTransfer(Long id, String executor) {
        if (executor == null || executor.trim().isEmpty()) {
            throw new BusinessException("执行人不能为空");
        }

        HighRiskTransferApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审批申请不存在: " + id));

        if (!ApprovalStatus.APPROVED.equals(approval.getStatus())) {
            throw new BusinessException("该申请当前状态为" + approval.getStatus().getDescription() + "，不允许执行移位");
        }

        ToolingAsset asset = toolingAssetRepository.findByToolingCode(approval.getToolingCode())
                .orElseThrow(() -> new BusinessException("工装不存在: " + approval.getToolingCode()));

        String currentWorkstation = asset.getWorkstation();
        if (currentWorkstation != null && !currentWorkstation.equals(approval.getFromWorkstation())) {
            throw new BusinessException("工装当前工位(" + currentWorkstation + ")与申请时工位("
                    + approval.getFromWorkstation() + ")不一致，请重新申请");
        }

        TransferRecord transferRecord = transferService.transferWithApproval(
                approval.getToolingCode(),
                approval.getFromWorkstation(),
                approval.getToWorkstation(),
                executor,
                "高风险移位审批通过执行，审批ID: " + id,
                id
        );

        approval.setExecutor(executor);
        approval.setExecuteTime(LocalDateTime.now());
        approval.setTransferRecordId(transferRecord.getId());
        approval.setStatus(ApprovalStatus.EXECUTED);

        return approvalRepository.save(approval);
    }

    public HighRiskTransferApproval cancelApplication(Long id, String canceller) {
        if (canceller == null || canceller.trim().isEmpty()) {
            throw new BusinessException("操作人不能为空");
        }

        HighRiskTransferApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审批申请不存在: " + id));

        if (!ApprovalStatus.PENDING.equals(approval.getStatus()) && !ApprovalStatus.APPROVED.equals(approval.getStatus())) {
            throw new BusinessException("该申请当前状态为" + approval.getStatus().getDescription() + "，不允许取消");
        }

        approval.setStatus(ApprovalStatus.CANCELLED);
        approval.setApproveRemark(approval.getApproveRemark() == null
                ? "由 " + canceller + " 取消"
                : approval.getApproveRemark() + "；由 " + canceller + " 取消");

        return approvalRepository.save(approval);
    }

    @Transactional(readOnly = true)
    public HighRiskTransferApproval getById(Long id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审批申请不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<HighRiskTransferApproval> listAll() {
        return approvalRepository.findAll((root, query, cb) -> {
            query.orderBy(cb.desc(root.get("applyTime")));
            return cb.conjunction();
        });
    }

    @Transactional(readOnly = true)
    public List<HighRiskTransferApproval> listByConditions(ApprovalStatus status, String toolingCode,
                                                           String applicant, String fromRegion, String toRegion) {
        Specification<HighRiskTransferApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (toolingCode != null && !toolingCode.isEmpty()) {
                predicates.add(cb.like(root.get("toolingCode"), "%" + toolingCode + "%"));
            }
            if (applicant != null && !applicant.isEmpty()) {
                predicates.add(cb.like(root.get("applicant"), "%" + applicant + "%"));
            }
            if (fromRegion != null && !fromRegion.isEmpty()) {
                predicates.add(cb.equal(root.get("fromRegion"), fromRegion));
            }
            if (toRegion != null && !toRegion.isEmpty()) {
                predicates.add(cb.equal(root.get("toRegion"), toRegion));
            }
            query.orderBy(cb.desc(root.get("applyTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return approvalRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public List<HighRiskTransferApproval> listByTooling(String toolingCode) {
        return approvalRepository.findByToolingCodeOrderByApplyTimeDesc(toolingCode);
    }

    @Transactional(readOnly = true)
    public List<HighRiskTransferApproval> listPending() {
        return approvalRepository.findByStatusOrderByApplyTimeDesc(ApprovalStatus.PENDING);
    }
}
