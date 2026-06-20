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

    public TransferRecord transfer(String toolingCode, String fromWorkstation, String toWorkstation, String operator, String remark) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new BusinessException("操作人不能为空");
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

        asset.setWorkstation(toWorkstation);
        asset.setStatus(ToolingStatus.TRANSFERRED);
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        TransferRecord record = TransferRecord.builder()
                .toolingCode(toolingCode)
                .fromWorkstation(actualFromWorkstation)
                .toWorkstation(toWorkstation)
                .transferTime(LocalDateTime.now())
                .operator(operator)
                .remark(remark)
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
