package com.roomwallah.partner.controller;

import com.roomwallah.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/developer/keys")
@RequiredArgsConstructor
public class DeveloperKeyController {

    private final PartnerService partnerService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createKey(
            @RequestParam String partnerName,
            @RequestParam(defaultValue = "read-only") String scopes,
            @RequestParam(defaultValue = "1000") int dailyQuota
    ) {
        String rawKey = partnerService.createApiKey(partnerName, scopes, dailyQuota);
        Map<String, String> response = new HashMap<>();
        response.put("apiKey", rawKey);
        response.put("message", "API key created successfully. Save it now, as it cannot be retrieved again!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rotate")
    public ResponseEntity<Map<String, String>> rotateKey(@PathVariable UUID id) {
        String newRawKey = partnerService.rotateKey(id);
        Map<String, String> response = new HashMap<>();
        response.put("apiKey", newRawKey);
        response.put("message", "API key rotated successfully. Save it now, as it cannot be retrieved again!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable UUID id) {
        partnerService.revokeKey(id);
        return ResponseEntity.ok().build();
    }
}
