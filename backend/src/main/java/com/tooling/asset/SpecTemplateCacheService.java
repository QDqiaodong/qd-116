package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecTemplateCacheService {

    private static final String KEY_PREFIX = "tooling:spec:template";

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveTemplate(String category, Map<String, String> specMap) {
        String key = KEY_PREFIX + ":" + category;
        Map<String, Object> hashMap = new HashMap<>(specMap);
        redisTemplate.opsForHash().putAll(key, hashMap);
    }

    public Map<Object, Object> getTemplate(String category) {
        String key = KEY_PREFIX + ":" + category;
        return redisTemplate.opsForHash().entries(key);
    }

    public void deleteTemplate(String category) {
        String key = KEY_PREFIX + ":" + category;
        redisTemplate.delete(key);
    }

    public Set<String> listCategories() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + ":*");
        if (keys == null) {
            return Set.of();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length() + 1))
                .collect(java.util.stream.Collectors.toSet());
    }
}
