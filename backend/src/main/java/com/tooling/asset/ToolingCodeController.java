package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code-rule")
@RequiredArgsConstructor
public class ToolingCodeController {

    private final ToolingCodeService toolingCodeService;

    @GetMapping("/locator-block/next")
    public Result<String> getNextLocatorBlockCode() {
        String nextCode = toolingCodeService.generateNextLocatorBlockCode();
        return Result.ok(nextCode);
    }

    @PostMapping("/locator-block/validate")
    public Result<CodeValidationResult> validateLocatorBlockCode(@RequestParam String toolingCode) {
        CodeValidationResult result = toolingCodeService.validateLocatorBlockCode(toolingCode);
        return Result.ok(result);
    }

    @GetMapping("/locator-block/info")
    public Result<ToolingCodeService.CodeGenerationInfo> getLocatorBlockCodeInfo() {
        ToolingCodeService.CodeGenerationInfo info = toolingCodeService.getCodeGenerationInfo();
        return Result.ok(info);
    }
}
