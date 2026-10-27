package org.strayzii.strayWeapons;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.strayzii.strayWeapons.Commands.StrayWeaponsCommand;
import org.strayzii.strayWeapons.Listeners.InteractListener;
import org.strayzii.strayWeapons.Listeners.PlayerAttackListener;
import org.strayzii.strayWeapons.Listeners.ThorHammerListener;

public final class StrayWeapons extends JavaPlugin {
    private WeaponManager weaponManager;
    private CooldownManager cooldownManager;
    private StunManager stunManager;
    private PassiveManager passiveManager;
    private RecipeManager recipeManager;
    private CustomItems customItems;
    private Weapons weapons;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.weaponManager = new WeaponManager(this);
        this.customItems = new CustomItems(this);
        this.cooldownManager = new CooldownManager(this);
        this.stunManager = new StunManager(this);
        this.weapons = new Weapons(this);

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerAttackListener(this), this);
        getServer().getPluginManager().registerEvents(stunManager, this);
        getServer().getPluginManager().registerEvents(new ThorHammerListener(this), this);

        this.passiveManager = new PassiveManager(this);
        this.passiveManager.start();

        this.recipeManager = new RecipeManager(this);
        getServer().getPluginManager().registerEvents(recipeManager, this);
        this.recipeManager.registerAll();

        PluginCommand command = getCommand("strayweapons");
        if (command != null) {
            StrayWeaponsCommand executor = new StrayWeaponsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    /**
     * Отправляет сообщение из messages.<path>.
     * replacements - пары "плейсхолдер", "значение", например: "$weapon", "Меч".
     */
    public void sendMessage(CommandSender target, String path, String... replacements) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null || msg.isEmpty()) return;

        msg = msg.replace("$prefix", getConfig().getString("messages.prefix", ""));

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }

        target.sendMessage(MiniMessage.miniMessage().deserialize(msg));
    }

    /** То же, что sendMessage, но в экшн-бар (не засоряет чат). */
    public void sendActionBar(Player target, String path, String... replacements) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null || msg.isEmpty()) return;

        msg = msg.replace("$prefix", getConfig().getString("messages.prefix", ""));

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }

        target.sendActionBar(MiniMessage.miniMessage().deserialize(msg));
    }

    public CustomItems getCustomItems() {
        return this.customItems;
    }

    public WeaponManager getWeaponManager() {
        return this.weaponManager;
    }

    public StunManager getStunManager() {
        return this.stunManager;
    }

    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public Weapons getWeapons() {
        return this.weapons;
    }

    @Override
    public void onDisable() {
        if (passiveManager != null) passiveManager.stop();
        if (recipeManager != null) recipeManager.unregisterAll();
    }
}
