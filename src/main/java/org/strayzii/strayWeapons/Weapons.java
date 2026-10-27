package org.strayzii.strayWeapons;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Weapons {
    private final Map<String, Weapon> weapons = new LinkedHashMap<>();

    public Weapons(StrayWeapons plugin) {
        // ======================= Меч Испытаний =======================
        register(new Weapon() {
            private static final String NAME = "trial_sword";

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "NETHERITE_SWORD");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую NETHERITE_SWORD");
                    material = Material.NETHERITE_SWORD;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Netherite sword: 8 урона всего = 1 (база игрока) + 7
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_damage"),
                        7 + plugin.getConfig().getDouble(path + ".attack-damage-buff", 1),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                // Скорость атаки как у обычного меча: 4.0 - 2.4 = 1.6
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_speed"),
                        -2.4,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#c16746>Меч Испытаний</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String cooldown = String.valueOf(plugin.getConfig().getInt(path + ".ability-cooldown", 30));
                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$ability", cooldown))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);
                meta.setItemModel(new NamespacedKey("strayweapons", NAME));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            @Override
            public void useAbility(Player player) {
                CooldownManager cooldowns = plugin.getCooldownManager();

                if (cooldowns.hasCooldown(player, NAME)) {
                    plugin.sendActionBar(player, "on-cooldown",
                            "$remaining", String.valueOf(cooldowns.getRemainingCooldown(player, NAME)),
                            "$weapon", plugin.getConfig().getString("weapons." + NAME + ".display-name", NAME));
                    return;
                }

                Location start = player.getLocation().clone();

                Vector direction = player.getLocation().getDirection();
                if (direction.lengthSquared() < 1.0E-4) return;
                direction.normalize();

                player.setVelocity(direction.multiply(2.2));

                World world = player.getWorld();

                world.playSound(start, Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.0f, 1.25f);

                try {
                    // новые версии Paper требуют Color для FLASH
                    world.spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0, Color.WHITE);
                } catch (IllegalArgumentException ex) {
                    world.spawnParticle(Particle.FLASH, start, 1, 0, 0, 0, 0);
                }

                world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, start, 30, 0.35, 0.25, 0.35, 0.12);

                cooldowns.setCooldown(player, NAME,
                        plugin.getConfig().getInt("weapons." + NAME + ".ability-cooldown", 30));
            }
        });

        // ======================= Топор Оглушения =======================
        register(new Weapon() {
            private static final String NAME = "stun_axe";

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "NETHERITE_AXE");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую NETHERITE_AXE");
                    material = Material.NETHERITE_AXE;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Netherite axe: 10 урона всего = 1 (база игрока) + 9
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_damage"),
                        9 + plugin.getConfig().getDouble(path + ".attack-damage-buff", 1),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                // Скорость атаки как у топора: 4.0 - 3.0 = 1.0
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_speed"),
                        -3.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#92887D>Топор Оглушения</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String cooldown = fmt(plugin.getConfig().getDouble(path + ".ability-cooldown", 30));
                String chance = fmt(plugin.getConfig().getDouble(path + ".stun-chance", 5));
                String duration = fmt(plugin.getConfig().getDouble(path + ".stun-duration", 1.5));

                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$ability", cooldown)
                                .replace("$chance", chance)
                                .replace("$duration", duration))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);
                meta.setItemModel(new NamespacedKey("strayweapons", NAME));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            // ПКМ: пассивное оружие, просто показываем оставшуюся перезарядку
            @Override
            public void useAbility(Player player) {
                CooldownManager cooldowns = plugin.getCooldownManager();

                if (cooldowns.hasCooldown(player, NAME)) {
                    plugin.sendActionBar(player, "on-cooldown",
                            "$remaining", String.valueOf(cooldowns.getRemainingCooldown(player, NAME)),
                            "$weapon", plugin.getConfig().getString("weapons." + NAME + ".display-name", NAME));
                }
            }

            @Override
            public void onHit(Player attacker, Player victim, boolean critical) {
                if (!critical) return;

                CooldownManager cooldowns = plugin.getCooldownManager();
                if (cooldowns.hasCooldown(attacker, NAME)) return;

                String path = "weapons." + NAME;

                double chance = plugin.getConfig().getDouble(path + ".stun-chance", 5.0);
                if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

                double duration = plugin.getConfig().getDouble(path + ".stun-duration", 1.5);
                if (!plugin.getStunManager().stunPlayerFor(victim, duration)) return;

                plugin.getStunManager().playStunEffects(victim, duration);

                cooldowns.setCooldown(attacker, NAME, plugin.getConfig().getInt(path + ".ability-cooldown", 30));

                plugin.sendMessage(attacker, "on-stun",
                        "$target", victim.getName(),
                        "$duration", fmt(duration));
            }
        });

        // ======================= Жало Джунглей =======================
        register(new Weapon() {
            private static final String NAME = "jungle_spear";
            private static final Color GREEN = Color.fromRGB(0x5F, 0xAE, 0x46);
            private static final Color LIME = Color.fromRGB(0xB6, 0xE3, 0x5A);

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "NETHERITE_SPEAR");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую NETHERITE_SWORD");
                    material = Material.NETHERITE_SWORD;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Бафф 0 = ванильные характеристики копья (атрибуты не трогаем).
                // Если бафф задан - переносим ванильные атрибуты и добавляем бафф сверху.
                double buff = plugin.getConfig().getDouble(path + ".attack-damage-buff", 0);
                if (buff != 0) {
                    for (Map.Entry<Attribute, AttributeModifier> en : material.getDefaultAttributeModifiers(EquipmentSlot.HAND).entries()) {
                        meta.addAttributeModifier(en.getKey(), en.getValue());
                    }

                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                            new NamespacedKey(plugin, NAME + "_damage"),
                            buff,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND));
                }

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#5FAE46>Жало Джунглей</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String cooldown = fmt(plugin.getConfig().getDouble(path + ".ability-cooldown", 25));
                String duration = fmt(plugin.getConfig().getDouble(path + ".poison-duration", 5));
                String level = fmt(plugin.getConfig().getInt(path + ".poison-level", 2));
                String speed = fmt(plugin.getConfig().getInt(path + ".speed-level", 2));

                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$ability", cooldown)
                                .replace("$duration", duration)
                                .replace("$level", level)
                                .replace("$speed", speed))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);
                meta.setItemModel(new NamespacedKey("strayweapons", NAME));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            @Override
            public void useAbility(Player player) {
                CooldownManager cooldowns = plugin.getCooldownManager();

                // ПКМ - ванильная механика копья (его можно зажимать), поэтому перезарядку
                // показываем в экшн-баре (как и у остального оружия): он не спамит чат
                if (cooldowns.hasCooldown(player, NAME)) {
                    plugin.sendActionBar(player, "on-cooldown",
                            "$remaining", String.valueOf(cooldowns.getRemainingCooldown(player, NAME)),
                            "$weapon", plugin.getConfig().getString("weapons." + NAME + ".display-name", NAME));
                    return;
                }

                cooldowns.setCooldown(player, NAME,
                        plugin.getConfig().getInt("weapons." + NAME + ".ability-cooldown", 25));

                shoot(player);
            }

            private void shoot(Player shooter) {
                String path = "weapons." + NAME;
                double speed = plugin.getConfig().getDouble(path + ".projectile-speed", 1.4);
                double range = plugin.getConfig().getDouble(path + ".projectile-range", 40);
                int poisonTicks = (int) Math.round(plugin.getConfig().getDouble(path + ".poison-duration", 5) * 20);
                int poisonAmp = Math.max(0, plugin.getConfig().getInt(path + ".poison-level", 2) - 1);

                Location eye = shooter.getEyeLocation();
                World world = eye.getWorld();
                Vector dir = eye.getDirection().normalize();
                Location pos = eye.clone().add(dir.clone().multiply(0.6)).subtract(0, 0.15, 0);

                world.playSound(eye, Sound.ENTITY_LLAMA_SPIT, 1.0f, 0.7f);
                world.playSound(eye, Sound.ENTITY_WITCH_THROW, 0.7f, 1.3f);

                new BukkitRunnable() {
                    double travelled = 0;

                    @Override
                    public void run() {
                        if (!shooter.isOnline() || travelled >= range) {
                            cancel();
                            return;
                        }

                        RayTraceResult hit = world.rayTrace(pos, dir, speed, FluidCollisionMode.NEVER, true, 0.25,
                                en -> en instanceof Player target
                                        && !target.equals(shooter)
                                        && target.getGameMode() != GameMode.SPECTATOR
                                        && !target.isDead());

                        double step = speed;
                        if (hit != null) {
                            step = pos.distance(hit.getHitPosition().toLocation(world));
                        }

                        // след выстрела
                        for (double d = 0; d < step; d += 0.35) {
                            Location point = pos.clone().add(dir.clone().multiply(d));
                            world.spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0,
                                    new Particle.DustOptions(GREEN, 1.5f));
                            world.spawnParticle(Particle.DUST, point, 1, 0.08, 0.08, 0.08, 0,
                                    new Particle.DustOptions(LIME, 1.0f));
                        }
                        world.spawnParticle(Particle.HAPPY_VILLAGER, pos, 1, 0.1, 0.1, 0.1, 0);

                        if (hit != null) {
                            Location impact = hit.getHitPosition().toLocation(world);

                            world.spawnParticle(Particle.DUST, impact, 30, 0.3, 0.3, 0.3, 0,
                                    new Particle.DustOptions(GREEN, 1.8f));
                            world.spawnParticle(Particle.HAPPY_VILLAGER, impact, 12, 0.3, 0.3, 0.3, 0);
                            world.playSound(impact, Sound.ENTITY_SPLASH_POTION_BREAK, 0.8f, 1.2f);

                            if (hit.getHitEntity() instanceof Player victim) {
                                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonTicks, poisonAmp));
                                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0),
                                        25, 0.4, 0.6, 0.4, 0, new Particle.DustOptions(LIME, 1.4f));

                                plugin.sendMessage(shooter, "on-poison", "$target", victim.getName());
                            }

                            cancel();
                            return;
                        }

                        pos.add(dir.clone().multiply(speed));
                        travelled += speed;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        });

        // ======================= Трезубец Посейдона =======================
        register(new Weapon() {
            private static final String NAME = "poseidon_trident";

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "TRIDENT");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую TRIDENT");
                    material = Material.TRIDENT;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Трезубец: 9 урона всего = 1 (база игрока) + 8
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_damage"),
                        8 + plugin.getConfig().getDouble(path + ".attack-damage-buff", 1),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                // Скорость атаки как у меча: 4.0 - 2.4 = 1.6
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_speed"),
                        -2.4,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#35B6D8>Трезубец Посейдона</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String shield = fmt(plugin.getConfig().getDouble(path + ".shield-disable-duration", 5));

                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$shield", shield))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);
                meta.setItemModel(new NamespacedKey("strayweapons", NAME));
                meta.setUnbreakable(true);

                // Тягун (Riptide): с ним трезубец невозможно бросить, только рвануть вперёд в воде/под дождём.
                // Тягун виден в описании предмета вместе с остальными зачарованиями.
                int riptide = plugin.getConfig().getInt(path + ".riptide-level", 3);
                if (riptide > 0) {
                    meta.addEnchant(Enchantment.RIPTIDE, riptide, true);
                }

                // Без фиолетового свечения зачарования (оно красило нашу модель)
                meta.setEnchantmentGlintOverride(false);

                // HIDE_ENCHANTS не ставим: иначе спрячется и Острота, которую игрок наложит в наковальне
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            // Активной способности нет (ПКМ - ванильный бросок трезубца)
            @Override
            public void useAbility(Player player) {
            }

            // Удар по игроку с поднятым щитом отключает щит (как топор)
            @Override
            public void onHit(Player attacker, Player victim, boolean critical) {
                if (!victim.isBlocking()) return;

                double seconds = plugin.getConfig().getDouble("weapons." + NAME + ".shield-disable-duration", 5);
                victim.setCooldown(Material.SHIELD, (int) Math.round(seconds * 20));
                victim.clearActiveItem();

                World world = victim.getWorld();
                Location loc = victim.getLocation().add(0, 1.0, 0);
                world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                world.spawnParticle(Particle.CRIT, loc, 15, 0.4, 0.5, 0.4, 0.3);
                world.spawnParticle(Particle.BUBBLE_POP, loc, 15, 0.4, 0.5, 0.4, 0.05);
            }
        });

        register(new Weapon() {
            private static final String NAME = "thor_hammer";
            private static final Color YELLOW = Color.fromRGB(0xFF, 0xE1, 0x4D);
            private static final Color SKY = Color.fromRGB(0x7E, 0xC8, 0xF0);

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "MACE");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую MACE");
                    material = Material.MACE;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Булава: 6 урона всего = 1 (база игрока) + 5
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_damage"),
                        5 + plugin.getConfig().getDouble(path + ".attack-damage-buff", 3),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                // Скорость атаки как у булавы: 4.0 - 3.4 = 0.6
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_speed"),
                        -3.4,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#7EC8F0>Молот Тора</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String cooldown = fmt(plugin.getConfig().getDouble(path + ".ability-cooldown", 45));
                String damage = fmt(plugin.getConfig().getDouble(path + ".ability-damage", 8));
                String radius = fmt(plugin.getConfig().getDouble(path + ".ability-radius", 3.5));
                String range = fmt(plugin.getConfig().getDouble(path + ".ability-range", 30));
                String smash = fmt(Math.round((plugin.getConfig().getDouble(path + ".smash-damage-multiplier", 1.2) - 1) * 100));

                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$ability", cooldown)
                                .replace("$damage", damage)
                                .replace("$radius", radius)
                                .replace("$range", range)
                                .replace("$smash", smash))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);
                meta.setItemModel(new NamespacedKey("strayweapons", NAME));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            // ПКМ: «Гнев Небес» - молния в точку, куда смотрит игрок
            @Override
            public void useAbility(Player player) {
                if (plugin.getStunManager().isStunned(player)) return;

                CooldownManager cooldowns = plugin.getCooldownManager();
                String path = "weapons." + NAME;

                if (cooldowns.hasCooldown(player, NAME)) {
                    plugin.sendActionBar(player, "on-cooldown",
                            "$remaining", String.valueOf(cooldowns.getRemainingCooldown(player, NAME)),
                            "$weapon", plugin.getConfig().getString(path + ".display-name", NAME));
                    return;
                }

                double range = plugin.getConfig().getDouble(path + ".ability-range", 30);
                double damage = plugin.getConfig().getDouble(path + ".ability-damage", 8);
                double radius = plugin.getConfig().getDouble(path + ".ability-radius", 3.5);

                Location eye = player.getEyeLocation();
                World world = eye.getWorld();

                RayTraceResult hit = world.rayTrace(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true, 0.3,
                        en -> en instanceof LivingEntity
                                && !(en instanceof ArmorStand)
                                && !en.equals(player)
                                && !(en instanceof Player p && p.getGameMode() == GameMode.SPECTATOR));

                if (hit == null) {
                    plugin.sendActionBar(player, "thor-no-target");
                    return;
                }

                Location impact = hit.getHitPosition().toLocation(world);

                // только визуал молнии: без пожара и урона владельцу
                world.strikeLightningEffect(impact);
                world.playSound(impact, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
                world.playSound(impact, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
                world.spawnParticle(Particle.ELECTRIC_SPARK, impact, 60, radius / 2, 0.8, radius / 2, 0.4);
                world.spawnParticle(Particle.DUST, impact, 40, radius / 2, 0.8, radius / 2, 0,
                        new Particle.DustOptions(YELLOW, 1.6f));

                // столб молнии
                for (double y = 0; y < 14; y += 0.5) {
                    world.spawnParticle(Particle.DUST, impact.clone().add(0, y, 0), 2, 0.1, 0, 0.1, 0,
                            new Particle.DustOptions(y % 1 == 0 ? YELLOW : SKY, 1.4f));
                }

                // ударная волна по земле
                for (int i = 0; i < 36; i++) {
                    double a = 2 * Math.PI * i / 36;
                    Location p = impact.clone().add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius);
                    world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, new Particle.DustOptions(SKY, 1.3f));
                }

                for (Entity en : world.getNearbyEntities(impact, radius, radius, radius)) {
                    if (!(en instanceof LivingEntity le) || en instanceof ArmorStand || en.equals(player)) continue;
                    if (le instanceof Player target && target.getGameMode() == GameMode.SPECTATOR) continue;

                    le.damage(damage, player);
                    le.setVelocity(le.getVelocity().add(new Vector(0, 0.5, 0)));
                }

                cooldowns.setCooldown(player, NAME, plugin.getConfig().getInt(path + ".ability-cooldown", 45));
            }
        });

        // ======================= Коса Пустоты =======================
        register(new Weapon() {
            private static final String NAME = "void_scythe";
            private static final Color MAGENTA = Color.fromRGB(0xA1, 0x3E, 0x91);
            private static final Color PINK = Color.fromRGB(0xE0, 0x7B, 0xD0);

            @Override
            public String getName() {
                return NAME;
            }

            @Override
            public @Nullable ItemStack getInstance() {
                String path = "weapons." + NAME;
                NamespacedKey key = new NamespacedKey(plugin, NAME);

                String materialName = plugin.getConfig().getString(path + ".material", "NETHERITE_HOE");
                Material material = Material.matchMaterial(materialName);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Неизвестный материал '" + materialName + "' в конфиге, использую NETHERITE_HOE");
                    material = Material.NETHERITE_HOE;
                }

                ItemStack item = new ItemStack(material, 1);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return null;

                // Урон как у незеритового меча: 1 (база игрока) + 7 + бафф
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_damage"),
                        7 + plugin.getConfig().getDouble(path + ".attack-damage-buff", 1),
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                // Скорость атаки как у меча: 4.0 - 2.4 = 1.6
                meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey(plugin, NAME + "_speed"),
                        -2.4,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));

                meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

                MiniMessage mm = MiniMessage.miniMessage();

                meta.displayName(
                        mm.deserialize(plugin.getConfig().getString(path + ".display-name", "<b><color:#A13E91>Коса Пустоты</color></b>"))
                                .decoration(TextDecoration.ITALIC, false)
                );

                String cooldown = fmt(plugin.getConfig().getDouble(path + ".ability-cooldown", 25));
                String range = fmt(plugin.getConfig().getDouble(path + ".pull-range", 15));
                String hearts = fmt(plugin.getConfig().getDouble(path + ".harvest-heal", 4) / 2.0);

                List<Component> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                        .map(s -> s.replace("$ability", cooldown)
                                .replace("$range", range)
                                .replace("$hearts", hearts))
                        .map(s -> mm.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
                meta.lore(lore);

                // Модель из ресурспака StrayWeapons, ключ меняется в конфиге
                NamespacedKey model = NamespacedKey.fromString(
                        plugin.getConfig().getString(path + ".item-model", "strayweapons:void_scythe"));
                if (model != null) {
                    meta.setItemModel(model);
                }

                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

                item.setItemMeta(meta);
                return item;
            }

            // ПКМ: притягивает игрока в прицеле
            @Override
            public void useAbility(Player player) {
                CooldownManager cooldowns = plugin.getCooldownManager();

                if (cooldowns.hasCooldown(player, NAME)) {
                    plugin.sendActionBar(player, "on-cooldown",
                            "$remaining", String.valueOf(cooldowns.getRemainingCooldown(player, NAME)),
                            "$weapon", plugin.getConfig().getString("weapons." + NAME + ".display-name", NAME));
                    return;
                }

                String path = "weapons." + NAME;
                double range = plugin.getConfig().getDouble(path + ".pull-range", 15);

                Location eye = player.getEyeLocation();
                World world = player.getWorld();
                Vector dir = eye.getDirection().normalize();
                Location handPos = eye.clone().subtract(0, 0.3, 0);

                world.playSound(eye, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.6f);

                RayTraceResult hit = world.rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.5,
                        en -> en instanceof Player candidate
                                && !candidate.equals(player)
                                && candidate.getGameMode() != GameMode.SPECTATOR
                                && !candidate.isDead());

                if (hit == null || !(hit.getHitEntity() instanceof Player target)) {
                    // Промах: рисуем луч и даём короткую перезарядку, чтобы не спамили
                    Location end = hit != null
                            ? hit.getHitPosition().toLocation(world)
                            : eye.clone().add(dir.clone().multiply(range));
                    beam(world, handPos, end);

                    cooldowns.setCooldown(player, NAME, plugin.getConfig().getInt(path + ".miss-cooldown", 2));
                    return;
                }

                cooldowns.setCooldown(player, NAME, plugin.getConfig().getInt(path + ".ability-cooldown", 25));

                world.playSound(target.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, 1.0f, 0.5f);
                plugin.sendMessage(player, "on-pull", "$target", target.getName());

                pull(player, target);
            }

            private void pull(Player caster, Player target) {
                String path = "weapons." + NAME;
                double speed = plugin.getConfig().getDouble(path + ".pull-speed", 1.2);
                double stop = plugin.getConfig().getDouble(path + ".pull-stop-distance", 2.0);
                int maxTicks = 25;

                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (!caster.isOnline() || !target.isOnline()
                                || caster.isDead() || target.isDead()
                                || !caster.getWorld().equals(target.getWorld())
                                || ticks >= maxTicks) {
                            cancel();
                            return;
                        }

                        Location from = caster.getLocation().add(0, 1.0, 0);
                        Location to = target.getLocation().add(0, 1.0, 0);

                        Vector delta = from.toVector().subtract(to.toVector());
                        double dist = delta.length();

                        if (dist <= stop) {
                            cancel();
                            return;
                        }

                        Vector velocity = delta.normalize().multiply(Math.min(speed, dist - stop + 0.2));
                        if (target.isOnGround()) {
                            velocity.setY(Math.max(velocity.getY(), 0.2)); // отрываем от земли, чтобы не тормозило трение
                        }

                        target.setVelocity(velocity);
                        target.setFallDistance(0);

                        beam(target.getWorld(), caster.getEyeLocation().subtract(0, 0.3, 0), to);
                        target.getWorld().spawnParticle(Particle.PORTAL, to, 8, 0.3, 0.5, 0.3, 0.2);

                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }

            private void beam(World world, Location from, Location to) {
                Vector dir = to.toVector().subtract(from.toVector());
                double len = dir.length();
                if (len < 0.1) return;
                dir.normalize();

                int i = 0;
                for (double d = 0; d < len; d += 0.4) {
                    Location point = from.clone().add(dir.clone().multiply(d));
                    world.spawnParticle(Particle.DUST, point, 1, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(i++ % 2 == 0 ? MAGENTA : PINK, 1.2f));
                }
            }

            // Жатва: за убийство игрока лечит
            @Override
            public void onKill(Player killer, Player victim) {
                double heal = plugin.getConfig().getDouble("weapons." + NAME + ".harvest-heal", 4);

                AttributeInstance max = killer.getAttribute(Attribute.MAX_HEALTH);
                double maxHealth = max == null ? 20 : max.getValue();
                killer.setHealth(Math.min(maxHealth, killer.getHealth() + heal));

                Location loc = killer.getLocation().add(0, 1.5, 0);
                killer.getWorld().spawnParticle(Particle.HEART, loc, 5, 0.4, 0.3, 0.4, 0);
                killer.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);

                plugin.sendActionBar(killer, "on-harvest", "$hearts", fmt(heal / 2.0));
            }
        });
    }

    /** 5.0 -> "5", 1.5 -> "1.5" */
    private static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void register(Weapon weapon) {
        weapons.put(weapon.getName(), weapon);
    }

    public @Nullable Weapon getWeapon(String name) {
        return weapons.get(name);
    }

    public Set<String> getWeaponNames() {
        return Collections.unmodifiableSet(weapons.keySet());
    }

    public void process(Player player, String weaponName) {
        Weapon weapon = weapons.get(weaponName);
        if (weapon != null) {
            weapon.useAbility(player);
        }
    }

    public void processHit(Player attacker, Player victim, String weaponName, boolean critical) {
        Weapon weapon = weapons.get(weaponName);
        if (weapon != null) {
            weapon.onHit(attacker, victim, critical);
        }
    }

    public void processKill(Player killer, Player victim, String weaponName) {
        Weapon weapon = weapons.get(weaponName);
        if (weapon != null) {
            weapon.onKill(killer, victim);
        }
    }
}
