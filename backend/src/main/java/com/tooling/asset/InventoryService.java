package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryCheckRepository inventoryCheckRepository;
    private final ToolingInventoryDiffRepository toolingInventoryDiffRepository;
    private final ToolingAssetRepository toolingAssetRepository;

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
            diffType = "MISSING";
        } else if (Boolean.FALSE.equals(bookExists) && Boolean.TRUE.equals(actualExists)) {
            diffType = "EXTRA";
        } else if (Boolean.TRUE.equals(bookExists) && Boolean.TRUE.equals(actualExists)) {
            diffType = "MATCH";
        } else {
            diffType = "UNKNOWN";
        }
        String workstation = null;
        Optional<ToolingAsset> assetOpt = toolingAssetRepository.findByToolingCode(toolingCode);
        if (assetOpt.isPresent()) {
            workstation = assetOpt.get().getWorkstation();
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
                .remark(remark)
                .build();
        return toolingInventoryDiffRepository.save(diff);
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
