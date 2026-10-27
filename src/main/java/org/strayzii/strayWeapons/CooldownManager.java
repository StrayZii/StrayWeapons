package org.strayzii.strayWeapons;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final StrayWeapons plugin;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    public void setCooldown(Player player, String weaponName, int time) {
        cooldowns
                .computeIfAbsent(player.getUniqueId(), p -> new HashMap<>())
                .put(weaponName, System.currentTimeMillis() + time * 1000L);
    }

    public boolean hasCooldown(Player player, String weaponName) {
        UUID id = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(id);

        if (playerCooldowns == null) return false;

        Long endTime = playerCooldowns.get(weaponName);

        if (endTime == null) return false;

        if (System.currentTimeMillis() >= endTime) {
            playerCooldowns.remove(weaponName);

            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(id);
            }

            return false;
        }

        return true;
    }

    public long getRemainingCooldown(Player player, String weaponName) {
        if (!hasCooldown(player, weaponName)) return 0;

        long ms = cooldowns.get(player.getUniqueId()).get(weaponName) - System.currentTimeMillis();
        return (long) Math.ceil(ms / 1000.0);
    }
}
