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
@RequestMapping("/api/workstation-capacity")
@RequiredArgsConstructor
public class WorkstationCapacityController {

    private final WorkstationCapacityService workstationCapacityService;

    @PostMapping
    public Result<WorkstationCapacity> create(@RequestBody WorkstationCapacity capacity) {
        return Result.ok(workstationCapacityService.createCapacity(capacity));
    }

    @PutMapping("/{id}")
    public Result<WorkstationCapacity> update(
            @PathVariable Long id,
            @RequestBody WorkstationCapacity capacity) {
        return Result.ok(workstationCapacityService.updateCapacity(id, capacity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workstationCapacityService.deleteCapacity(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<WorkstationCapacity> getById(@PathVariable Long id) {
        return Result.ok(workstationCapacityService.getCapacity(id));
    }

    @GetMapping("/list")
    public Result<List<WorkstationCapacity>> list(
            @RequestParam(required = false) String region) {
        if (region != null && !region.isEmpty()) {
            return Result.ok(workstationCapacityService.listByRegion(region));
        }
        return Result.ok(workstationCapacityService.listAllCapacities());
    }

    @GetMapping("/check")
    public Result<CapacityCheckResult> checkCapacity(@RequestParam String workstation) {
        return Result.ok(workstationCapacityService.checkCapacity(workstation));
    }

    @GetMapping("/names")
    public Result<List<String>> listWorkstationNames() {
        return Result.ok(workstationCapacityService.listWorkstationNames());
    }
}
