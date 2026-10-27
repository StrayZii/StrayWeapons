package org.strayzii.strayWeapons.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.strayzii.strayWeapons.StrayWeapons;

/**
 * Молот Тора: +N% урона от smash-атаки булавой (по умолчанию +20%).
 * Smash-атака в ваниле - удар булавой с падением: fallDistance > 1.5 и игрок не летит на элитрах.
 * Множитель: weapons.thor_hammer.smash-damage-multiplier (1.2 = +20%).
 */
public class ThorHammerListener implements Listener {
    private static final String NAME = "thor_hammer";
    private static final float SMASH_MIN_FALL = 1.5f;

    private final StrayWeapons plugin;

    public ThorHammerListener(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onThorExplosion(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (e.getEntity() instanceof Player victim
                && victim.hasMetadata("thor_immune")
                && e.getDamager().equals(victim)) {
            e.setCancelled(true);
        }
    }

    // HIGH: после ванильного расчёта урона булавы, но до MONITOR-слушателей
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmash(EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player player)) return;

        if (!NAME.equals(plugin.getWeaponManager().isWeapon(player.getInventory().getItemInMainHand()))) return;

        if (player.isGliding() || player.getFallDistance() <= SMASH_MIN_FALL) return;

        double multiplier = plugin.getConfig().getDouble("weapons." + NAME + ".smash-damage-multiplier", 1.2);
        if (multiplier <= 0 || multiplier == 1.0) return;

        e.setDamage(e.getDamage() * multiplier);
    }
}
