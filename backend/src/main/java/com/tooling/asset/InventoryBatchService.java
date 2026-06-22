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
public class InventoryBatchService {

    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryBatchSnapshotRepository inventoryBatchSnapshotRepository;
    private final ToolingAssetRepository toolingAssetRepository;
    private final ToolingInventoryDiffRepository toolingInventoryDiffRepository;

    private String regionOf(String workstation) {
        if (workstation == null || workstation.isEmpty()) return "其他";
        if (workstation.startsWith("注塑机")) return "注塑机区";
        if (workstation.startsWith("模具库")) return "模具库";
        if (workstation.equals("待检区") || workstation.startsWith("待检")) return "待检区";
        return "其他";
    }

    public InventoryBatch createBatch(String batchMonth, String batchName, String creator, String remark) {
        if (batchMonth == null || batchMonth.isEmpty()) {
            throw new BusinessException("清点月份不能为空");
        }
        if (inventoryBatchRepository.existsByBatchMonth(batchMonth)) {
            throw new BusinessException("该月份的清点批次已存在: " + batchMonth);
        }
        if (creator == null || creator.trim().isEmpty()) {
            throw new BusinessException("创建人不能为空");
        }

        List<ToolingAsset> allAssets = toolingAssetRepository.findAll();
        int totalBookCount = (int) allAssets.stream()
                .filter(a -> !ToolingStatus.SCRAPPED.equals(a.getStatus()))
                .count();
        int scrappedExcludedCount = (int) allAssets.stream()
                .filter(a -> ToolingStatus.SCRAPPED.equals(a.getStatus()))
                .count();

        String name = (batchName == null || batchName.isEmpty())
                ? batchMonth + "月度盘点"
                : batchName;

        InventoryBatch batch = InventoryBatch.builder()
                .batchMonth(batchMonth)
                .batchName(name)
                .status(InventoryBatch.BatchStatus.DRAFT)
                .totalBookCount(totalBookCount)
                .scrappedExcludedCount(scrappedExcludedCount)
                .creator(creator)
                .createTime(LocalDateTime.now())
                .remark(remark)
                .build();

        return inventoryBatchRepository.save(batch);
    }

    public InventoryBatch freezeBatch(Long batchId, String freezer) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));

        if (!InventoryBatch.BatchStatus.DRAFT.equals(batch.getStatus())) {
            throw new BusinessException("仅草稿状态的批次可执行冻结操作，当前状态: " + batch.getStatus().getDescription());
        }
        if (freezer == null || freezer.trim().isEmpty()) {
            throw new BusinessException("冻结人不能为空");
        }

        inventoryBatchSnapshotRepository.deleteByBatchId(batchId);

        List<ToolingAsset> allAssets = toolingAssetRepository.findAll();
        LocalDateTime snapshotTime = LocalDateTime.now();

        int activeCount = 0;
        for (ToolingAsset asset : allAssets) {
            InventoryBatchSnapshot snapshot = InventoryBatchSnapshot.builder()
                    .batchId(batchId)
                    .batchMonth(batch.getBatchMonth())
                    .toolingCode(asset.getToolingCode())
                    .productName(asset.getProductName())
                    .bookWorkstation(asset.getWorkstation())
                    .bookStatus(asset.getStatus())
                    .bookEntryDate(asset.getEntryDate())
                    .bookImageUrl(asset.getImageUrl())
                    .bookRemark(asset.getRemark())
                    .snapshotTime(snapshotTime)
                    .region(regionOf(asset.getWorkstation()))
                    .build();
            inventoryBatchSnapshotRepository.save(snapshot);

            if (!ToolingStatus.SCRAPPED.equals(asset.getStatus())) {
                activeCount++;
            }
        }

        batch.setStatus(InventoryBatch.BatchStatus.FROZEN);
        batch.setTotalBookCount(activeCount);
        batch.setFreezer(freezer);
        batch.setFreezeTime(snapshotTime);

        return inventoryBatchRepository.save(batch);
    }

    public InventoryBatch unfreezeBatch(Long batchId, String operator) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));

        if (!InventoryBatch.BatchStatus.FROZEN.equals(batch.getStatus())) {
            throw new BusinessException("仅已冻结状态的批次可执行解冻操作，当前状态: " + batch.getStatus().getDescription());
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new BusinessException("操作人不能为空");
        }

        inventoryBatchSnapshotRepository.deleteByBatchId(batchId);

        batch.setStatus(InventoryBatch.BatchStatus.DRAFT);
        batch.setFreezer(null);
        batch.setFreezeTime(null);
        batch.setRemark((batch.getRemark() == null ? "" : batch.getRemark())
                + "[解冻] 由 " + operator + " 于 " + LocalDateTime.now() + " 解冻; ");

        return inventoryBatchRepository.save(batch);
    }

    public InventoryBatch closeBatch(Long batchId, String closer) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));

        if (!InventoryBatch.BatchStatus.FROZEN.equals(batch.getStatus())) {
            throw new BusinessException("仅已冻结状态的批次可执行关闭操作，当前状态: " + batch.getStatus().getDescription());
        }
        if (closer == null || closer.trim().isEmpty()) {
            throw new BusinessException("关闭人不能为空");
        }

        batch.setStatus(InventoryBatch.BatchStatus.CLOSED);
        batch.setCloser(closer);
        batch.setCloseTime(LocalDateTime.now());

        return inventoryBatchRepository.save(batch);
    }

    public InventoryBatch updateBatch(Long batchId, String batchName, String remark) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));

        if (!InventoryBatch.BatchStatus.DRAFT.equals(batch.getStatus())) {
            throw new BusinessException("仅草稿状态的批次可修改，当前状态: " + batch.getStatus().getDescription());
        }

        if (batchName != null && !batchName.isEmpty()) {
            batch.setBatchName(batchName);
        }
        if (remark != null) {
            batch.setRemark(remark);
        }

        return inventoryBatchRepository.save(batch);
    }

    public void deleteBatch(Long batchId) {
        InventoryBatch batch = inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));

        if (InventoryBatch.BatchStatus.FROZEN.equals(batch.getStatus())
                || InventoryBatch.BatchStatus.CLOSED.equals(batch.getStatus())) {
            throw new BusinessException("已冻结或已关闭的批次不允许删除");
        }

        inventoryBatchSnapshotRepository.deleteByBatchId(batchId);
        inventoryBatchRepository.delete(batch);
    }

    @Transactional(readOnly = true)
    public InventoryBatch getBatch(Long batchId) {
        return inventoryBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException("清点批次不存在: " + batchId));
    }

    @Transactional(readOnly = true)
    public Optional<InventoryBatch> getBatchByMonth(String batchMonth) {
        return inventoryBatchRepository.findByBatchMonth(batchMonth);
    }

    @Transactional(readOnly = true)
    public List<InventoryBatch> listAllBatches() {
        return inventoryBatchRepository.findAllByOrderByBatchMonthDesc();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryBatch> getLatestBatch() {
        return inventoryBatchRepository.findTopByOrderByBatchMonthDescCreateTimeDesc();
    }

    @Transactional(readOnly = true)
    public Optional<LatestInventoryBatchVO> getLatestBatchSummary() {
        Optional<InventoryBatch> latestOpt = getLatestBatch();
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }

        InventoryBatch batch = latestOpt.get();
        String batchMonth = batch.getBatchMonth();

        List<ToolingInventoryDiff> diffs = toolingInventoryDiffRepository.findByCheckMonth(batchMonth);

        int totalDiffCount = diffs.size();
        int pendingCount = 0;
        int missingCount = 0;
        int misplacedCount = 0;
        int extraCount = 0;
        int processedCount = 0;

        for (ToolingInventoryDiff diff : diffs) {
            if ("PENDING".equals(diff.getHandleStatus())) {
                pendingCount++;
            } else if ("PROCESSED".equals(diff.getHandleStatus())) {
                processedCount++;
            }

            String diffType = diff.getDiffType();
            if ("MISSING".equals(diffType)) {
                missingCount++;
            } else if ("MISPLACED".equals(diffType)) {
                misplacedCount++;
            } else if ("EXTRA".equals(diffType)) {
                extraCount++;
            }
        }

        LatestInventoryBatchVO vo = LatestInventoryBatchVO.builder()
                .batchMonth(batch.getBatchMonth())
                .batchName(batch.getBatchName())
                .status(batch.getStatus())
                .statusDescription(batch.getStatus() != null ? batch.getStatus().getDescription() : null)
                .totalDiffCount(totalDiffCount)
                .pendingDiffCount(pendingCount)
                .missingCount(missingCount)
                .misplacedCount(misplacedCount)
                .extraCount(extraCount)
                .processedCount(processedCount)
                .build();

        return Optional.of(vo);
    }

    @Transactional(readOnly = true)
    public List<InventoryBatchSnapshot> listSnapshots(Long batchId) {
        if (!inventoryBatchRepository.existsById(batchId)) {
            throw new BusinessException("清点批次不存在: " + batchId);
        }
        return inventoryBatchSnapshotRepository.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public List<InventoryBatchSnapshot> listSnapshotsByMonth(String batchMonth) {
        return inventoryBatchSnapshotRepository.findByBatchMonth(batchMonth);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryBatchSnapshot> getSnapshot(Long batchId, String toolingCode) {
        return inventoryBatchSnapshotRepository.findByBatchIdAndToolingCode(batchId, toolingCode);
    }

    @Transactional(readOnly = true)
    public long countSnapshots(Long batchId) {
        return inventoryBatchSnapshotRepository.countByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public InventoryBatch.BatchStatus getBatchStatus(String batchMonth) {
        return inventoryBatchRepository.findByBatchMonth(batchMonth)
                .map(InventoryBatch::getStatus)
                .orElse(null);
    }
}
