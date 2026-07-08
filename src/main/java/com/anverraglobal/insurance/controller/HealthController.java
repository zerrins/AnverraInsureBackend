package com.anverraglobal.insurance.controller;

import com.anverraglobal.insurance.model.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", "OK"));
    }

    @GetMapping("/liveness")
    public ResponseEntity<ApiResponse<String>> liveness() {
        return ResponseEntity.ok(ApiResponse.success("Liveness check passed", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiResponse<String>> readiness() {
        return ResponseEntity.ok(ApiResponse.success("Readiness check passed", "READY"));
    }
}
