package com.tooling.asset;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ToolingAssetService {

    private static final int ENTRY_DATE_TOLERANCE_DAYS = 7;

    private final ToolingAssetRepository toolingAssetRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final ToolingInventoryDiffRepository toolingInventoryDiffRepository;
    private final ScrapRecordRepository scrapRecordRepository;
    private final WorkstationCapacityService workstationCapacityService;

    public DuplicateCheckResult checkDuplicate(ToolingAsset asset) {
        boolean codeDuplicate = false;
        if (asset.getId() != null) {
            codeDuplicate = toolingAssetRepository.findByToolingCode(asset.getToolingCode())
                    .filter(a -> !a.getId().equals(asset.getId()))
                    .isPresent();
        } else {
            codeDuplicate = toolingAssetRepository.findByToolingCode(asset.getToolingCode()).isPresent();
        }

        List<ToolingAsset> similarAssets = new ArrayList<>();
        if (asset.getProductName() != null && asset.getWorkstation() != null && asset.getEntryDate() != null) {
            LocalDate startDate = asset.getEntryDate().minusDays(ENTRY_DATE_TOLERANCE_DAYS);
            LocalDate endDate = asset.getEntryDate().plusDays(ENTRY_DATE_TOLERANCE_DAYS);
            similarAssets = toolingAssetRepository
                    .findByProductNameAndWorkstationAndEntryDateBetween(
                            asset.getProductName(),
                            asset.getWorkstation(),
                            startDate,
                            endDate
                    );
            if (asset.getId() != null) {
                similarAssets = similarAssets.stream()
                        .filter(a -> !a.getId().equals(asset.getId()))
                        .collect(Collectors.toList());
            }
        }

        boolean hasDuplicate = codeDuplicate || !similarAssets.isEmpty();
        String message = buildDuplicateMessage(codeDuplicate, similarAssets, asset.getEntryDate());

        return DuplicateCheckResult.builder()
                .duplicate(hasDuplicate)
                .codeDuplicate(codeDuplicate)
                .similarAssets(similarAssets)
                .message(message)
                .build();
    }

    private String buildDuplicateMessage(boolean codeDuplicate, List<ToolingAsset> similarAssets, LocalDate currentEntryDate) {
        StringBuilder sb = new StringBuilder();
        if (codeDuplicate) {
            sb.append("⚠️ 工装编号已存在，无法录入。\n");
        }
        if (!similarAssets.isEmpty()) {
            sb.append("⚠️ 检测到 ").append(similarAssets.size()).append(" 条疑似重复记录：\n");
            for (int i = 0; i < similarAssets.size(); i++) {
                ToolingAsset a = similarAssets.get(i);
                sb.append("  ").append(i + 1).append(". 编号: ").append(a.getToolingCode())
                        .append("，产品: ").append(a.getProductName())
                        .append("，工位: ").append(a.getWorkstation())
                        .append("，入库日期: ").append(a.getEntryDate());
                if (a.getEntryDate() != null && currentEntryDate != null) {
                    long days = Math.abs(ChronoUnit.DAYS.between(a.getEntryDate(), currentEntryDate));
                    sb.append("（相差").append(days).append("天）");
                }
                sb.append("\n");
            }
            sb.append("请确认是否继续录入。");
        }
        return sb.toString().trim();
    }

    public ToolingAsset createAsset(ToolingAsset asset, boolean forceCreate) {
        if (toolingAssetRepository.findByToolingCode(asset.getToolingCode()).isPresent()) {
            throw new RuntimeException("工装编号已存在: " + asset.getToolingCode());
        }
        if (!forceCreate) {
            DuplicateCheckResult check = checkDuplicate(asset);
            if (check.isDuplicate() && !check.isCodeDuplicate()) {
                throw new DuplicateWarningException(check.getMessage(), check.getSimilarAssets());
            }
        }
        workstationCapacityService.validateCapacityForAdd(asset.getWorkstation());
        asset.setCreateTime(LocalDateTime.now());
        asset.setUpdateTime(LocalDateTime.now());
        return toolingAssetRepository.save(asset);
    }

    public ToolingAsset updateAsset(Long id, ToolingAsset asset, boolean forceUpdate, String statusChangeRemark) {
        ToolingAsset existing = toolingAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + id));

        boolean codeChanged = !existing.getToolingCode().equals(asset.getToolingCode());
        if (codeChanged) {
            if (toolingAssetRepository.findByToolingCode(asset.getToolingCode()).isPresent()) {
                throw new RuntimeException("工装编号已存在: " + asset.getToolingCode());
            }
        }

        asset.setId(id);
        if (!forceUpdate) {
            DuplicateCheckResult check = checkDuplicate(asset);
            if (check.isDuplicate() && !check.isCodeDuplicate()) {
                throw new DuplicateWarningException(check.getMessage(), check.getSimilarAssets());
            }
            if (check.isCodeDuplicate()) {
                throw new RuntimeException("工装编号已存在: " + asset.getToolingCode());
            }
        }

        boolean workstationChanged = (existing.getWorkstation() == null && asset.getWorkstation() != null)
                || (existing.getWorkstation() != null && !existing.getWorkstation().equals(asset.getWorkstation()));
        boolean statusChanged = (existing.getStatus() == null && asset.getStatus() != null)
                || (existing.getStatus() != null && !existing.getStatus().equals(asset.getStatus()));
        boolean statusChangedToScrapped = !ToolingStatus.SCRAPPED.equals(existing.getStatus())
                && ToolingStatus.SCRAPPED.equals(asset.getStatus());
        boolean statusChangedFromScrapped = ToolingStatus.SCRAPPED.equals(existing.getStatus())
                && !ToolingStatus.SCRAPPED.equals(asset.getStatus());

        if (workstationChanged || statusChanged) {
            if (statusChangeRemark == null || statusChangeRemark.trim().isEmpty()) {
                throw new BusinessException("状态或工位发生变化时，状态变更说明不能为空");
            }
        }

        if (workstationChanged && !statusChangedToScrapped) {
            workstationCapacityService.validateCapacityForAdd(asset.getWorkstation());
        } else if (!workstationChanged && statusChangedFromScrapped) {
            workstationCapacityService.validateCapacityForAdd(asset.getWorkstation());
        }

        existing.setToolingCode(asset.getToolingCode());
        existing.setProductName(asset.getProductName());
        existing.setWorkstation(asset.getWorkstation());
        existing.setEntryDate(asset.getEntryDate());
        existing.setStatus(asset.getStatus());
        existing.setImageUrl(asset.getImageUrl());
        existing.setRemark(asset.getRemark());
        if (workstationChanged || statusChanged) {
            existing.setLastStatusChangeRemark(statusChangeRemark);
        }
        existing.setUpdateTime(LocalDateTime.now());
        return toolingAssetRepository.save(existing);
    }

    public void deleteAsset(Long id) {
        ToolingAsset asset = toolingAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + id));
        String toolingCode = asset.getToolingCode();
        transferRecordRepository.deleteByToolingCode(toolingCode);
        toolingInventoryDiffRepository.deleteByToolingCode(toolingCode);
        scrapRecordRepository.deleteByToolingCode(toolingCode);
        toolingAssetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public ToolingAsset getAsset(Long id) {
        return toolingAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<ToolingAsset> listAssets(ToolingStatus status, String workstation, String keyword) {
        Specification<ToolingAsset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (workstation != null && !workstation.isEmpty()) {
                predicates.add(cb.equal(root.get("workstation"), workstation));
            }
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(root.get("toolingCode"), "%" + keyword + "%"),
                        cb.like(root.get("productName"), "%" + keyword + "%")
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return toolingAssetRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Map<ToolingStatus, Long> countByStatus() {
        List<ToolingAsset> all = toolingAssetRepository.findAll();
        return all.stream().collect(Collectors.groupingBy(ToolingAsset::getStatus, Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getWorkstationSummary() {
        List<ToolingAsset> all = toolingAssetRepository.findAll();
        return all.stream().collect(Collectors.groupingBy(ToolingAsset::getWorkstation, Collectors.counting()));
    }
}
