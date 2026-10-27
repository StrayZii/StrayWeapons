package org.strayzii.strayWeapons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Кастомные предметы-ингредиенты (не оружие): осколок молнии и навершие Тора.
 * Настройки - в config.yml, секция items.<id>.
 * Модель предмета: strayweapons:<id> (см. resourcepack).
 */
public class CustomItems {
    public static final String LIGHTNING_SHARD = "lightning_shard";
    public static final String THOR_HEAD = "thor_head";

    private static final Map<String, Material> DEFAULT_MATERIALS = new LinkedHashMap<>();

    static {
        DEFAULT_MATERIALS.put(LIGHTNING_SHARD, Material.AMETHYST_SHARD);
        DEFAULT_MATERIALS.put(THOR_HEAD, Material.NETHERITE_SCRAP);
    }

    private final StrayWeapons plugin;
    private final NamespacedKey idKey;

    public CustomItems(StrayWeapons plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "custom_item");
    }

    public boolean has(String id) {
        return id != null && DEFAULT_MATERIALS.containsKey(id.toLowerCase());
    }

    public Set<String> getIds() {
        return DEFAULT_MATERIALS.keySet();
    }

    public Material materialOf(String id) {
        String path = "items." + id;
        Material def = DEFAULT_MATERIALS.getOrDefault(id, Material.AMETHYST_SHARD);
        String name = plugin.getConfig().getString(path + ".material", def.name());
        Material m = Material.matchMaterial(name);
        if (m == null || !m.isItem()) {
            plugin.getLogger().warning("Неизвестный материал '" + name + "' для items." + id + ", использую " + def);
            return def;
        }
        return m;
    }

    public @Nullable ItemStack create(String id) {
        if (!has(id)) return null;
        String path = "items." + id;

        ItemStack item = new ItemStack(materialOf(id));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        MiniMessage mm = MiniMessage.miniMessage();

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        meta.displayName(mm.deserialize(plugin.getConfig().getString(path + ".display-name", id))
                .decoration(TextDecoration.ITALIC, false));

        String count = String.valueOf(plugin.getConfig().getInt(path + ".recipe.shard-count", 9));
        List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                .map(s -> s.replace("$count", count))
                .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(lore);

        meta.setItemModel(new NamespacedKey("strayweapons", id));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    /** id кастомного предмета или null, если это обычный предмет. */
    public @Nullable String idOf(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }
}
