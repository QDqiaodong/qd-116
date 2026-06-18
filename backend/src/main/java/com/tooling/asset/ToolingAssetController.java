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
import java.util.Map;

@RestController
@RequestMapping("/api/tooling")
@RequiredArgsConstructor
public class ToolingAssetController {

    private final ToolingAssetService toolingAssetService;

    @PostMapping
    public Result<ToolingAsset> create(@RequestBody ToolingAsset asset) {
        return Result.ok(toolingAssetService.createAsset(asset));
    }

    @PutMapping("/{id}")
    public Result<ToolingAsset> update(@PathVariable Long id, @RequestBody ToolingAsset asset) {
        return Result.ok(toolingAssetService.updateAsset(id, asset));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        toolingAssetService.deleteAsset(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<ToolingAsset> getById(@PathVariable Long id) {
        return Result.ok(toolingAssetService.getAsset(id));
    }

    @GetMapping("/list")
    public Result<List<ToolingAsset>> list(
            @RequestParam(required = false) ToolingStatus status,
            @RequestParam(required = false) String workstation,
            @RequestParam(required = false) String keyword) {
        return Result.ok(toolingAssetService.listAssets(status, workstation, keyword));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<ToolingStatus, Long> statusCount = toolingAssetService.countByStatus();
        Map<String, Long> workstationSummary = toolingAssetService.getWorkstationSummary();
        return Result.ok(Map.of("statusCount", statusCount, "workstationSummary", workstationSummary));
    }
}
