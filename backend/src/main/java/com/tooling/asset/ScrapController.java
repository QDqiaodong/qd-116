package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scrap")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping
    public Result<ScrapRecord> scrap(
            @RequestParam String toolingCode,
            @RequestParam String scrapReason,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scrapDate,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.ok(scrapService.scrap(toolingCode, scrapReason, scrapDate, operator, remark));
    }

    @GetMapping("/list")
    public Result<List<ScrapRecord>> listAll() {
        return Result.ok(scrapService.listAll());
    }

    @GetMapping("/{toolingCode}")
    public Result<List<ScrapRecord>> listByTooling(@PathVariable String toolingCode) {
        return Result.ok(scrapService.listByTooling(toolingCode));
    }
}
