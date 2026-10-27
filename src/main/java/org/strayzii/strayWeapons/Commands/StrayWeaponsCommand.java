package org.strayzii.strayWeapons.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.strayzii.strayWeapons.StrayWeapons;
import org.strayzii.strayWeapons.Weapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StrayWeaponsCommand implements CommandExecutor, TabCompleter {
    private final StrayWeapons plugin;

    public StrayWeaponsCommand(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.sendMessage(sender, "usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "get" -> handleGet(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            default -> plugin.sendMessage(sender, "usage");
        }

        return true;
    }

    private void handleGet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("strayweapons.get")) {
            plugin.sendMessage(sender, "no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "player-only");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "usage");
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        Weapon weapon = plugin.getWeapons().getWeapon(name);

        if (weapon == null) {
            plugin.sendMessage(sender, "unknown-weapon", "$weapon", args[1]);
            return;
        }

        ItemStack item = weapon.getInstance();
        if (item == null) {
            plugin.sendMessage(sender, "item-error");
            return;
        }

        // если инвентарь полон - предмет падает под ноги
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));

        plugin.sendMessage(sender, "weapon-given",
                "$weapon", plugin.getConfig().getString("weapons." + name + ".display-name", name));
    }

    // /sw reset <оружие> - снова разрешить крафт легендарки
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("strayweapons.reset")) {
            plugin.sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "usage");
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);

        if (plugin.getWeapons().getWeapon(name) == null) {
            plugin.sendMessage(sender, "unknown-weapon", "$weapon", args[1]);
            return;
        }

        String display = plugin.getConfig().getString("weapons." + name + ".display-name", name);

        if (!plugin.getRecipeManager().isCrafted(name)) {
            plugin.sendMessage(sender, "reset-not-needed", "$weapon", display);
            return;
        }

        plugin.getRecipeManager().setCrafted(name, false);
        plugin.sendMessage(sender, "reset-done", "$weapon", display);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("strayweapons.reload")) {
            plugin.sendMessage(sender, "no-permission");
            return;
        }

        plugin.reloadConfig();
        plugin.getRecipeManager().registerAll();
        plugin.sendMessage(sender, "reloaded");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("strayweapons.get")) result.add("get");
            if (sender.hasPermission("strayweapons.reset")) result.add("reset");
            if (sender.hasPermission("strayweapons.reload")) result.add("reload");
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            if ((sub.equals("get") && sender.hasPermission("strayweapons.get"))
                    || (sub.equals("reset") && sender.hasPermission("strayweapons.reset"))) {
                result.addAll(plugin.getWeapons().getWeaponNames());
            }
        }

        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        result.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(current));
        return result;
    }
}
