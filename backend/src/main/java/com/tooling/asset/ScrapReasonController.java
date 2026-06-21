package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scrap-reason")
@RequiredArgsConstructor
public class ScrapReasonController {

    private final ScrapReasonService scrapReasonService;

    @PostMapping
    public Result<ScrapReason> create(@RequestBody ScrapReason scrapReason) {
        return Result.ok(scrapReasonService.create(scrapReason));
    }

    @PutMapping("/{id}")
    public Result<ScrapReason> update(
            @PathVariable Long id,
            @RequestBody ScrapReason scrapReason) {
        return Result.ok(scrapReasonService.update(id, scrapReason));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scrapReasonService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/enable")
    public Result<ScrapReason> enable(@PathVariable Long id) {
        return Result.ok(scrapReasonService.enable(id));
    }

    @PutMapping("/{id}/disable")
    public Result<ScrapReason> disable(@PathVariable Long id) {
        return Result.ok(scrapReasonService.disable(id));
    }

    @GetMapping("/{id}")
    public Result<ScrapReason> getById(@PathVariable Long id) {
        return Result.ok(scrapReasonService.getById(id));
    }

    @GetMapping("/code/{reasonCode}")
    public Result<ScrapReason> getByCode(@PathVariable String reasonCode) {
        return Result.ok(scrapReasonService.getByCode(reasonCode));
    }

    @GetMapping("/list")
    public Result<List<ScrapReason>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabledOnly) {
        if (Boolean.TRUE.equals(enabledOnly)) {
            return Result.ok(scrapReasonService.listEnabledByCategory(category));
        }
        return Result.ok(scrapReasonService.listByCategory(category));
    }

    @PostMapping("/init")
    public Result<Void> initDefault() {
        scrapReasonService.initDefaultReasons();
        return Result.ok();
    }
}
