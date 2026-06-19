package com.tooling.asset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @GetMapping("/{toolingCode}")
    public Result<ToolingTraceVO> getTrace(@PathVariable String toolingCode) {
        return Result.ok(traceService.getTraceByToolingCode(toolingCode));
    }
}
