package com.roomwallah.plugin;

import com.roomwallah.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private final ActivePluginRepository pluginRepository;

    @Transactional
    public ActivePlugin installPlugin(String id, String name, String version, String permissions) {
        log.info("Installing new system plugin: {} v{}", name, version);
        ActivePlugin plugin = ActivePlugin.builder()
                .id(id)
                .pluginName(name)
                .version(version)
                .status("INACTIVE")
                .permissions(permissions)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return pluginRepository.save(plugin);
    }

    @Transactional
    public ActivePlugin activatePlugin(String id) {
        log.info("Activating plugin: {}", id);
        ActivePlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin not found: " + id));

        plugin.setStatus("ACTIVE");
        plugin.setUpdatedAt(Instant.now());
        return pluginRepository.save(plugin);
    }

    @Transactional
    public ActivePlugin deactivatePlugin(String id) {
        log.info("Deactivating plugin: {}", id);
        ActivePlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plugin not found: " + id));

        plugin.setStatus("INACTIVE");
        plugin.setUpdatedAt(Instant.now());
        return pluginRepository.save(plugin);
    }

    public List<ActivePlugin> getInstalledPlugins() {
        return pluginRepository.findAll();
    }

    public boolean isPluginActive(String id) {
        return pluginRepository.findById(id)
                .map(p -> "ACTIVE".equals(p.getStatus()))
                .orElse(false);
    }
}
