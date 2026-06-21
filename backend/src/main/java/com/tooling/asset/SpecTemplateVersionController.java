package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/spec-template-version")
@RequiredArgsConstructor
public class SpecTemplateVersionController {

    private final SpecTemplateVersionService specTemplateVersionService;

    @PostMapping("/{category}")
    public Result<SpecTemplateVersion> saveTemplateWithVersion(
            @PathVariable String category,
            @RequestBody List<SpecField> fields) {
        SpecTemplateVersion versioned = specTemplateVersionService.saveTemplateWithVersion(category, fields);
        return Result.ok(versioned);
    }

    @GetMapping("/{category}")
    public Result<List<SpecField>> getCurrentTemplate(@PathVariable String category) {
        return Result.ok(specTemplateVersionService.getTemplate(category));
    }

    @GetMapping("/{category}/version/{version}")
    public Result<SpecTemplateVersion> getTemplateByVersion(
            @PathVariable String category,
            @PathVariable Integer version) {
        SpecTemplateVersion versioned = specTemplateVersionService.getTemplateByVersion(category, version);
        if (versioned == null) {
            return Result.fail("指定的模板版本不存在");
        }
        return Result.ok(versioned);
    }

    @GetMapping("/{category}/latest")
    public Result<SpecTemplateVersion> getLatestVersion(@PathVariable String category) {
        SpecTemplateVersion versioned = specTemplateVersionService.getLatestVersion(category);
        if (versioned == null) {
            return Result.fail("该分类没有模板版本");
        }
        return Result.ok(versioned);
    }

    @GetMapping("/{category}/current-version")
    public Result<Integer> getCurrentVersionNumber(@PathVariable String category) {
        Integer version = specTemplateVersionService.getCurrentVersionNumber(category);
        if (version == null) {
            return Result.fail("该分类没有模板版本");
        }
        return Result.ok(version);
    }

    @GetMapping("/{category}/versions")
    public Result<List<SpecTemplateVersionSummary>> listVersionSummaries(@PathVariable String category) {
        return Result.ok(specTemplateVersionService.listVersionSummaries(category));
    }

    @PutMapping("/{category}/rollback/{version}")
    public Result<SpecTemplateVersion> rollbackToVersion(
            @PathVariable String category,
            @PathVariable Integer version) {
        SpecTemplateVersion rolledBack = specTemplateVersionService.rollbackToVersion(category, version);
        return Result.ok(rolledBack);
    }

    @DeleteMapping("/{category}")
    public Result<Void> deleteTemplate(@PathVariable String category) {
        specTemplateVersionService.deleteTemplate(category);
        return Result.ok();
    }

    @DeleteMapping("/{category}/version/{version}")
    public Result<Void> deleteSpecificVersion(
            @PathVariable String category,
            @PathVariable Integer version) {
        specTemplateVersionService.deleteSpecificVersion(category, version);
        return Result.ok();
    }

    @GetMapping("/categories")
    public Result<Set<String>> listCategories() {
        return Result.ok(specTemplateVersionService.listCategories());
    }
}
