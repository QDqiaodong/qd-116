package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scrap")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping
    public ResponseEntity<Result<?>> scrap(
            @RequestParam String toolingCode,
            @RequestParam String scrapReason,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scrapDate,
            @RequestParam String operator,
            @RequestParam(required = false) String remark,
            @RequestParam String statusChangeRemark) {
        try {
            ScrapRecord created = scrapService.scrap(toolingCode, scrapReason, scrapDate, operator, remark, statusChangeRemark);
            return ResponseEntity.ok(Result.ok(created));
        } catch (ScrapDuplicateException e) {
            ScrapRecord existing = e.getExistingRecord();
            Map<String, Object> data = null;
            if (existing != null) {
                data = Map.of(
                        "toolingCode", existing.getToolingCode(),
                        "scrapDate", existing.getScrapDate(),
                        "scrapReason", existing.getScrapReason(),
                        "operator", existing.getOperator(),
                        "remark", existing.getRemark() != null ? existing.getRemark() : ""
                );
            }
            Result<Map<String, Object>> result = new Result<>(409, e.getMessage(), data);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
    }

    @GetMapping("/list")
    public Result<List<ScrapRecord>> listAll() {
        return Result.ok(scrapService.listAll());
    }

    @GetMapping("/{toolingCode}")
    public Result<List<ScrapRecord>> listByTooling(@PathVariable String toolingCode) {
        return Result.ok(scrapService.listByTooling(toolingCode));
    }

    @GetMapping("/summary/{toolingCode}")
    public Result<ScrapSummaryVO> getScrapSummary(@PathVariable String toolingCode) {
        return Result.ok(scrapService.getScrapSummary(toolingCode));
    }
}
