package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ScrapReasonService {

    public static final String CATEGORY_LOCATING_BLOCK = "LOCATING_BLOCK";
    public static final String CATEGORY_GENERAL = "GENERAL";

    private final ScrapReasonRepository scrapReasonRepository;

    public ScrapReason create(ScrapReason scrapReason) {
        if (scrapReason.getReasonCode() == null || scrapReason.getReasonCode().trim().isEmpty()) {
            throw new BusinessException("原因编码不能为空");
        }
        if (scrapReason.getReasonName() == null || scrapReason.getReasonName().trim().isEmpty()) {
            throw new BusinessException("原因名称不能为空");
        }
        if (scrapReasonRepository.existsByReasonCode(scrapReason.getReasonCode())) {
            throw new BusinessException("原因编码已存在: " + scrapReason.getReasonCode());
        }
        if (scrapReason.getEnabled() == null) {
            scrapReason.setEnabled(true);
        }
        if (scrapReason.getSortOrder() == null) {
            scrapReason.setSortOrder(0);
        }
        if (scrapReason.getCategory() == null || scrapReason.getCategory().trim().isEmpty()) {
            scrapReason.setCategory(CATEGORY_GENERAL);
        }
        scrapReason.setCreateTime(LocalDateTime.now());
        scrapReason.setUpdateTime(LocalDateTime.now());
        return scrapReasonRepository.save(scrapReason);
    }

    public ScrapReason update(Long id, ScrapReason scrapReason) {
        ScrapReason existing = scrapReasonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报废原因不存在: " + id));

        if (scrapReason.getReasonCode() != null && !scrapReason.getReasonCode().equals(existing.getReasonCode())) {
            if (scrapReasonRepository.existsByReasonCode(scrapReason.getReasonCode())) {
                throw new BusinessException("原因编码已存在: " + scrapReason.getReasonCode());
            }
            existing.setReasonCode(scrapReason.getReasonCode());
        }
        if (scrapReason.getReasonName() != null) {
            existing.setReasonName(scrapReason.getReasonName());
        }
        if (scrapReason.getCategory() != null) {
            existing.setCategory(scrapReason.getCategory());
        }
        if (scrapReason.getEnabled() != null) {
            existing.setEnabled(scrapReason.getEnabled());
        }
        if (scrapReason.getSortOrder() != null) {
            existing.setSortOrder(scrapReason.getSortOrder());
        }
        if (scrapReason.getRemark() != null) {
            existing.setRemark(scrapReason.getRemark());
        }
        existing.setUpdateTime(LocalDateTime.now());
        return scrapReasonRepository.save(existing);
    }

    public void delete(Long id) {
        if (!scrapReasonRepository.existsById(id)) {
            throw new BusinessException("报废原因不存在: " + id);
        }
        scrapReasonRepository.deleteById(id);
    }

    public ScrapReason enable(Long id) {
        ScrapReason existing = scrapReasonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报废原因不存在: " + id));
        existing.setEnabled(true);
        existing.setUpdateTime(LocalDateTime.now());
        return scrapReasonRepository.save(existing);
    }

    public ScrapReason disable(Long id) {
        ScrapReason existing = scrapReasonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报废原因不存在: " + id));
        existing.setEnabled(false);
        existing.setUpdateTime(LocalDateTime.now());
        return scrapReasonRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public ScrapReason getById(Long id) {
        return scrapReasonRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报废原因不存在: " + id));
    }

    @Transactional(readOnly = true)
    public ScrapReason getByCode(String reasonCode) {
        return scrapReasonRepository.findByReasonCode(reasonCode)
                .orElseThrow(() -> new BusinessException("报废原因不存在: " + reasonCode));
    }

    @Transactional(readOnly = true)
    public List<ScrapReason> listAll() {
        return scrapReasonRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<ScrapReason> listEnabled() {
        return scrapReasonRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<ScrapReason> listByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return listAll();
        }
        return scrapReasonRepository.findByCategoryOrderBySortOrderAsc(category);
    }

    @Transactional(readOnly = true)
    public List<ScrapReason> listEnabledByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return listEnabled();
        }
        return scrapReasonRepository.findByCategoryAndEnabledTrueOrderBySortOrderAsc(category);
    }

    public void initDefaultReasons() {
        long count = scrapReasonRepository.count();
        if (count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        ScrapReason r1 = ScrapReason.builder()
                .reasonCode("LB_WEAR")
                .reasonName("磨损")
                .category(CATEGORY_LOCATING_BLOCK)
                .enabled(true)
                .sortOrder(1)
                .remark("定位块长期使用造成磨损，尺寸超出公差")
                .createTime(now)
                .updateTime(now)
                .build();
        scrapReasonRepository.save(r1);

        ScrapReason r2 = ScrapReason.builder()
                .reasonCode("LB_FRACTURE")
                .reasonName("断裂")
                .category(CATEGORY_LOCATING_BLOCK)
                .enabled(true)
                .sortOrder(2)
                .remark("定位块发生断裂、裂纹或破损")
                .createTime(now)
                .updateTime(now)
                .build();
        scrapReasonRepository.save(r2);

        ScrapReason r3 = ScrapReason.builder()
                .reasonCode("LB_DIMENSION")
                .reasonName("尺寸偏差")
                .category(CATEGORY_LOCATING_BLOCK)
                .enabled(true)
                .sortOrder(3)
                .remark("定位块加工或安装尺寸偏差超出允许范围")
                .createTime(now)
                .updateTime(now)
                .build();
        scrapReasonRepository.save(r3);

        ScrapReason r4 = ScrapReason.builder()
                .reasonCode("LB_UNLOCATABLE")
                .reasonName("无法定位")
                .category(CATEGORY_LOCATING_BLOCK)
                .enabled(true)
                .sortOrder(4)
                .remark("定位块无法正常完成定位功能，定位精度不达标")
                .createTime(now)
                .updateTime(now)
                .build();
        scrapReasonRepository.save(r4);

        ScrapReason r5 = ScrapReason.builder()
                .reasonCode("LB_REPAIR_FAIL")
                .reasonName("维修失败")
                .category(CATEGORY_LOCATING_BLOCK)
                .enabled(true)
                .sortOrder(5)
                .remark("定位块经维修后仍无法满足使用要求")
                .createTime(now)
                .updateTime(now)
                .build();
        scrapReasonRepository.save(r5);
    }
}
