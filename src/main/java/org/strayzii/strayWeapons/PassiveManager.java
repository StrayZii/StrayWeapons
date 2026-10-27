package org.strayzii.strayWeapons;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PassiveManager {
    private static final String SPEAR = "jungle_spear";
    private static final int REFRESH_DURATION = 30; // тиков

    private final StrayWeapons plugin;
    private final Set<UUID> active = new HashSet<>();
    private BukkitTask task;

    public PassiveManager(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    public void stop() {
        if (task != null) task.cancel();

        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) clear(p);
        }
        active.clear();
    }

    private int amplifier() {
        return Math.max(0, plugin.getConfig().getInt("weapons." + SPEAR + ".speed-level", 2) - 1);
    }

    private void tick() {
        int amp = amplifier();

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean holding = SPEAR.equals(plugin.getWeaponManager().isWeapon(p.getInventory().getItemInMainHand()));

            if (holding) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, REFRESH_DURATION, amp, true, false, false));
                active.add(p.getUniqueId());
            } else if (active.remove(p.getUniqueId())) {
                clear(p);
            }
        }

        active.removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    // убираем только наш эффект, чужой (зелье и т.п.) не трогаем
    private void clear(Player p) {
        PotionEffect e = p.getPotionEffect(PotionEffectType.SPEED);
        if (e != null && e.getAmplifier() == amplifier() && e.getDuration() <= REFRESH_DURATION) {
            p.removePotionEffect(PotionEffectType.SPEED);
        }
    }
}
