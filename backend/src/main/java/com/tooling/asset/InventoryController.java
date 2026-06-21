package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public Result<InventoryCheck> check(
            @RequestParam String checkMonth,
            @RequestParam Integer totalBook,
            @RequestParam Integer totalActual,
            @RequestParam(required = false) Integer missingCount,
            @RequestParam(required = false) Integer misplacedCount,
            @RequestParam(required = false) Integer scrappedExcludedCount,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.check(checkMonth, totalBook, totalActual,
                missingCount, misplacedCount, scrappedExcludedCount, checker, remark));
    }

    @PostMapping("/diff")
    public Result<ToolingInventoryDiff> recordToolingDiff(
            @RequestParam String checkMonth,
            @RequestParam String toolingCode,
            @RequestParam(required = false) Boolean bookExists,
            @RequestParam(required = false) Boolean actualExists,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.recordToolingDiff(checkMonth, toolingCode, bookExists, actualExists, checker, remark));
    }

    @PostMapping("/diff/mark-missing")
    public Result<ToolingInventoryDiff> markMissing(
            @RequestParam String checkMonth,
            @RequestParam String toolingCode,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.markMissing(checkMonth, toolingCode, checker, remark));
    }

    @PostMapping("/diff/mark-misplaced")
    public Result<ToolingInventoryDiff> markMisplaced(
            @RequestParam String checkMonth,
            @RequestParam String toolingCode,
            @RequestParam String actualFoundWorkstation,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.markMisplaced(checkMonth, toolingCode, actualFoundWorkstation, checker, remark));
    }

    @PostMapping("/diff/mark-match")
    public Result<ToolingInventoryDiff> markMatch(
            @RequestParam String checkMonth,
            @RequestParam String toolingCode,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.markMatch(checkMonth, toolingCode, checker, remark));
    }

    @PostMapping("/diff/mark-extra")
    public Result<ToolingInventoryDiff> markExtra(
            @RequestParam String checkMonth,
            @RequestParam String toolingCode,
            @RequestParam(required = false) String actualFoundWorkstation,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.markExtra(checkMonth, toolingCode, actualFoundWorkstation, checker, remark));
    }

    @PostMapping("/diff/handle-recover/{diffId}")
    public Result<ToolingInventoryDiff> handleRecover(
            @PathVariable Long diffId,
            @RequestParam String handler,
            @RequestParam(required = false) String handleRemark) {
        return Result.ok(inventoryService.handleRecover(diffId, handler, handleRemark));
    }

    @PostMapping("/diff/handle-correct-workstation/{diffId}")
    public Result<ToolingInventoryDiff> handleCorrectWorkstation(
            @PathVariable Long diffId,
            @RequestParam String correctedWorkstation,
            @RequestParam String handler,
            @RequestParam(required = false) String handleRemark) {
        return Result.ok(inventoryService.handleCorrectWorkstation(diffId, correctedWorkstation, handler, handleRemark));
    }

    @PostMapping("/diff/handle-scrap/{diffId}")
    public Result<ToolingInventoryDiff> handleScrap(
            @PathVariable Long diffId,
            @RequestParam String scrapReason,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scrapDate,
            @RequestParam String handler,
            @RequestParam(required = false) String handleRemark) {
        return Result.ok(inventoryService.handleScrap(diffId, scrapReason, scrapDate, handler, handleRemark));
    }

    @GetMapping("/diff/pending/list")
    public Result<List<ToolingInventoryDiff>> listPendingDiffs(
            @RequestParam(required = false) String diffType) {
        return Result.ok(inventoryService.listPendingDiffs(diffType));
    }

    @GetMapping("/diff/pending/count")
    public Result<Map<String, Object>> countPendingDiffs() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", inventoryService.countPendingDiffs(null));
        result.put("missing", inventoryService.countPendingDiffs(InventoryService.DIFF_TYPE_MISSING));
        result.put("misplaced", inventoryService.countPendingDiffs(InventoryService.DIFF_TYPE_MISPLACED));
        result.put("extra", inventoryService.countPendingDiffs(InventoryService.DIFF_TYPE_EXTRA));
        return Result.ok(result);
    }

    @GetMapping("/diff/{toolingCode}")
    public Result<List<ToolingInventoryDiff>> listToolingDiffs(@PathVariable String toolingCode) {
        return Result.ok(inventoryService.listToolingDiffs(toolingCode));
    }

    @GetMapping("/diff/month/{checkMonth}")
    public Result<List<ToolingInventoryDiff>> listToolingDiffsByMonth(@PathVariable String checkMonth) {
        return Result.ok(inventoryService.listToolingDiffsByMonth(checkMonth));
    }

    @GetMapping("/list")
    public Result<List<InventoryCheck>> listAll() {
        return Result.ok(inventoryService.listAll());
    }

    @GetMapping("/latest")
    public Result<InventoryCheck> latest() {
        return Result.ok(inventoryService.getLatestCheck().orElse(null));
    }

    @GetMapping("/stats/summary")
    public Result<InventorySummaryStats> summaryStats(@RequestParam(required = false) String checkMonth) {
        return Result.ok(inventoryService.getSummaryStats(checkMonth));
    }
}
