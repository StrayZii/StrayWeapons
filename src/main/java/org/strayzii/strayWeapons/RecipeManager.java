package org.strayzii.strayWeapons;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Рецепты легендарок. Каждое оружие можно скрафтить только один раз на весь сервер,
 * после этого рецепт не работает, пока не выполнена команда /sw reset <оружие>.
 * Состояние хранится в plugins/StrayWeapons/data.yml.
 *
 * Ингредиентом может быть кастомный предмет (CustomItems): в конфиге вместо материала
 * пишется его id (например thor_head). Рецепт регистрируется по базовому материалу,
 * а в PrepareItemCraftEvent проверяется, что в слотах именно наш предмет.
 * Кастомные предметы нельзя использовать в чужих (ванильных) рецептах.
 */
public class RecipeManager implements Listener {
    private final StrayWeapons plugin;
    private final File file;
    private final YamlConfiguration data;
    /** рецепты легендарок: ключ -> имя оружия */
    private final Map<NamespacedKey, String> recipeKeys = new HashMap<>();
    /** рецепты кастомных предметов (навершие): без лимита "один раз" */
    private final Set<NamespacedKey> itemRecipeKeys = new HashSet<>();
    /** ключ рецепта -> (материал -> id кастомного предмета, который тут обязан лежать) */
    private final Map<NamespacedKey, Map<Material, String>> customIngredients = new HashMap<>();

    public RecipeManager(StrayWeapons plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    // ---------------- состояние "уже скрафчено" ----------------

    public boolean isCrafted(String weapon) {
        return data.getBoolean("crafted." + weapon, false);
    }

    public void setCrafted(String weapon, boolean crafted) {
        data.set("crafted." + weapon, crafted ? Boolean.TRUE : null);
        save();
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + ex.getMessage());
        }
    }

    // ---------------- регистрация рецептов ----------------

    public void registerAll() {
        unregisterAll();

        registerItemRecipes();
        for (String name : plugin.getWeapons().getWeaponNames()) {
            register(name);
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : recipeKeys.keySet()) Bukkit.removeRecipe(key);
        for (NamespacedKey key : itemRecipeKeys) Bukkit.removeRecipe(key);
        recipeKeys.clear();
        itemRecipeKeys.clear();
        customIngredients.clear();
    }

    /** 9 осколков молнии (без формы) -> навершие Тора */
    private void registerItemRecipes() {
        CustomItems items = plugin.getCustomItems();
        String path = "items." + CustomItems.THOR_HEAD + ".recipe";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section != null && !section.getBoolean("enabled", true)) return;

        int count = Math.max(1, Math.min(9, plugin.getConfig().getInt(path + ".shard-count", 9)));

        ItemStack result = items.create(CustomItems.THOR_HEAD);
        if (result == null) return;

        NamespacedKey key = new NamespacedKey(plugin, CustomItems.THOR_HEAD + "_recipe");
        Material shardMaterial = items.materialOf(CustomItems.LIGHTNING_SHARD);

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (int i = 0; i < count; i++) {
            recipe.addIngredient(new RecipeChoice.MaterialChoice(shardMaterial));
        }

        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
        itemRecipeKeys.add(key);
        customIngredients.put(key, Map.of(shardMaterial, CustomItems.LIGHTNING_SHARD));
    }

    private void register(String name) {
        String path = "weapons." + name + ".recipe";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);

        if (section == null || !section.getBoolean("enabled", true)) return;

        List<String> shape = section.getStringList("shape");
        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");

        if (shape.isEmpty() || ingredients == null) {
            plugin.getLogger().warning("Рецепт " + name + ": не заданы shape или ingredients, пропускаю.");
            return;
        }

        Weapon weapon = plugin.getWeapons().getWeapon(name);
        ItemStack result = weapon == null ? null : weapon.getInstance();
        if (result == null) {
            plugin.getLogger().warning("Рецепт " + name + ": не удалось создать предмет, пропускаю.");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, name + "_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        try {
            recipe.shape(shape.toArray(new String[0]));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Рецепт " + name + ": неверная форма (нужно 1-3 строки по 1-3 символа одной длины).");
            return;
        }

        Set<Character> used = new HashSet<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ') used.add(c);
            }
        }

        Map<Material, String> custom = new HashMap<>();

        for (char c : used) {
            String ingredientName = ingredients.getString(String.valueOf(c));

            // кастомный предмет (например thor_head)
            if (ingredientName != null && plugin.getCustomItems().has(ingredientName)) {
                String id = ingredientName.toLowerCase(Locale.ROOT);
                Material base = plugin.getCustomItems().materialOf(id);
                recipe.setIngredient(c, new RecipeChoice.MaterialChoice(base));
                custom.put(base, id);
                continue;
            }

            Material material = ingredientName == null ? null : Material.matchMaterial(ingredientName);

            if (material == null || !material.isItem()) {
                plugin.getLogger().warning("Рецепт " + name + ": неизвестный ингредиент для символа '" + c + "': " + ingredientName);
                return;
            }

            recipe.setIngredient(c, new RecipeChoice.MaterialChoice(material));
        }

        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
        recipeKeys.put(key, name);
        if (!custom.isEmpty()) customIngredients.put(key, custom);
    }

    private String weaponOf(Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            return recipeKeys.get(keyed.getKey());
        }
        return null;
    }

    // ---------------- события крафта ----------------

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        Recipe recipe = e.getRecipe();
        if (recipe == null) return;

        // 1) кастомные предметы: только в "своих" рецептах и только на своих местах
        Map<Material, String> allowed = recipe instanceof Keyed k ? customIngredients.get(k.getKey()) : null;

        for (ItemStack it : e.getInventory().getMatrix()) {
            if (it == null || it.getType().isAir()) continue;

            String id = plugin.getCustomItems().idOf(it);
            String need = allowed == null ? null : allowed.get(it.getType());

            // кастомный предмет в чужом рецепте, или не тот предмет там, где нужен кастомный
            if ((need == null && id != null) || (need != null && !need.equals(id))) {
                e.getInventory().setResult(null);
                return;
            }
        }

        // 2) легендарка уже создана - результата нет
        String weapon = weaponOf(recipe);
        if (weapon != null && isCrafted(weapon)) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        String weapon = weaponOf(e.getRecipe());
        if (weapon == null) return;

        // кто-то успел раньше (например, второй игрок с уже собранным результатом)
        if (isCrafted(weapon)) {
            e.setCancelled(true);
            return;
        }

        // Shift-клик мог бы скрафтить несколько штук за раз
        if (e.isShiftClick()) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player player) {
                plugin.sendMessage(player, "craft-shift");
            }
        }
    }

    // Крафт реально состоялся - запоминаем и объявляем
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftDone(CraftItemEvent e) {
        String weapon = weaponOf(e.getRecipe());
        if (weapon == null) return;

        setCrafted(weapon, true);

        String weaponName = plugin.getConfig().getString("weapons." + weapon + ".display-name", weapon);

        if (e.getWhoClicked() instanceof Player player) {
            plugin.sendMessage(player, "on-craft", "$weapon", weaponName);

            String msg = plugin.getConfig().getString("messages.craft-broadcast");
            if (msg != null && !msg.isEmpty()) {
                msg = msg.replace("$prefix", plugin.getConfig().getString("messages.prefix", ""))
                        .replace("$player", player.getName())
                        .replace("$weapon", weaponName);
                plugin.getServer().broadcast(MiniMessage.miniMessage().deserialize(msg));
            }
        }
    }
}