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
public class WorkstationCapacityService {

    private final WorkstationCapacityRepository workstationCapacityRepository;
    private final ToolingAssetRepository toolingAssetRepository;

    private String regionOf(String workstation) {
        if (workstation == null || workstation.isEmpty()) return "其他";
        if (workstation.startsWith("注塑机")) return "注塑机区";
        if (workstation.startsWith("模具库")) return "模具库";
        if (workstation.equals("待检区") || workstation.startsWith("待检")) return "待检区";
        return "其他";
    }

    public WorkstationCapacity createCapacity(WorkstationCapacity capacity) {
        if (workstationCapacityRepository.existsByWorkstation(capacity.getWorkstation())) {
            throw new BusinessException("工位容量配置已存在: " + capacity.getWorkstation());
        }
        if (capacity.getRegion() == null || capacity.getRegion().isEmpty()) {
            capacity.setRegion(regionOf(capacity.getWorkstation()));
        }
        capacity.setCreateTime(LocalDateTime.now());
        capacity.setUpdateTime(LocalDateTime.now());
        return workstationCapacityRepository.save(capacity);
    }

    public WorkstationCapacity updateCapacity(Long id, WorkstationCapacity capacity) {
        WorkstationCapacity existing = workstationCapacityRepository.findById(id)
                .orElseThrow(() -> new BusinessException("容量配置不存在: " + id));

        if (!existing.getWorkstation().equals(capacity.getWorkstation())) {
            if (workstationCapacityRepository.existsByWorkstation(capacity.getWorkstation())) {
                throw new BusinessException("工位容量配置已存在: " + capacity.getWorkstation());
            }
        }

        existing.setWorkstation(capacity.getWorkstation());
        existing.setRegion(capacity.getRegion() != null && !capacity.getRegion().isEmpty()
                ? capacity.getRegion() : regionOf(capacity.getWorkstation()));
        existing.setMaxCapacity(capacity.getMaxCapacity());
        existing.setRemark(capacity.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        return workstationCapacityRepository.save(existing);
    }

    public void deleteCapacity(Long id) {
        if (!workstationCapacityRepository.existsById(id)) {
            throw new BusinessException("容量配置不存在: " + id);
        }
        workstationCapacityRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public WorkstationCapacity getCapacity(Long id) {
        return workstationCapacityRepository.findById(id)
                .orElseThrow(() -> new BusinessException("容量配置不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<WorkstationCapacity> listAllCapacities() {
        return workstationCapacityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WorkstationCapacity> listByRegion(String region) {
        return workstationCapacityRepository.findByRegion(region);
    }

    @Transactional(readOnly = true)
    public CapacityCheckResult checkCapacity(String workstation) {
        if (workstation == null || workstation.isEmpty()) {
            return CapacityCheckResult.builder()
                    .sufficient(true)
                    .workstation(workstation)
                    .currentCount(0)
                    .maxCapacity(null)
                    .availableSlots(null)
                    .message("工位为空，不进行容量校验")
                    .build();
        }

        List<ToolingAsset> assets = toolingAssetRepository.findByWorkstation(workstation);
        int currentCount = (int) assets.stream()
                .filter(a -> !ToolingStatus.SCRAPPED.equals(a.getStatus()))
                .count();

        Optional<WorkstationCapacity> capacityOpt = workstationCapacityRepository.findByWorkstation(workstation);

        if (capacityOpt.isEmpty()) {
            return CapacityCheckResult.builder()
                    .sufficient(true)
                    .workstation(workstation)
                    .currentCount(currentCount)
                    .maxCapacity(null)
                    .availableSlots(null)
                    .message("工位[" + workstation + "]未配置容量限制，当前存放" + currentCount + "件")
                    .build();
        }

        WorkstationCapacity capacity = capacityOpt.get();
        int maxCapacity = capacity.getMaxCapacity();
        int availableSlots = maxCapacity - currentCount;
        boolean sufficient = availableSlots >= 0;

        String message;
        if (sufficient) {
            message = String.format("工位[%s]容量充足：当前%d件 / 上限%d件，剩余%d个空位",
                    workstation, currentCount, maxCapacity, availableSlots);
        } else {
            message = String.format("工位[%s]容量不足：当前%d件已超过上限%d件，超出%d件",
                    workstation, currentCount, maxCapacity, -availableSlots);
        }

        return CapacityCheckResult.builder()
                .sufficient(sufficient)
                .workstation(workstation)
                .currentCount(currentCount)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .message(message)
                .build();
    }

    public void validateCapacityForAdd(String workstation) {
        if (workstation == null || workstation.isEmpty()) {
            return;
        }

        List<ToolingAsset> assets = toolingAssetRepository.findByWorkstation(workstation);
        int currentCount = (int) assets.stream()
                .filter(a -> !ToolingStatus.SCRAPPED.equals(a.getStatus()))
                .count();

        Optional<WorkstationCapacity> capacityOpt = workstationCapacityRepository.findByWorkstation(workstation);

        if (capacityOpt.isPresent()) {
            WorkstationCapacity capacity = capacityOpt.get();
            int maxCapacity = capacity.getMaxCapacity();
            int availableSlots = maxCapacity - currentCount;

            if (availableSlots <= 0) {
                throw new BusinessException(String.format(
                        "工位[%s]容量不足，无法放入：当前%d件已达上限%d件，请先移走部分工装或调整工位容量配置",
                        workstation, currentCount, maxCapacity));
            }
        }
    }

    public void validateCapacityForTransfer(String toWorkstation) {
        validateCapacityForAdd(toWorkstation);
    }
}
