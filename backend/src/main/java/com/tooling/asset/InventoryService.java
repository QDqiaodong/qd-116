package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    public static final String HANDLE_STATUS_PENDING = "PENDING";
    public static final String HANDLE_STATUS_PROCESSED = "PROCESSED";

    public static final String HANDLE_TYPE_RECOVERED = "RECOVERED";
    public static final String HANDLE_TYPE_CORRECTED_WORKSTATION = "CORRECTED_WORKSTATION";
    public static final String HANDLE_TYPE_SCRAPPED = "SCRAPPED";
    public static final String HANDLE_TYPE_REGISTERED = "REGISTERED";

    public static final String DIFF_TYPE_MISSING = "MISSING";
    public static final String DIFF_TYPE_MISPLACED = "MISPLACED";
    public static final String DIFF_TYPE_MATCH = "MATCH";
    public static final String DIFF_TYPE_EXTRA = "EXTRA";
    public static final String DIFF_TYPE_UNKNOWN = "UNKNOWN";

    private final InventoryCheckRepository inventoryCheckRepository;
    private final ToolingInventoryDiffRepository toolingInventoryDiffRepository;
    private final ToolingAssetRepository toolingAssetRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final ScrapRecordRepository scrapRecordRepository;

    public InventoryCheck check(String checkMonth, Integer totalBook, Integer totalActual, String checker, String remark) {
        Integer difference = totalActual - totalBook;
        InventoryCheck inventoryCheck = inventoryCheckRepository
                .findTopByCheckMonthOrderByCheckTimeDesc(checkMonth)
                .orElseGet(InventoryCheck::new);
        inventoryCheck.setCheckMonth(checkMonth);
        inventoryCheck.setTotalBook(totalBook);
        inventoryCheck.setTotalActual(totalActual);
        inventoryCheck.setDifference(difference);
        inventoryCheck.setChecker(checker);
        inventoryCheck.setCheckTime(LocalDateTime.now());
        inventoryCheck.setRemark(remark);
        return inventoryCheckRepository.save(inventoryCheck);
    }

    public ToolingInventoryDiff recordToolingDiff(String checkMonth, String toolingCode,
                                                   Boolean bookExists, Boolean actualExists,
                                                   String checker, String remark) {
        String diffType;
        if (Boolean.TRUE.equals(bookExists) && Boolean.FALSE.equals(actualExists)) {
            diffType = DIFF_TYPE_MISSING;
        } else if (Boolean.FALSE.equals(bookExists) && Boolean.TRUE.equals(actualExists)) {
            diffType = DIFF_TYPE_EXTRA;
        } else if (Boolean.TRUE.equals(bookExists) && Boolean.TRUE.equals(actualExists)) {
            diffType = DIFF_TYPE_MATCH;
        } else {
            diffType = DIFF_TYPE_UNKNOWN;
        }
        String workstation = null;
        Optional<ToolingAsset> assetOpt = toolingAssetRepository.findByToolingCode(toolingCode);
        if (assetOpt.isPresent()) {
            workstation = assetOpt.get().getWorkstation();
        }
        String handleStatus = null;
        if (DIFF_TYPE_MATCH.equals(diffType)) {
            handleStatus = HANDLE_STATUS_PROCESSED;
        } else if (DIFF_TYPE_MISSING.equals(diffType) || DIFF_TYPE_EXTRA.equals(diffType)) {
            handleStatus = HANDLE_STATUS_PENDING;
        }
        ToolingInventoryDiff diff = ToolingInventoryDiff.builder()
                .checkMonth(checkMonth)
                .toolingCode(toolingCode)
                .bookExists(bookExists)
                .actualExists(actualExists)
                .diffType(diffType)
                .checker(checker)
                .checkTime(LocalDateTime.now())
                .workstation(workstation)
                .expectedWorkstation(workstation)
                .remark(remark)
                .handleStatus(handleStatus)
                .build();
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff markMissing(String checkMonth, String toolingCode, String checker, String remark) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在，无法标记缺失: " + toolingCode));
        ToolingInventoryDiff diff = toolingInventoryDiffRepository
                .findByCheckMonthAndToolingCode(checkMonth, toolingCode)
                .orElseGet(ToolingInventoryDiff::new);
        diff.setCheckMonth(checkMonth);
        diff.setToolingCode(toolingCode);
        diff.setBookExists(true);
        diff.setActualExists(false);
        diff.setDiffType(DIFF_TYPE_MISSING);
        diff.setChecker(checker);
        diff.setCheckTime(LocalDateTime.now());
        diff.setWorkstation(asset.getWorkstation());
        diff.setExpectedWorkstation(asset.getWorkstation());
        diff.setRemark(remark);
        diff.setHandleStatus(HANDLE_STATUS_PENDING);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff markMisplaced(String checkMonth, String toolingCode,
                                               String actualFoundWorkstation, String checker, String remark) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在，无法标记错位: " + toolingCode));
        if (actualFoundWorkstation == null || actualFoundWorkstation.trim().isEmpty()) {
            throw new RuntimeException("标记错位必须填写实际发现工位");
        }
        ToolingInventoryDiff diff = toolingInventoryDiffRepository
                .findByCheckMonthAndToolingCode(checkMonth, toolingCode)
                .orElseGet(ToolingInventoryDiff::new);
        diff.setCheckMonth(checkMonth);
        diff.setToolingCode(toolingCode);
        diff.setBookExists(true);
        diff.setActualExists(true);
        diff.setDiffType(DIFF_TYPE_MISPLACED);
        diff.setChecker(checker);
        diff.setCheckTime(LocalDateTime.now());
        diff.setWorkstation(asset.getWorkstation());
        diff.setExpectedWorkstation(asset.getWorkstation());
        diff.setActualFoundWorkstation(actualFoundWorkstation);
        diff.setRemark(remark);
        diff.setHandleStatus(HANDLE_STATUS_PENDING);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff markMatch(String checkMonth, String toolingCode, String checker, String remark) {
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(toolingCode)
                .orElseThrow(() -> new RuntimeException("工装不存在，无法标记一致: " + toolingCode));
        ToolingInventoryDiff diff = toolingInventoryDiffRepository
                .findByCheckMonthAndToolingCode(checkMonth, toolingCode)
                .orElseGet(ToolingInventoryDiff::new);
        diff.setCheckMonth(checkMonth);
        diff.setToolingCode(toolingCode);
        diff.setBookExists(true);
        diff.setActualExists(true);
        diff.setDiffType(DIFF_TYPE_MATCH);
        diff.setChecker(checker);
        diff.setCheckTime(LocalDateTime.now());
        diff.setWorkstation(asset.getWorkstation());
        diff.setExpectedWorkstation(asset.getWorkstation());
        diff.setRemark(remark);
        diff.setHandleStatus(HANDLE_STATUS_PROCESSED);
        diff.setHandleTime(LocalDateTime.now());
        diff.setHandler(checker);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff markExtra(String checkMonth, String toolingCode,
                                           String actualFoundWorkstation, String checker, String remark) {
        ToolingInventoryDiff diff = toolingInventoryDiffRepository
                .findByCheckMonthAndToolingCode(checkMonth, toolingCode)
                .orElseGet(ToolingInventoryDiff::new);
        diff.setCheckMonth(checkMonth);
        diff.setToolingCode(toolingCode);
        diff.setBookExists(false);
        diff.setActualExists(true);
        diff.setDiffType(DIFF_TYPE_EXTRA);
        diff.setChecker(checker);
        diff.setCheckTime(LocalDateTime.now());
        diff.setActualFoundWorkstation(actualFoundWorkstation);
        diff.setRemark(remark);
        diff.setHandleStatus(HANDLE_STATUS_PENDING);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff handleRecover(Long diffId, String handler, String handleRemark) {
        ToolingInventoryDiff diff = toolingInventoryDiffRepository.findById(diffId)
                .orElseThrow(() -> new RuntimeException("差异记录不存在: " + diffId));
        if (!DIFF_TYPE_MISSING.equals(diff.getDiffType())) {
            throw new RuntimeException("仅盘亏(MISSING)差异可执行找回操作");
        }
        if (HANDLE_STATUS_PROCESSED.equals(diff.getHandleStatus())) {
            throw new RuntimeException("该差异已闭环，不可重复处理");
        }
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(diff.getToolingCode())
                .orElseThrow(() -> new RuntimeException("关联工装不存在: " + diff.getToolingCode()));
        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new RuntimeException("关联工装已报废，无法执行找回");
        }
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        diff.setHandleStatus(HANDLE_STATUS_PROCESSED);
        diff.setHandleType(HANDLE_TYPE_RECOVERED);
        diff.setHandleTime(LocalDateTime.now());
        diff.setHandler(handler);
        diff.setHandleRemark(handleRemark);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff handleCorrectWorkstation(Long diffId, String correctedWorkstation,
                                                          String handler, String handleRemark) {
        ToolingInventoryDiff diff = toolingInventoryDiffRepository.findById(diffId)
                .orElseThrow(() -> new RuntimeException("差异记录不存在: " + diffId));
        if (!DIFF_TYPE_MISPLACED.equals(diff.getDiffType()) && !DIFF_TYPE_MISSING.equals(diff.getDiffType())) {
            throw new RuntimeException("仅错位(MISPLACED)或盘亏(MISSING)差异可执行修正工位操作");
        }
        if (HANDLE_STATUS_PROCESSED.equals(diff.getHandleStatus())) {
            throw new RuntimeException("该差异已闭环，不可重复处理");
        }
        if (correctedWorkstation == null || correctedWorkstation.trim().isEmpty()) {
            throw new RuntimeException("修正工位不能为空");
        }
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(diff.getToolingCode())
                .orElseThrow(() -> new RuntimeException("关联工装不存在: " + diff.getToolingCode()));
        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new RuntimeException("关联工装已报废，无法执行修正工位");
        }
        String fromWs = asset.getWorkstation();
        asset.setWorkstation(correctedWorkstation);
        asset.setStatus(ToolingStatus.TRANSFERRED);
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        TransferRecord transfer = TransferRecord.builder()
                .toolingCode(diff.getToolingCode())
                .fromWorkstation(fromWs)
                .toWorkstation(correctedWorkstation)
                .transferTime(LocalDateTime.now())
                .operator(handler)
                .remark("[清点修正工位] " + (handleRemark == null ? "" : handleRemark))
                .build();
        transferRecordRepository.save(transfer);

        diff.setHandleStatus(HANDLE_STATUS_PROCESSED);
        diff.setHandleType(HANDLE_TYPE_CORRECTED_WORKSTATION);
        diff.setHandleTime(LocalDateTime.now());
        diff.setHandler(handler);
        diff.setHandleRemark(handleRemark);
        diff.setCorrectedWorkstation(correctedWorkstation);
        return toolingInventoryDiffRepository.save(diff);
    }

    public ToolingInventoryDiff handleScrap(Long diffId, String scrapReason, LocalDate scrapDate,
                                             String handler, String handleRemark) {
        ToolingInventoryDiff diff = toolingInventoryDiffRepository.findById(diffId)
                .orElseThrow(() -> new RuntimeException("差异记录不存在: " + diffId));
        if (!DIFF_TYPE_MISSING.equals(diff.getDiffType()) && !DIFF_TYPE_MISPLACED.equals(diff.getDiffType())) {
            throw new RuntimeException("仅盘亏(MISSING)或错位(MISPLACED)差异可执行转报废操作");
        }
        if (HANDLE_STATUS_PROCESSED.equals(diff.getHandleStatus())) {
            throw new RuntimeException("该差异已闭环，不可重复处理");
        }
        if (scrapReason == null || scrapReason.trim().isEmpty()) {
            throw new RuntimeException("报废原因不能为空");
        }
        if (scrapDate == null) {
            scrapDate = LocalDate.now();
        }
        ToolingAsset asset = toolingAssetRepository.findByToolingCode(diff.getToolingCode())
                .orElseThrow(() -> new RuntimeException("关联工装不存在: " + diff.getToolingCode()));
        if (ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
            throw new RuntimeException("关联工装已报废，不允许重复报废");
        }
        asset.setStatus(ToolingStatus.SCRAPPED);
        asset.setUpdateTime(LocalDateTime.now());
        toolingAssetRepository.save(asset);

        ScrapRecord scrap = ScrapRecord.builder()
                .toolingCode(diff.getToolingCode())
                .scrapReason(scrapReason)
                .scrapDate(scrapDate)
                .operator(handler)
                .remark("[清点转报废] " + (handleRemark == null ? "" : handleRemark))
                .build();
        scrapRecordRepository.save(scrap);

        diff.setHandleStatus(HANDLE_STATUS_PROCESSED);
        diff.setHandleType(HANDLE_TYPE_SCRAPPED);
        diff.setHandleTime(LocalDateTime.now());
        diff.setHandler(handler);
        diff.setHandleRemark(handleRemark);
        return toolingInventoryDiffRepository.save(diff);
    }

    @Transactional(readOnly = true)
    public List<ToolingInventoryDiff> listPendingDiffs(String diffType) {
        if (diffType != null && !diffType.trim().isEmpty()) {
            return toolingInventoryDiffRepository.findByDiffTypeAndHandleStatusOrderByCheckTimeDesc(
                    diffType, HANDLE_STATUS_PENDING);
        }
        return toolingInventoryDiffRepository.findByHandleStatusOrderByCheckTimeDesc(HANDLE_STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    public long countPendingDiffs(String diffType) {
        if (diffType != null && !diffType.trim().isEmpty()) {
            return toolingInventoryDiffRepository.countByDiffTypeAndHandleStatus(diffType, HANDLE_STATUS_PENDING);
        }
        return toolingInventoryDiffRepository.countByHandleStatus(HANDLE_STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    public List<ToolingInventoryDiff> listToolingDiffs(String toolingCode) {
        return toolingInventoryDiffRepository.findByToolingCodeOrderByCheckTimeDesc(toolingCode);
    }

    @Transactional(readOnly = true)
    public List<ToolingInventoryDiff> listToolingDiffsByMonth(String checkMonth) {
        return toolingInventoryDiffRepository.findByCheckMonth(checkMonth);
    }

    @Transactional(readOnly = true)
    public List<InventoryCheck> listByMonth(String checkMonth) {
        return inventoryCheckRepository.findTopByCheckMonthOrderByCheckTimeDesc(checkMonth)
                .map(List::of)
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<InventoryCheck> listAll() {
        return inventoryCheckRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryCheck> getLatestCheck() {
        return inventoryCheckRepository.findTopByOrderByCheckMonthDescCheckTimeDesc();
    }
}
