package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TraceService {

    private final ToolingAssetRepository toolingAssetRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final ToolingInventoryDiffRepository toolingInventoryDiffRepository;
    private final ScrapRecordRepository scrapRecordRepository;

    @Transactional(readOnly = true)
    public ToolingTraceVO getTraceByToolingCode(String toolingCode) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + toolingCode));

        List<TraceEvent> events = new ArrayList<>();

        events.add(TraceEvent.builder()
                .eventType("CREATE")
                .eventLabel("建档入库")
                .eventTime(asset.getCreateTime() != null ? asset.getCreateTime() :
                        (asset.getEntryDate() != null ? LocalDateTime.of(asset.getEntryDate(), LocalTime.MIN) : null))
                .workstation(asset.getWorkstation())
                .operator(null)
                .remark(asset.getRemark())
                .detail("建档入库，适配产品：" + asset.getProductName())
                .build());

        List<TransferRecord> transfers = transferRecordRepository.findByToolingCodeOrderByTransferTimeDesc(toolingCode);
        for (TransferRecord t : transfers) {
            events.add(TraceEvent.builder()
                    .eventType("TRANSFER")
                    .eventLabel("移位")
                    .eventTime(t.getTransferTime())
                    .workstation(t.getToWorkstation())
                    .operator(t.getOperator())
                    .remark(t.getRemark())
                    .detail("从 " + t.getFromWorkstation() + " 移位至 " + t.getToWorkstation())
                    .build());
        }

        List<ToolingInventoryDiff> diffs = toolingInventoryDiffRepository.findByToolingCodeOrderByCheckTimeDesc(toolingCode);
        for (ToolingInventoryDiff d : diffs) {
            events.add(TraceEvent.builder()
                    .eventType("INVENTORY")
                    .eventLabel("清点差异")
                    .eventTime(d.getCheckTime())
                    .workstation(d.getWorkstation())
                    .operator(d.getChecker())
                    .remark(d.getRemark())
                    .detail(buildInventoryDetail(d))
                    .build());
        }

        List<ScrapRecord> scraps = scrapRecordRepository.findByToolingCodeOrderByScrapDateDesc(toolingCode);
        for (ScrapRecord s : scraps) {
            LocalDate sd = s.getScrapDate();
            events.add(TraceEvent.builder()
                    .eventType("SCRAP")
                    .eventLabel("报废")
                    .eventTime(sd != null ? LocalDateTime.of(sd, LocalTime.MIN) : null)
                    .workstation(null)
                    .operator(s.getOperator())
                    .remark(s.getRemark())
                    .detail("报废原因：" + s.getScrapReason())
                    .build());
        }

        events.sort(Comparator.comparing(TraceEvent::getEventTime, Comparator.nullsLast(Comparator.reverseOrder())));

        return ToolingTraceVO.builder()
                .toolingCode(asset.getToolingCode())
                .productName(asset.getProductName())
                .currentStatus(asset.getStatus() != null ? asset.getStatus().name() : null)
                .currentStatusLabel(asset.getStatus() != null ? asset.getStatus().getDescription() : null)
                .currentWorkstation(asset.getWorkstation())
                .events(events)
                .build();
    }

    private String buildInventoryDetail(ToolingInventoryDiff d) {
        StringBuilder sb = new StringBuilder();
        sb.append("月度：").append(d.getCheckMonth());
        if ("MISSING".equals(d.getDiffType())) {
            sb.append("，状态：账面存在但实物缺失");
        } else if ("EXTRA".equals(d.getDiffType())) {
            sb.append("，状态：实物存在但账面未记录");
        } else if ("MATCH".equals(d.getDiffType())) {
            sb.append("，状态：账实一致");
        } else if (d.getDiffType() != null) {
            sb.append("，状态：").append(d.getDiffType());
        }
        return sb.toString();
    }
}
