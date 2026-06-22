package com.tooling.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemConfigController {

    @Value("${system.enable-date:2024-01-01}")
    private String systemEnableDate;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entryDateMin", systemEnableDate);
        data.put("today", LocalDate.now().toString());
        return Result.ok(data);
    }
}
