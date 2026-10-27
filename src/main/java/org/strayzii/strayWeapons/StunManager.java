package org.strayzii.strayWeapons;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StunManager implements Listener {
    private static final Color GRAY = Color.fromRGB(0x92, 0x88, 0x7D);
    private static final Color LIGHT = Color.fromRGB(0xC4, 0xBB, 0xAF);

    private final StrayWeapons plugin;
    private final Map<UUID, Long> stunnedUntil = new HashMap<>();

    public StunManager(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    public boolean isStunned(Player player) {
        Long end = stunnedUntil.get(player.getUniqueId());
        if (end == null) return false;

        if (System.currentTimeMillis() >= end) {
            stunnedUntil.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    /** @return true, если оглушение применено; false, если игрок уже оглушён. */
    public boolean stunPlayerFor(Player player, double seconds) {
        if (isStunned(player)) return false;

        int ticks = Math.max(1, (int) Math.round(seconds * 20));

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 4));
        stunnedUntil.put(player.getUniqueId(), System.currentTimeMillis() + (long) (seconds * 1000));

        return true;
    }

    /** Визуальные и звуковые эффекты оглушения. */
    public void playStunEffects(Player victim, double seconds) {
        World world = victim.getWorld();
        Location center = victim.getLocation().add(0, 1.0, 0);

        // --- удар ---
        world.playSound(center, Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 0.9f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
        world.spawnParticle(Particle.DUST, center, 40, 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(GRAY, 1.6f));
        world.spawnParticle(Particle.CRIT, center, 25, 0.4, 0.5, 0.4, 0.4);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center, 15, 0.4, 0.5, 0.4, 0.3);

        // --- ударная волна по земле ---
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!victim.isOnline() || tick > 6) {
                    cancel();
                    return;
                }

                double radius = 0.6 + tick * 0.35;
                int points = 20 + tick * 4;
                Location base = victim.getLocation().add(0, 0.1, 0);

                for (int i = 0; i < points; i++) {
                    double a = 2 * Math.PI * i / points;
                    Location p = base.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                    victim.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(GRAY, 1.3f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // --- «звёздочки» над головой на всё время оглушения ---
        int totalTicks = (int) Math.round(seconds * 20);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!victim.isOnline() || victim.isDead() || t >= totalTicks || !isStunned(victim)) {
                    cancel();
                    return;
                }

                Location head = victim.getLocation().add(0, victim.getHeight() + 0.25, 0);
                double spin = t * 0.45;

                for (int i = 0; i < 3; i++) {
                    double a = spin + i * (2 * Math.PI / 3);
                    Location p = head.clone().add(Math.cos(a) * 0.5, Math.sin(t * 0.3 + i) * 0.05, Math.sin(a) * 0.5);
                    victim.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(i == 0 ? LIGHT : GRAY, 1.2f));
                }

                if (t % 10 == 0) {
                    victim.getWorld().spawnParticle(Particle.CRIT, head, 3, 0.3, 0.1, 0.3, 0.05);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stunnedUntil.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        stunnedUntil.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!e.hasChangedPosition()) return; // вертеть головой можно
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        if (!isStunned(player)) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!isStunned(player)) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!isStunned(player)) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerShear(PlayerShearEntityEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    // PlayerBucketEvent абстрактный, на него нельзя подписаться - только на конкретные
    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!isStunned(e.getPlayer())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player player)) return;
        if (!isStunned(player)) return;

        e.setCancelled(true);
    }
}
