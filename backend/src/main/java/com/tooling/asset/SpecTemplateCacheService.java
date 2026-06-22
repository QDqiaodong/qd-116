package com.tooling.asset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SpecTemplateCacheService {

    private static final String KEY_PREFIX = "tooling:spec:template";

    private static final Set<String> ALLOWED_FIELD_TYPES = Set.of(
            "text", "textarea", "number", "select", "date", "boolean"
    );

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveTemplate(String category, List<SpecField> fields) {
        if (category == null || category.trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
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

        String key = KEY_PREFIX + ":" + category.trim();
        try {
            String json = objectMapper.writeValueAsString(validFields);
            redisTemplate.opsForValue().set(key, json);
        } catch (Exception e) {
            throw new BusinessException("保存规格模板失败: " + e.getMessage());
        }
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

    public void deleteTemplate(String category) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        String key = KEY_PREFIX + ":" + category.trim();
        redisTemplate.delete(key);
    }

    private static final String VERSION_KEY_PREFIX = "tooling:spec:template:version";
    private static final String VERSIONS_INDEX_PREFIX = "tooling:spec:template:versions";
    private static final String CURRENT_VERSION_PREFIX = "tooling:spec:template:current-version";

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
                .collect(java.util.stream.Collectors.toSet());
    }
}
