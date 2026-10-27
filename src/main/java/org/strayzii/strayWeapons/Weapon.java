package org.strayzii.strayWeapons;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface Weapon {
    String getName();

    @Nullable ItemStack getInstance();

    void useAbility(Player player);

    /** Вызывается после удара игрока по игроку этим оружием. */
    default void onHit(Player attacker, Player victim, boolean critical) {
    }

    /** Вызывается, когда игрок убил игрока этим оружием в руке. */
    default void onKill(Player killer, Player victim) {
    }
}
