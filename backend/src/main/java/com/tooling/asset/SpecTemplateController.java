package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/spec-template")
@RequiredArgsConstructor
public class SpecTemplateController {

    private final SpecTemplateCacheService specTemplateCacheService;

    @PostMapping("/{category}")
    public Result<Void> saveTemplate(@PathVariable String category, @RequestBody Map<String, String> specMap) {
        specTemplateCacheService.saveTemplate(category, specMap);
        return Result.ok();
    }

    @GetMapping("/{category}")
    public Result<Map<Object, Object>> getTemplate(@PathVariable String category) {
        return Result.ok(specTemplateCacheService.getTemplate(category));
    }

    @DeleteMapping("/{category}")
    public Result<Void> deleteTemplate(@PathVariable String category) {
        specTemplateCacheService.deleteTemplate(category);
        return Result.ok();
    }

    @GetMapping("/categories")
    public Result<Set<String>> listCategories() {
        return Result.ok(specTemplateCacheService.listCategories());
    }
}
