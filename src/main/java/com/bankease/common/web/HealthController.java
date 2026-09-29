package com.bankease.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "bankease-net-banking");
        body.put("time", Instant.now().toString());
        body.put("docs", "/swagger-ui.html");
        return body;
    }
}
