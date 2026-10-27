package org.strayzii.strayWeapons;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class WeaponManager {
    private final StrayWeapons plugin;
    private final Map<String, NamespacedKey> keys;

    public WeaponManager(StrayWeapons plugin) {
        this.plugin = plugin;

        this.keys = new HashMap<>();
        keys.put("trial_sword", new NamespacedKey(plugin, "trial_sword"));
        keys.put("stun_axe", new NamespacedKey(plugin, "stun_axe"));
        keys.put("jungle_spear", new NamespacedKey(plugin, "jungle_spear"));
        keys.put("poseidon_trident", new NamespacedKey(plugin, "poseidon_trident"));
        keys.put("thor_hammer", new NamespacedKey(plugin, "thor_hammer"));
        keys.put("void_scythe", new NamespacedKey(plugin, "void_scythe"));
    }

    public @Nullable String isWeapon(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (String key : keys.keySet()) {
            if (container.has(keys.get(key), PersistentDataType.BOOLEAN)) {
                return key;
            }
        }

        return null;
    }
}
