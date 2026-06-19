package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransferService {

    private final TransferRecordRepository transferRecordRepository;
    private final ToolingAssetRepository toolingAssetRepository;

    public TransferRecord transfer(String toolingCode, String fromWorkstation, String toWorkstation, String operator, String remark) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + toolingCode));

        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new RuntimeException("已报废工装不允许移位: " + toolingCode);
        }

        String actualFromWorkstation = asset.getWorkstation();

        if (actualFromWorkstation != null && actualFromWorkstation.equals(toWorkstation)) {
            throw new RuntimeException("目标工位与当前工位相同，无需移位");
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
}
