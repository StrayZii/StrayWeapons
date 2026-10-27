package org.strayzii.strayWeapons.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.strayzii.strayWeapons.StrayWeapons;

public class PlayerAttackListener implements Listener {
    private final StrayWeapons plugin;

    public PlayerAttackListener(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    // MONITOR + ignoreCancelled: реагируем только на удары, которые реально прошли
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        String weaponName = plugin.getWeaponManager().isWeapon(attacker.getInventory().getItemInMainHand());
        if (weaponName == null) return;

        plugin.getWeapons().processHit(attacker, victim, weaponName, e.isCritical());
    }

    // Убийство игрока: даёт оружию шанс сработать (например, лечение косы)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        String weaponName = plugin.getWeaponManager().isWeapon(killer.getInventory().getItemInMainHand());
        if (weaponName == null) return;

        plugin.getWeapons().processKill(killer, victim, weaponName);
    }
}
