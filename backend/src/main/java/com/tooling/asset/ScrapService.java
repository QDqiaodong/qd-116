package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRecordRepository scrapRecordRepository;
    private final ToolingAssetRepository toolingAssetRepository;

    public ScrapRecord scrap(String toolingCode, String scrapReason, java.time.LocalDate scrapDate, String operator, String remark) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + toolingCode));

        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new RuntimeException("工装已报废，不允许重复报废: " + toolingCode);
        }

        List<ScrapRecord> existingScraps = scrapRecordRepository.findByToolingCodeOrderByScrapDateDesc(toolingCode);
        if (existingScraps != null && !existingScraps.isEmpty()) {
            throw new RuntimeException("该工装已有报废记录，不允许重复提交: " + toolingCode);
        }

        asset.setStatus(ToolingStatus.SCRAPPED);
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        ScrapRecord record = ScrapRecord.builder()
                .toolingCode(toolingCode)
                .scrapReason(scrapReason)
                .scrapDate(scrapDate)
                .operator(operator)
                .remark(remark)
                .build();
        return scrapRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ScrapRecord> listByTooling(String toolingCode) {
        return scrapRecordRepository.findByToolingCodeOrderByScrapDateDesc(toolingCode);
    }

    @Transactional(readOnly = true)
    public List<ScrapRecord> listAll() {
        return scrapRecordRepository.findAll();
    }
}
