package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory-batch")
@RequiredArgsConstructor
public class InventoryBatchController {

    private final InventoryBatchService inventoryBatchService;

    @PostMapping
    public Result<InventoryBatch> create(
            @RequestParam String batchMonth,
            @RequestParam(required = false) String batchName,
            @RequestParam String creator,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryBatchService.createBatch(batchMonth, batchName, creator, remark));
    }

    @PostMapping("/{batchId}/freeze")
    public Result<InventoryBatch> freeze(
            @PathVariable Long batchId,
            @RequestParam String freezer) {
        return Result.ok(inventoryBatchService.freezeBatch(batchId, freezer));
    }

    @PostMapping("/{batchId}/unfreeze")
    public Result<InventoryBatch> unfreeze(
            @PathVariable Long batchId,
            @RequestParam String operator) {
        return Result.ok(inventoryBatchService.unfreezeBatch(batchId, operator));
    }

    @PostMapping("/{batchId}/close")
    public Result<InventoryBatch> close(
            @PathVariable Long batchId,
            @RequestParam String closer) {
        return Result.ok(inventoryBatchService.closeBatch(batchId, closer));
    }

    @PutMapping("/{batchId}")
    public Result<InventoryBatch> update(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> body) {
        String batchName = body.get("batchName");
        String remark = body.get("remark");
        return Result.ok(inventoryBatchService.updateBatch(batchId, batchName, remark));
    }

    @DeleteMapping("/{batchId}")
    public Result<Void> delete(@PathVariable Long batchId) {
        inventoryBatchService.deleteBatch(batchId);
        return Result.ok();
    }

    @GetMapping("/{batchId}")
    public Result<InventoryBatch> getById(@PathVariable Long batchId) {
        return Result.ok(inventoryBatchService.getBatch(batchId));
    }

    @GetMapping("/month/{batchMonth}")
    public Result<InventoryBatch> getByMonth(@PathVariable String batchMonth) {
        return Result.ok(inventoryBatchService.getBatchByMonth(batchMonth).orElse(null));
    }

    @GetMapping("/list")
    public Result<List<InventoryBatch>> list() {
        return Result.ok(inventoryBatchService.listAllBatches());
    }

    @GetMapping("/latest")
    public Result<InventoryBatch> latest() {
        return Result.ok(inventoryBatchService.getLatestBatch().orElse(null));
    }

    @GetMapping("/latest/summary")
    public Result<LatestInventoryBatchVO> latestSummary() {
        return Result.ok(inventoryBatchService.getLatestBatchSummary().orElse(null));
    }

    @GetMapping("/{batchId}/snapshots")
    public Result<List<InventoryBatchSnapshot>> listSnapshots(@PathVariable Long batchId) {
        return Result.ok(inventoryBatchService.listSnapshots(batchId));
    }

    @GetMapping("/month/{batchMonth}/snapshots")
    public Result<List<InventoryBatchSnapshot>> listSnapshotsByMonth(@PathVariable String batchMonth) {
        return Result.ok(inventoryBatchService.listSnapshotsByMonth(batchMonth));
    }

    @GetMapping("/{batchId}/snapshots/{toolingCode}")
    public Result<InventoryBatchSnapshot> getSnapshot(
            @PathVariable Long batchId,
            @PathVariable String toolingCode) {
        return Result.ok(inventoryBatchService.getSnapshot(batchId, toolingCode).orElse(null));
    }

    @GetMapping("/{batchId}/snapshot-count")
    public Result<Map<String, Object>> getSnapshotCount(@PathVariable Long batchId) {
        Map<String, Object> result = new HashMap<>();
        result.put("count", inventoryBatchService.countSnapshots(batchId));
        return Result.ok(result);
    }

    @GetMapping("/status/{batchMonth}")
    public Result<Map<String, Object>> getBatchStatus(@PathVariable String batchMonth) {
        InventoryBatch.BatchStatus status = inventoryBatchService.getBatchStatus(batchMonth);
        Map<String, Object> result = new HashMap<>();
        result.put("status", status);
        result.put("description", status != null ? status.getDescription() : null);
        return Result.ok(result);
    }
}
