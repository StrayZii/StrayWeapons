package org.strayzii.strayWeapons.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.strayzii.strayWeapons.StrayWeapons;

public class InteractListener implements Listener {
    private final StrayWeapons plugin;

    public InteractListener(StrayWeapons plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();

        String weaponName = plugin.getWeaponManager().isWeapon(item);
        if (weaponName == null) return;

        // Основа косы - мотыга: запрещаем ей вспахивать землю, способность срабатывает и так
        if (weaponName.equals("void_scythe")) {
            e.setUseItemInHand(Event.Result.DENY);
        }

        plugin.getWeapons().process(e.getPlayer(), weaponName);
    }
}
