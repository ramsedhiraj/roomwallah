package com.roomwallah.plugin;

import com.roomwallah.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/plugins")
@RequiredArgsConstructor
@Tag(name = "Plugin Manager", description = "Admin endpoints for installing and managing system extensions")
public class PluginController {

    private final PluginService pluginService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all installed plugins and their lifecycle status")
    public ApiResponse<List<ActivePlugin>> list() {
        log.info("Admin requested installed plugins list");
        return ApiResponse.success(pluginService.getInstalledPlugins(), "Installed plugins list retrieved");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register and install a new plugin")
    public ApiResponse<ActivePlugin> install(@RequestBody Map<String, String> payload) {
        String id = payload.get("id");
        String name = payload.get("name");
        String version = payload.get("version");
        String permissions = payload.get("permissions");

        ActivePlugin plugin = pluginService.installPlugin(id, name, version, permissions);
        return ApiResponse.success(plugin, "Plugin installed successfully");
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate an installed plugin")
    public ApiResponse<ActivePlugin> activate(@PathVariable String id) {
        log.info("Admin activating plugin ID: {}", id);
        ActivePlugin plugin = pluginService.activatePlugin(id);
        return ApiResponse.success(plugin, "Plugin activated successfully");
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an active plugin")
    public ApiResponse<ActivePlugin> deactivate(@PathVariable String id) {
        log.info("Admin deactivating plugin ID: {}", id);
        ActivePlugin plugin = pluginService.deactivatePlugin(id);
        return ApiResponse.success(plugin, "Plugin deactivated successfully");
    }
}
