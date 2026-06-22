package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransferService {

    private final TransferRecordRepository transferRecordRepository;
    private final ToolingAssetRepository toolingAssetRepository;
    private final WorkstationCapacityService workstationCapacityService;
    private final WorkstationCapacityRepository workstationCapacityRepository;

    private static final String REGION_INJECTION = "注塑机区";
    private static final String REGION_REPAIR = "维修区";
    private static final String REGION_MOLD = "模具库";

    private String getRegionOfWorkstation(String workstation) {
        if (workstation == null || workstation.isEmpty()) {
            return "其他";
        }
        java.util.Optional<WorkstationCapacity> capacity = workstationCapacityRepository.findByWorkstation(workstation);
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

    public TransferRecord transfer(String toolingCode, String fromWorkstation, String toWorkstation, String operator, String remark, String statusChangeRemark) {
        return transferWithApproval(toolingCode, fromWorkstation, toWorkstation, operator, remark, statusChangeRemark, null);
    }

    public TransferRecord transferWithApproval(String toolingCode, String fromWorkstation, String toWorkstation,
                                               String operator, String remark, String statusChangeRemark, Long approvalId) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new BusinessException("操作人不能为空");
        }
        if (statusChangeRemark == null || statusChangeRemark.trim().isEmpty()) {
            throw new BusinessException("状态变更说明不能为空");
        }

        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new BusinessException("工装不存在: " + toolingCode));

        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new BusinessException("已报废工装不允许移位");
        }

        String actualFromWorkstation = asset.getWorkstation();

        if (actualFromWorkstation != null && actualFromWorkstation.equals(toWorkstation)) {
            throw new BusinessException("目标工位与当前工位相同，无需移位");
        }

        if (isHighRiskTransfer(actualFromWorkstation, toWorkstation) && approvalId == null) {
            throw new BusinessException("该移位属于高风险移位（从注塑机区到维修区/模具库），需先走审批流程，请通过高风险移位审批接口提交申请");
        }

        workstationCapacityService.validateCapacityForTransfer(toWorkstation);

        asset.setWorkstation(toWorkstation);
        asset.setStatus(ToolingStatus.TRANSFERRED);
        asset.setLastStatusChangeRemark(statusChangeRemark);
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        TransferRecord record = TransferRecord.builder()
                .toolingCode(toolingCode)
                .fromWorkstation(actualFromWorkstation)
                .toWorkstation(toWorkstation)
                .transferTime(LocalDateTime.now())
                .operator(operator)
                .remark(remark)
                .statusChangeRemark(statusChangeRemark)
                .approvalId(approvalId)
                .build();
        return transferRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<TransferRecord> listByTooling(String toolingCode) {
        return transferRecordRepository.findByToolingCodeOrderByTransferTimeDesc(toolingCode);
    }

    @Transactional(readOnly = true)
    public List<TransferRecord> listAll() {
        return transferRecordRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WorkstationStayVO> getWorkstationStays(String toolingCode) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new BusinessException("工装不存在: " + toolingCode));

        List<TransferRecord> transfers = transferRecordRepository.findByToolingCodeOrderByTransferTimeAsc(toolingCode);

        List<WorkstationStayVO> stays = new ArrayList<>();

        String initialWorkstation = asset.getWorkstation();
        if (!transfers.isEmpty()) {
            TransferRecord earliest = transfers.get(0);
            if (earliest.getFromWorkstation() != null) {
                initialWorkstation = earliest.getFromWorkstation();
            }
        }

        LocalDateTime initialTime = asset.getCreateTime() != null ? asset.getCreateTime() :
                (asset.getEntryDate() != null ? LocalDateTime.of(asset.getEntryDate(), LocalTime.MIN) : null);

        if (initialWorkstation != null && initialTime != null) {
            stays.add(WorkstationStayVO.builder()
                    .workstation(initialWorkstation)
                    .startTime(initialTime)
                    .lastOperator(null)
                    .build());
        }

        for (TransferRecord t : transfers) {
            String toWs = t.getToWorkstation();
            if (toWs == null) continue;

            if (!stays.isEmpty()) {
                WorkstationStayVO lastStay = stays.get(stays.size() - 1);
                lastStay.setEndTime(t.getTransferTime());
                lastStay.setStayDays(calculateStayDays(lastStay.getStartTime(), lastStay.getEndTime()));

                if (toWs.equals(lastStay.getWorkstation())) {
                    lastStay.setLastOperator(t.getOperator());
                    continue;
                }
            }

            stays.add(WorkstationStayVO.builder()
                    .workstation(toWs)
                    .startTime(t.getTransferTime())
                    .lastOperator(t.getOperator())
                    .build());
        }

        if (!stays.isEmpty()) {
            WorkstationStayVO lastStay = stays.get(stays.size() - 1);
            if (lastStay.getEndTime() == null) {
                if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
                    lastStay.setEndTime(asset.getUpdateTime());
                } else {
                    lastStay.setEndTime(LocalDateTime.now());
                }
                lastStay.setStayDays(calculateStayDays(lastStay.getStartTime(), lastStay.getEndTime()));
            }
        }

        for (int i = 0; i < stays.size(); i++) {
            stays.get(i).setSequence(i + 1);
        }

        return stays;
    }

    private Long calculateStayDays(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        if (days == 0 && duration.toHours() >= 0) {
            return 1L;
        }
        return days;
    }
}
