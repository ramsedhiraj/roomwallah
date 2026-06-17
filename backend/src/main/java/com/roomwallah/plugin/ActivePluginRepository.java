package com.roomwallah.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivePluginRepository extends JpaRepository<ActivePlugin, String> {
}
