package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public Result<TransferRecord> transfer(
            @RequestParam String toolingCode,
            @RequestParam String fromWorkstation,
            @RequestParam String toWorkstation,
            @RequestParam String operator,
            @RequestParam(required = false) String remark) {
        return Result.ok(transferService.transfer(toolingCode, fromWorkstation, toWorkstation, operator, remark));
    }

    @GetMapping("/list")
    public Result<List<TransferRecord>> listAll() {
        return Result.ok(transferService.listAll());
    }

    @GetMapping("/{toolingCode}")
    public Result<List<TransferRecord>> listByTooling(@PathVariable String toolingCode) {
        return Result.ok(transferService.listByTooling(toolingCode));
    }
}
