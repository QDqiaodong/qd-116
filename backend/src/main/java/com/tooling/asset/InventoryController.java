package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public Result<InventoryCheck> check(
            @RequestParam String checkMonth,
            @RequestParam Integer totalBook,
            @RequestParam Integer totalActual,
            @RequestParam String checker,
            @RequestParam(required = false) String remark) {
        return Result.ok(inventoryService.check(checkMonth, totalBook, totalActual, checker, remark));
    }

    @GetMapping("/list")
    public Result<List<InventoryCheck>> listAll() {
        return Result.ok(inventoryService.listAll());
    }

    @GetMapping("/latest")
    public Result<InventoryCheck> latest() {
        return Result.ok(inventoryService.getLatestCheck().orElse(null));
    }
}
