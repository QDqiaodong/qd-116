package com.tooling.asset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecTemplateVersionService {

    private static final String KEY_PREFIX = "tooling:spec:template";
    private static final String VERSION_KEY_PREFIX = "tooling:spec:template:version";
    private static final String VERSIONS_INDEX_PREFIX = "tooling:spec:template:versions";
    private static final String CURRENT_VERSION_PREFIX = "tooling:spec:template:current-version";

    private static final Set<String> ALLOWED_FIELD_TYPES = Set.of(
            "text", "textarea", "number", "select", "date", "boolean"
    );

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = createObjectMapper();

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public SpecTemplateVersion saveTemplateWithVersion(String category, List<SpecField> fields) {
        if (category == null || category.trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        String trimmedCategory = category.trim();

        List<SpecField> validFields = validateAndCleanFields(fields);

        Integer newVersion = incrementVersion(trimmedCategory);

        LocalDateTime now = LocalDateTime.now();

        SpecTemplateVersion versionedTemplate = SpecTemplateVersion.builder()
                .category(trimmedCategory)
                .version(newVersion)
                .fields(validFields)
                .updateTime(now)
                .build();

        saveVersion(trimmedCategory, newVersion, versionedTemplate);
        addToVersionIndex(trimmedCategory, newVersion);
        saveCurrentTemplate(trimmedCategory, validFields);

        return versionedTemplate;
    }

    private List<SpecField> validateAndCleanFields(List<SpecField> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("规格模板字段列表不能为空");
        }

        Set<String> fieldNames = new HashSet<>();
        List<SpecField> validFields = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            SpecField field = fields.get(i);
            int index = i + 1;

            if (field == null) {
                throw new BusinessException("第 " + index + " 个字段为空");
            }

            String fieldName = field.getFieldName();
            if (fieldName == null || fieldName.trim().isEmpty()) {
                throw new BusinessException("第 " + index + " 个字段的字段名不能为空");
            }
            fieldName = fieldName.trim();
            if (!FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
                throw new BusinessException("第 " + index + " 个字段的字段名只能包含字母、数字、下划线和中文");
            }
            if (fieldNames.contains(fieldName)) {
                throw new BusinessException("存在重复的字段名: " + fieldName);
            }
            fieldNames.add(fieldName);

            String fieldType = field.getFieldType();
            if (fieldType == null || fieldType.trim().isEmpty()) {
                throw new BusinessException("字段「" + fieldName + "」的字段类型不能为空");
            }
            fieldType = fieldType.trim().toLowerCase();
            if (!ALLOWED_FIELD_TYPES.contains(fieldType)) {
                throw new BusinessException("字段「" + fieldName + "」的字段类型不合法，仅允许: " + String.join("、", ALLOWED_FIELD_TYPES));
            }

            Boolean required = field.getRequired();
            if (required == null) {
                throw new BusinessException("字段「" + fieldName + "」的必填项必须明确指定 true 或 false");
            }

            String defaultValue = field.getDefaultValue();
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                defaultValue = defaultValue.trim();
                validateDefaultValue(fieldName, fieldType, defaultValue);
            } else {
                defaultValue = "";
            }

            String label = field.getLabel();
            if (label == null || label.trim().isEmpty()) {
                label = fieldName;
            } else {
                label = label.trim();
            }

            SpecField validField = SpecField.builder()
                    .fieldName(fieldName)
                    .fieldType(fieldType)
                    .required(required)
                    .defaultValue(defaultValue)
                    .label(label)
                    .build();
            validFields.add(validField);
        }

        return validFields;
    }

    private void validateDefaultValue(String fieldName, String fieldType, String defaultValue) {
        switch (fieldType) {
            case "number":
                try {
                    Double.parseDouble(defaultValue);
                } catch (NumberFormatException e) {
                    throw new BusinessException("字段「" + fieldName + "」的默认值不是有效的数字");
                }
                break;
            case "boolean":
                if (!"true".equalsIgnoreCase(defaultValue) && !"false".equalsIgnoreCase(defaultValue)) {
                    throw new BusinessException("字段「" + fieldName + "」的默认值只能是 true 或 false");
                }
                break;
            case "date":
                if (!defaultValue.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    throw new BusinessException("字段「" + fieldName + "」的默认值日期格式不正确，应为 YYYY-MM-DD");
                }
                break;
            case "text":
            case "textarea":
            case "select":
                break;
            default:
                throw new BusinessException("字段「" + fieldName + "」的字段类型不支持");
        }
    }

    private Integer incrementVersion(String category) {
        String versionKey = CURRENT_VERSION_PREFIX + ":" + category;
        Long incremented = redisTemplate.opsForValue().increment(versionKey);
        if (incremented == null) {
            return 1;
        }
        return incremented.intValue();
    }

    private void saveVersion(String category, Integer version, SpecTemplateVersion templateVersion) {
        String key = VERSION_KEY_PREFIX + ":" + category + ":" + version;
        try {
            String json = objectMapper.writeValueAsString(templateVersion);
            redisTemplate.opsForValue().set(key, json);
        } catch (Exception e) {
            throw new BusinessException("保存规格模板版本失败: " + e.getMessage());
        }
    }

    private void addToVersionIndex(String category, Integer version) {
        String indexKey = VERSIONS_INDEX_PREFIX + ":" + category;
        redisTemplate.opsForZSet().add(indexKey, String.valueOf(version), version.doubleValue());
    }

    private void saveCurrentTemplate(String category, List<SpecField> fields) {
        String key = KEY_PREFIX + ":" + category;
        try {
            String json = objectMapper.writeValueAsString(fields);
            redisTemplate.opsForValue().set(key, json);
        } catch (Exception e) {
            throw new BusinessException("保存当前规格模板失败: " + e.getMessage());
        }
    }

    public SpecTemplateVersion getTemplateByVersion(String category, Integer version) {
        if (category == null || category.trim().isEmpty() || version == null) {
            return null;
        }
        String key = VERSION_KEY_PREFIX + ":" + category.trim() + ":" + version;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            String json = value.toString();
            return objectMapper.readValue(json, SpecTemplateVersion.class);
        } catch (Exception e) {
            return null;
        }
    }

    public SpecTemplateVersion getLatestVersion(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        String trimmedCategory = category.trim();
        Integer currentVersion = getCurrentVersionNumber(trimmedCategory);
        if (currentVersion == null) {
            return null;
        }
        return getTemplateByVersion(trimmedCategory, currentVersion);
    }

    public Integer getCurrentVersionNumber(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        String versionKey = CURRENT_VERSION_PREFIX + ":" + category.trim();
        Object value = redisTemplate.opsForValue().get(versionKey);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<SpecField> getTemplate(String category) {
        if (category == null || category.trim().isEmpty()) {
            return List.of();
        }
        String key = KEY_PREFIX + ":" + category.trim();
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return List.of();
        }
        try {
            String json = value.toString();
            return objectMapper.readValue(json, new TypeReference<List<SpecField>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<SpecTemplateVersionSummary> listVersionSummaries(String category) {
        if (category == null || category.trim().isEmpty()) {
            return List.of();
        }
        String trimmedCategory = category.trim();
        String indexKey = VERSIONS_INDEX_PREFIX + ":" + trimmedCategory;
        Set<Object> versionStrings = redisTemplate.opsForZSet().reverseRange(indexKey, 0, -1);
        if (versionStrings == null || versionStrings.isEmpty()) {
            return List.of();
        }

        return versionStrings.stream()
                .map(versionStr -> {
                    try {
                        Integer version = Integer.parseInt(versionStr.toString());
                        SpecTemplateVersion versioned = getTemplateByVersion(trimmedCategory, version);
                        if (versioned != null) {
                            return SpecTemplateVersionSummary.builder()
                                    .category(trimmedCategory)
                                    .version(version)
                                    .updateTime(versioned.getUpdateTime())
                                    .fieldCount(versioned.getFields() != null ? versioned.getFields().size() : 0)
                                    .build();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    return null;
                })
                .filter(summary -> summary != null)
                .sorted(Comparator.comparing(SpecTemplateVersionSummary::getVersion).reversed())
                .collect(Collectors.toList());
    }

    public void deleteTemplate(String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        String trimmedCategory = category.trim();

        String currentTemplateKey = KEY_PREFIX + ":" + trimmedCategory;
        redisTemplate.delete(currentTemplateKey);

        String versionCounterKey = CURRENT_VERSION_PREFIX + ":" + trimmedCategory;
        redisTemplate.delete(versionCounterKey);

        String indexKey = VERSIONS_INDEX_PREFIX + ":" + trimmedCategory;
        Set<Object> versionStrings = redisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (versionStrings != null) {
            for (Object versionStr : versionStrings) {
                String versionKey = VERSION_KEY_PREFIX + ":" + trimmedCategory + ":" + versionStr.toString();
                redisTemplate.delete(versionKey);
            }
        }
        redisTemplate.delete(indexKey);
    }

    public void deleteSpecificVersion(String category, Integer version) {
        if (category == null || category.trim().isEmpty() || version == null) {
            return;
        }
        String trimmedCategory = category.trim();

        Integer currentVersion = getCurrentVersionNumber(trimmedCategory);
        if (currentVersion != null && version.equals(currentVersion)) {
            throw new BusinessException("不能删除当前最新版本，请先回滚到其他版本或删除整个模板");
        }

        String versionKey = VERSION_KEY_PREFIX + ":" + trimmedCategory + ":" + version;
        redisTemplate.delete(versionKey);

        String indexKey = VERSIONS_INDEX_PREFIX + ":" + trimmedCategory;
        redisTemplate.opsForZSet().remove(indexKey, String.valueOf(version));
    }

    public SpecTemplateVersion rollbackToVersion(String category, Integer version) {
        if (category == null || category.trim().isEmpty() || version == null) {
            throw new BusinessException("分类和版本号不能为空");
        }
        String trimmedCategory = category.trim();

        SpecTemplateVersion targetVersion = getTemplateByVersion(trimmedCategory, version);
        if (targetVersion == null) {
            throw new BusinessException("目标版本不存在: " + version);
        }

        return saveTemplateWithVersion(trimmedCategory, targetVersion.getFields());
    }

    public Set<String> listCategories() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + ":*");
        if (keys == null) {
            return Set.of();
        }
        return keys.stream()
                .filter(key -> !key.startsWith(VERSION_KEY_PREFIX + ":")
                        && !key.startsWith(VERSIONS_INDEX_PREFIX + ":")
                        && !key.startsWith(CURRENT_VERSION_PREFIX + ":"))
                .map(key -> key.substring(KEY_PREFIX.length() + 1))
                .collect(Collectors.toSet());
    }
}
