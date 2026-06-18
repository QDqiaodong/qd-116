package com.tooling.asset;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ToolingAssetService {

    private final ToolingAssetRepository toolingAssetRepository;

    public ToolingAsset createAsset(ToolingAsset asset) {
        if (toolingAssetRepository.findByToolingCode(asset.getToolingCode()).isPresent()) {
            throw new RuntimeException("工装编号已存在: " + asset.getToolingCode());
        }
        asset.setCreateTime(LocalDateTime.now());
        asset.setUpdateTime(LocalDateTime.now());
        return toolingAssetRepository.save(asset);
    }

    public ToolingAsset updateAsset(Long id, ToolingAsset asset) {
        ToolingAsset existing = toolingAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工装不存在: " + id));
        existing.setToolingCode(asset.getToolingCode());
        existing.setProductName(asset.getProductName());
        existing.setWorkstation(asset.getWorkstation());
        existing.setEntryDate(asset.getEntryDate());
        existing.setStatus(asset.getStatus());
        existing.setImageUrl(asset.getImageUrl());
        existing.setRemark(asset.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        return toolingAssetRepository.save(existing);
    }

    public void deleteAsset(Long id) {
        toolingAssetRepository.deleteById(id);
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
