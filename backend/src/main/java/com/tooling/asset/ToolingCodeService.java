package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class ToolingCodeService {

    private static final String LOCATOR_BLOCK_PREFIX = "TL";
    private static final String CODE_PATTERN = "^TL-\\d{4}-\\d{3}$";
    private static final Pattern CODE_REGEX = Pattern.compile(CODE_PATTERN);
    private static final int SEQUENCE_DIGITS = 3;
    private static final int INITIAL_SEQUENCE = 1;

    private final ToolingAssetRepository toolingAssetRepository;

    public String generateNextLocatorBlockCode() {
        int year = LocalDate.now().getYear();
        String prefix = LOCATOR_BLOCK_PREFIX + "-" + year + "-";
        String maxCode = toolingAssetRepository.findMaxCodeByPrefix(prefix);

        int nextSequence = INITIAL_SEQUENCE;
        if (maxCode != null && !maxCode.isEmpty()) {
            String sequenceStr = maxCode.substring(prefix.length());
            try {
                int currentSeq = Integer.parseInt(sequenceStr);
                nextSequence = currentSeq + 1;
            } catch (NumberFormatException e) {
                nextSequence = INITIAL_SEQUENCE;
            }
        }

        return prefix + String.format("%0" + SEQUENCE_DIGITS + "d", nextSequence);
    }

    public CodeValidationResult validateLocatorBlockCode(String toolingCode) {
        return validateLocatorBlockCode(toolingCode, null);
    }

    public CodeValidationResult validateLocatorBlockCode(String toolingCode, Long excludeId) {
        if (toolingCode == null || toolingCode.trim().isEmpty()) {
            return CodeValidationResult.builder()
                    .valid(false)
                    .formatValid(false)
                    .exists(false)
                    .message("工装编号不能为空")
                    .build();
        }

        String trimmedCode = toolingCode.trim();
        boolean formatValid = CODE_REGEX.matcher(trimmedCode).matches();

        if (!formatValid) {
            String suggestedCode = generateNextLocatorBlockCode();
            return CodeValidationResult.builder()
                    .valid(false)
                    .formatValid(false)
                    .exists(false)
                    .message("编号格式不正确，定位块编号格式应为 TL-YYYY-XXX（如 TL-2024-001）")
                    .suggestedCode(suggestedCode)
                    .build();
        }

        Matcher matcher = CODE_REGEX.matcher(trimmedCode);
        if (matcher.matches()) {
            String yearStr = trimmedCode.substring(3, 7);
            int year = Integer.parseInt(yearStr);
            int currentYear = LocalDate.now().getYear();
            if (year < 2000 || year > currentYear + 1) {
                return CodeValidationResult.builder()
                        .valid(false)
                        .formatValid(false)
                        .exists(false)
                        .message("年份不合理，应在 2000 年至 " + (currentYear + 1) + " 年之间")
                        .build();
            }
        }

        boolean exists;
        if (excludeId != null) {
            exists = toolingAssetRepository.findByToolingCode(trimmedCode)
                    .filter(a -> !a.getId().equals(excludeId))
                    .isPresent();
        } else {
            exists = toolingAssetRepository.existsByToolingCode(trimmedCode);
        }

        if (exists) {
            String suggestedCode = generateNextLocatorBlockCode();
            return CodeValidationResult.builder()
                    .valid(false)
                    .formatValid(true)
                    .exists(true)
                    .message("该编号已存在，请更换编号")
                    .suggestedCode(suggestedCode)
                    .build();
        }

        return CodeValidationResult.builder()
                .valid(true)
                .formatValid(true)
                .exists(false)
                .message("编号校验通过")
                .build();
    }

    public boolean isLocatorBlockCode(String toolingCode) {
        if (toolingCode == null) {
            return false;
        }
        return CODE_REGEX.matcher(toolingCode.trim()).matches();
    }

    public CodeGenerationInfo getCodeGenerationInfo() {
        String nextCode = generateNextLocatorBlockCode();
        String prefix = LOCATOR_BLOCK_PREFIX + "-" + LocalDate.now().getYear() + "-";
        String maxCode = toolingAssetRepository.findMaxCodeByPrefix(prefix);

        int currentMax = 0;
        if (maxCode != null && !maxCode.isEmpty()) {
            try {
                currentMax = Integer.parseInt(maxCode.substring(prefix.length()));
            } catch (NumberFormatException e) {
                currentMax = 0;
            }
        }

        return CodeGenerationInfo.builder()
                .prefix(LOCATOR_BLOCK_PREFIX)
                .pattern("TL-YYYY-XXX")
                .description("定位块编码规则：TL(定位块前缀) + 年份 + 3位流水号")
                .nextCode(nextCode)
                .currentYear(LocalDate.now().getYear())
                .currentMaxSequence(currentMax)
                .totalCount(currentMax)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CodeGenerationInfo {
        private String prefix;
        private String pattern;
        private String description;
        private String nextCode;
        private int currentYear;
        private int currentMaxSequence;
        private int totalCount;
    }
}
