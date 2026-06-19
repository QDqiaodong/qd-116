package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.check(checkMonth, totalBook, totalActual, checker, remark));
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
}
