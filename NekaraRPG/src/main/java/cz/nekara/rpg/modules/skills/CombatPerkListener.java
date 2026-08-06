package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.WeaponCombatConfig;
import cz.nekara.rpg.items.weapons.WeaponCatalog;
import cz.nekara.rpg.items.weapons.WeaponDefinition;
import cz.nekara.rpg.items.weapons.WeaponFamily;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillPresentation;
import cz.nekara.rpg.skills.combat.BleedRegistry;
import cz.nekara.rpg.skills.combat.ArmorProtectionResolver;
import cz.nekara.rpg.skills.combat.DamageTypeResolver;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.stats.StatId;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.block.Action;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class CombatPerkListener implements Listener {
    private static final long ACTIVE_ABILITY_COOLDOWN_MILLIS = 5_000L;
    private static final long SURVIVAL_PROC_COOLDOWN_MILLIS = 20_000L;
    private static final int MAXIMUM_ACTIVE_BLEEDS = 2_048;
    private static final int BLEED_TICKS = 3;

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final NamespacedKey chargedArrowKey;
    private final NamespacedKey scoutArrowKey;
    private final NamespacedKey lightMobilityKey;
    private final NamespacedKey weaponMobilityKey;
    private final NamespacedKey heavyStabilityKey;
    private final NamespacedKey proficiencyMobilityKey;
    private final NamespacedKey smithingWeaponDamageKey;
    private final NamespacedKey smithingArmorKey;
    private final SmithingTier.Keys smithingTierKeys;
    private final NamespacedKey coatingTypeKey;
    private final NamespacedKey coatingDurationKey;
    private final NamespacedKey coatingAmplifierKey;
    private final NamespacedKey coatingChargesKey;
    private final Map<CooldownKey, Long> cooldowns = new HashMap<>();
    private final Map<UUID, String> proficiencyWarnings = new HashMap<>();
    private final BleedRegistry bleeds = new BleedRegistry(MAXIMUM_ACTIVE_BLEEDS);
    private BukkitTask bleedTask;
    private boolean enabled;

    CombatPerkListener(NekaraRPGPlugin plugin, SkillsModule module) {
        this.plugin = plugin;
        this.module = module;
        this.chargedArrowKey = new NamespacedKey(plugin, "skills_charged_arrow");
        this.scoutArrowKey = new NamespacedKey(plugin, "skills_scout_arrow");
        this.lightMobilityKey = new NamespacedKey(plugin, "skills_light_mobility");
        this.weaponMobilityKey = new NamespacedKey(plugin, "skills_weapon_mobility");
        this.heavyStabilityKey = new NamespacedKey(plugin, "skills_heavy_stability");
        this.proficiencyMobilityKey = new NamespacedKey(plugin, "skills_equipment_proficiency_mobility");
        this.smithingWeaponDamageKey = new NamespacedKey(plugin, "smithing_weapon_damage");
        this.smithingArmorKey = new NamespacedKey(plugin, "smithing_armor");
        this.smithingTierKeys = SmithingTier.keys(plugin);
        this.coatingTypeKey = new NamespacedKey(plugin, "skills_coating_type");
        this.coatingDurationKey = new NamespacedKey(plugin, "skills_coating_duration");
        this.coatingAmplifierKey = new NamespacedKey(plugin, "skills_coating_amplifier");
        this.coatingChargesKey = new NamespacedKey(plugin, "skills_coating_charges");
    }

    void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        bleedTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBleeds, 20L, 20L);
        enabled = true;
        Bukkit.getOnlinePlayers().forEach(this::refreshPlayer);
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        if (bleedTask != null) {
            bleedTask.cancel();
            bleedTask = null;
        }
        bleeds.clear();
        cooldowns.clear();
        proficiencyWarnings.clear();
        Bukkit.getOnlinePlayers().forEach(this::removeArmorModifiers);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyCombatPerks(EntityDamageByEntityEvent event) {
        if (SyntheticCombatGuard.isActive()) {
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker != null && !(event.getDamager() instanceof Projectile) && supported(attacker)) {
            applyWeaponProficiency(event, attacker);
        }
        if (attacker != null && event.getEntity() instanceof LivingEntity target
            && !(target instanceof Player) && supported(attacker)) {
            SkillId attackSkill = event.getDamager() instanceof Projectile
                ? SkillId.ARCHERY
                : SkillEquipmentPolicy.meleeSkill(attacker.getInventory().getItemInMainHand()).orElse(null);
            if (attackSkill != null) {
                WeaponDefinition weapon = event.getDamager() instanceof Projectile ? null
                    : WeaponCatalog.resolve(attacker.getInventory().getItemInMainHand()).orElse(null);
                if (weapon == null || hasRequiredOffhand(attacker, weapon)) {
                    applyAttack(event, attacker, target, attackSkill, weapon);
                }
            }
            if (target instanceof Animals) {
                module.runtimeState(attacker.getUniqueId(), SkillId.FARMING).ifPresent(state ->
                    event.setDamage(event.getDamage()
                        * state.stats().value(StatId.ANIMAL_DAMAGE_MULTIPLIER)));
            }
        }
        if (event.getEntity() instanceof Player defender && supported(defender)) {
            applyDefense(event, defender);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void bypassArmorForBleed(EntityDamageEvent event) {
        if (!SyntheticCombatGuard.isBleed()) {
            return;
        }
        if (event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0.0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void prepareArrow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
            || !(event.getProjectile() instanceof AbstractArrow arrow) || !supported(player)) {
            return;
        }
        Optional<SkillRuntimeState> state = module.runtimeState(player.getUniqueId(), SkillId.ARCHERY);
        if (state.isEmpty()) {
            module.preloadProfile(player);
            return;
        }
        double accuracy = state.get().stats().value(StatId.ACCURACY);
        if (accuracy > 0.0) {
            Vector current = arrow.getVelocity();
            double speed = current.length();
            if (speed > 0.0) {
                Vector exact = player.getEyeLocation().getDirection().normalize().multiply(speed);
                arrow.setVelocity(current.multiply(1.0 - accuracy).add(exact.multiply(accuracy)));
            }
        }
        if (event.getForce() >= 0.95F && state.get().has(MechanicId.CHARGED_SHOT)) {
            arrow.getPersistentDataContainer().set(chargedArrowKey, PersistentDataType.BYTE, (byte) 1);
        }
        ItemStack consumed = event.getConsumable();
        if (state.get().has(MechanicId.CUSTOM_ARROW_RECIPES)
            && consumed != null && consumed.getPersistentDataContainer().has(scoutArrowKey, PersistentDataType.BYTE)) {
            arrow.getPersistentDataContainer().set(scoutArrowKey, PersistentDataType.BYTE, (byte) 1);
        }
        double saveChance = state.get().stats().value(StatId.AMMO_CONSUMPTION_REDUCTION);
        if (saveChance > 0.0 && ThreadLocalRandom.current().nextDouble() < saveChance) {
            event.setConsumeArrow(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void markScoutArrowTarget(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)
            || !arrow.getPersistentDataContainer().has(scoutArrowKey, PersistentDataType.BYTE)
            || !(event.getHitEntity() instanceof LivingEntity target)) {
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0, false, true, true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void grapple(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || !player.isSneaking()
            || !player.getInventory().getItemInMainHand().getType().isAir()
            || !(event.getRightClicked() instanceof Enemy target) || !supported(player)) {
            return;
        }
        Optional<SkillRuntimeState> state = module.runtimeState(player.getUniqueId(), SkillId.MARTIAL_ARTS);
        if (state.isEmpty() || !state.get().has(MechanicId.GRAPPLE)
            || !acquire(player.getUniqueId(), "grapple", ACTIVE_ABILITY_COOLDOWN_MILLIS)) {
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, true, true));
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void prepareCombatTechnique(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR || event.getHand() != EquipmentSlot.HAND
            || !event.getPlayer().isSneaking() || !supported(event.getPlayer())) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (weapon.getType().isAir() && offhand.getType().isAir()) {
            if (module.runtimeState(player.getUniqueId(), SkillId.MARTIAL_ARTS)
                .map(state -> state.has(MechanicId.MEDITATION)).orElse(false)
                && acquire(player.getUniqueId(), "meditation", 30_000L)) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, 100, 0, false, true, true));
                event.setCancelled(true);
            }
            return;
        }
        Optional<SkillId> weaponSkill = SkillEquipmentPolicy.meleeSkill(weapon)
            .filter(skill -> skill == SkillId.LIGHT_WEAPONS || skill == SkillId.HEAVY_WEAPONS);
        if (weaponSkill.isEmpty()
            || !module.runtimeState(player.getUniqueId(), weaponSkill.get())
                .map(state -> state.has(MechanicId.WEAPON_COATING)).orElse(false)
            || !(offhand.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }
        Optional<PotionEffect> coating = potionMeta.getAllEffects().stream()
            .filter(effect -> !effect.getType().isInstant())
            .findFirst();
        if (coating.isEmpty()) {
            return;
        }
        PotionEffect effect = coating.get();
        ItemMeta weaponMeta = weapon.getItemMeta();
        weaponMeta.getPersistentDataContainer().set(
            coatingTypeKey, PersistentDataType.STRING, effect.getType().getKey().asString());
        weaponMeta.getPersistentDataContainer().set(
            coatingDurationKey, PersistentDataType.INTEGER, Math.min(100, effect.getDuration()));
        weaponMeta.getPersistentDataContainer().set(
            coatingAmplifierKey, PersistentDataType.INTEGER, Math.min(1, effect.getAmplifier()));
        weaponMeta.getPersistentDataContainer().set(coatingChargesKey, PersistentDataType.INTEGER, 3);
        weapon.setItemMeta(weaponMeta);
        if (player.getGameMode() != GameMode.CREATIVE) {
            offhand.subtract(1);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(
                new ItemStack(org.bukkit.Material.GLASS_BOTTLE, 1));
            leftovers.values().forEach(item ->
                player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveRegeneration(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        SkillEquipmentPolicy.armorSkill(player.getInventory())
            .filter(skill -> skill == SkillId.HEAVY_ARMOR)
            .flatMap(skill -> module.runtimeState(player.getUniqueId(), skill))
            .ifPresent(state -> event.setAmount(event.getAmount()
                * (1.0 + state.stats().value(StatId.HEALTH_REGENERATION))));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void conserveFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        SkillEquipmentPolicy.armorSkill(player.getInventory()).flatMap(skill ->
            module.runtimeState(player.getUniqueId(), skill)).ifPresent(state -> {
                double reduction = state.stats().value(StatId.HUNGER_CONSUMPTION_REDUCTION);
                if (reduction > 0.0 && ThreadLocalRandom.current().nextDouble() < reduction) {
                    event.setFoodLevel(Math.min(20, event.getFoodLevel() + 1));
                }
            });
    }

    @EventHandler
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshPlayer(player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.keySet().removeIf(key -> key.playerId().equals(playerId));
        proficiencyWarnings.remove(playerId);
        removeArmorModifiers(event.getPlayer());
    }

    void refreshPlayer(Player player) {
        if (!enabled || !player.isOnline()) {
            return;
        }
        removeArmorModifiers(player);
        Optional<SkillRuntimeState> lightState = module.runtimeState(player.getUniqueId(), SkillId.LIGHT_ARMOR);
        Optional<SkillRuntimeState> heavyState = module.runtimeState(player.getUniqueId(), SkillId.HEAVY_ARMOR);
        if (lightState.isEmpty() || heavyState.isEmpty()) {
            module.preloadProfile(player);
        }
        double armorPenalty = EquipmentMobilityPolicy.armorPenalty(
            armorMaterials(player),
            has(lightState, MechanicId.LIGHT_ARMOR_CHAINMAIL_MOBILITY),
            has(lightState, MechanicId.LIGHT_ARMOR_DIAMOND_MOBILITY),
            has(heavyState, MechanicId.HEAVY_ARMOR_IRON_MOBILITY),
            has(heavyState, MechanicId.HEAVY_ARMOR_NETHERITE_MOBILITY));
        addModifier(player, Attribute.MOVEMENT_SPEED, lightMobilityKey, armorPenalty);
        module.cachedProfile(player.getUniqueId()).ifPresent(profile -> {
            int untrainedPieces = untrainedArmorPieces(player, profile);
            if (untrainedPieces > 0) {
                addModifier(player, Attribute.MOVEMENT_SPEED, proficiencyMobilityKey,
                    EquipmentProficiencyPolicy.armorMovementPenalty(untrainedPieces));
                warn(player, firstUntrainedArmorRequirement(player, profile).orElseThrow(),
                    "Tuto zbroj ještě nemůžeš nosit");
            }
        });
        applyWeaponMobility(player);

        if (SkillEquipmentPolicy.armorSkill(player.getInventory()).orElse(null) == SkillId.HEAVY_ARMOR) {
            heavyState.ifPresent(state -> {
            double stability = 0.05;
            if (state.has(MechanicId.HEAVY_ARMOR_SET_BONUS)) {
                stability += 0.05;
            }
            addModifier(player, Attribute.KNOCKBACK_RESISTANCE, heavyStabilityKey, stability);
            });
        }
    }

    private void applyWeaponMobility(Player player) {
        WeaponCatalog.resolve(player.getInventory().getItemInMainHand()).ifPresent(weapon -> {
            Optional<SkillRuntimeState> state = module.runtimeState(player.getUniqueId(), weapon.family().skill());
            if (state.isEmpty()) {
                module.preloadProfile(player);
                return;
            }
            boolean heavy = weapon.family().skill() == SkillId.HEAVY_WEAPONS;
            double penalty = EquipmentMobilityPolicy.weaponPenalty(weapon,
                has(state, heavy ? MechanicId.HEAVY_WEAPON_IRON_MOBILITY : MechanicId.LIGHT_WEAPON_IRON_MOBILITY),
                has(state, heavy ? MechanicId.HEAVY_WEAPON_DIAMOND_MOBILITY : MechanicId.LIGHT_WEAPON_DIAMOND_MOBILITY),
                has(state, heavy ? MechanicId.HEAVY_WEAPON_NETHERITE_MOBILITY : MechanicId.LIGHT_WEAPON_NETHERITE_MOBILITY));
            addModifier(player, Attribute.MOVEMENT_SPEED, weaponMobilityKey, penalty);
            module.cachedProfile(player.getUniqueId()).ifPresent(profile -> {
                mobilityRequirement(weapon, player.getInventory().getItemInMainHand()).ifPresent(requirement -> {
                    if (module.skillLevel(profile, requirement.skill()) < requirement.requiredLevel()) {
                        addModifier(player, Attribute.MOVEMENT_SPEED, proficiencyMobilityKey,
                            EquipmentProficiencyPolicy.weaponMovementPenalty(requirement.skill()));
                        warn(player, requirement, "Tuto zbraň ještě neumíš používat");
                    }
                });
            });
        });
    }

    private static boolean has(Optional<SkillRuntimeState> state, MechanicId mechanic) {
        return state.map(value -> value.has(mechanic)).orElse(false);
    }

    private static org.bukkit.Material[] armorMaterials(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        org.bukkit.Material[] materials = new org.bukkit.Material[armor.length];
        for (int index = 0; index < armor.length; index++) {
            materials[index] = armor[index] == null ? org.bukkit.Material.AIR : armor[index].getType();
        }
        return materials;
    }

    private void applyAttack(
        EntityDamageByEntityEvent event,
        Player attacker,
        LivingEntity target,
        SkillId skill,
        WeaponDefinition weapon
    ) {
        Optional<SkillRuntimeState> state = module.runtimeState(attacker.getUniqueId(), skill);
        if (state.isEmpty()) {
            module.preloadProfile(attacker);
            return;
        }
        SkillRuntimeState runtime = state.get();
        double multiplier = runtime.stats().value(StatId.DAMAGE_MULTIPLIER);
        WeaponFamily family = weapon == null ? null : weapon.family();
        WeaponCombatConfig weaponConfig = module.weaponCombatConfig();
        double criticalChance = runtime.stats().value(StatId.CRITICAL_CHANCE)
            + (family == null ? 0.0 : weaponConfig.criticalChance(family));
        if (criticalChance > 0.0 && ThreadLocalRandom.current().nextDouble() < criticalChance) {
            multiplier *= runtime.stats().value(StatId.CRITICAL_DAMAGE_MULTIPLIER);
            playEffectSound(target, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9F, 1.1F);
        }
        if (skill == SkillId.HEAVY_WEAPONS && attacker.getAttackCooldown() >= 0.9F) {
            multiplier *= runtime.stats().value(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER);
            multiplier *= 1.0 + runtime.stats().value(StatId.ARMOR_PENETRATION) * 0.5;
        }
        if (family != null && weaponConfig.armorPenetration(family) > 0.0) {
            multiplier *= 1.0 + weaponConfig.armorPenetration(family) * 0.5;
        }
        if (family == WeaponFamily.DAGGER && isRearAttack(attacker, target)) {
            multiplier *= 1.0 + weaponConfig.rearAttackBonus(family);
            playEffectSound(target, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.75F, 1.35F);
        }
        if (skill == SkillId.MARTIAL_ARTS && attacker.getAttackCooldown() >= 0.95F
            && runtime.has(MechanicId.PUNCH_HOLDING)) {
            multiplier *= 1.15;
        }
        if (event.getDamager() instanceof AbstractArrow arrow
            && arrow.getPersistentDataContainer().has(chargedArrowKey, PersistentDataType.BYTE)) {
            multiplier *= 1.25;
        }
        ItemStack craftedWeapon = attacker.getInventory().getItemInMainHand();
        double craftedDamage = SmithingTier.weaponBonusActive(craftedWeapon, smithingTierKeys)
            ? itemDouble(craftedWeapon, smithingWeaponDamageKey) : 0.0;
        double weaponCondition = switch (SmithingTier.state(craftedWeapon, smithingTierKeys)) {
            case UNPROCESSED, HEATED -> 0.75;
            default -> 1.0;
        };
        event.setDamage(event.getDamage() * Math.max(0.0, multiplier) * weaponCondition + craftedDamage);
        double bleedChance = runtime.stats().value(StatId.BLEED_CHANCE)
            + (family == null ? 0.0 : weaponConfig.bleedChance(family));
        if (bleedChance > 0.0 && ThreadLocalRandom.current().nextDouble() < bleedChance) {
            double damagePerTick = Math.min(4.0, Math.max(0.25,
                event.getDamage() * 0.08 * runtime.stats().value(StatId.BLEED_DAMAGE_MULTIPLIER)));
            bleeds.apply(target.getUniqueId(), attacker.getUniqueId(), damagePerTick, BLEED_TICKS);
            playEffectSound(target, Sound.ENTITY_BEE_STING, 0.8F, 0.85F);
        }
        if (family == WeaponFamily.HAMMER && weaponConfig.hammerStunChance() > 0.0
            && ThreadLocalRandom.current().nextDouble() < weaponConfig.hammerStunChance()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, true, true));
            playEffectSound(target, Sound.BLOCK_ANVIL_LAND, 0.55F, 1.35F);
        }
        if (family == WeaponFamily.GREATSWORD && weaponConfig.greatswordCleaveDamageMultiplier() > 0.0) {
            applyCleave(attacker, target, event.getDamage() * weaponConfig.greatswordCleaveDamageMultiplier());
        }
        if (module.runtimeState(attacker.getUniqueId(), SkillId.ENCHANTING)
            .map(value -> value.has(MechanicId.HEXBLADE)).orElse(false)) {
            target.setFireTicks(Math.max(target.getFireTicks(), 40));
        }
        if (!(event.getDamager() instanceof Projectile)) {
            applyWeaponCoating(attacker.getInventory().getItemInMainHand(), target);
        }

        if (skill == SkillId.MARTIAL_ARTS) {
            if (runtime.stats().value(StatId.STUN_CHANCE) > ThreadLocalRandom.current().nextDouble()) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, true, true));
            }
            if (attacker.isSneaking() && runtime.has(MechanicId.UPPERCUT)
                && acquire(attacker.getUniqueId(), "uppercut", ACTIVE_ABILITY_COOLDOWN_MILLIS)) {
                target.setVelocity(target.getVelocity().setY(0.65));
            } else if (!attacker.isOnGround() && attacker.isSprinting() && runtime.has(MechanicId.DROPKICK)
                && acquire(attacker.getUniqueId(), "dropkick", ACTIVE_ABILITY_COOLDOWN_MILLIS)) {
                Vector knockback = attacker.getLocation().getDirection().setY(0.25).normalize().multiply(0.8);
                target.setVelocity(target.getVelocity().add(knockback));
                event.setDamage(event.getDamage() * 1.20);
            }
        }
    }

    private static boolean hasRequiredOffhand(Player player, WeaponDefinition weapon) {
        return !weapon.family().requiresEmptyOffhand()
            || player.getInventory().getItemInOffHand().getType().isAir();
    }

    private static boolean isRearAttack(Player attacker, LivingEntity target) {
        Vector targetFacing = target.getLocation().getDirection().setY(0.0);
        Vector towardAttacker = attacker.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0.0);
        if (targetFacing.lengthSquared() < 0.0001 || towardAttacker.lengthSquared() < 0.0001) {
            return false;
        }
        return targetFacing.normalize().dot(towardAttacker.normalize()) < -0.5;
    }

    private void applyCleave(Player attacker, LivingEntity primaryTarget, double damage) {
        if (damage <= 0.0) {
            return;
        }
        int affected = 0;
        for (Entity nearby : primaryTarget.getNearbyEntities(2.25, 1.5, 2.25)) {
            if (affected >= 4 || nearby == attacker || nearby instanceof Player
                || !(nearby instanceof LivingEntity target) || target.isDead()) {
                continue;
            }
            SyntheticCombatGuard.run(() -> target.damage(damage, attacker));
            affected++;
        }
        if (affected > 0) {
            primaryTarget.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK, primaryTarget.getLocation().add(0.0, 0.7, 0.0), 1);
            playEffectSound(primaryTarget, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9F, 0.9F);
        }
    }

    private static void playEffectSound(LivingEntity target, Sound sound, float volume, float pitch) {
        target.getWorld().playSound(target.getLocation(), sound, volume, pitch);
    }

    private void tickBleeds() {
        for (BleedRegistry.BleedTick tick : bleeds.advance()) {
            Entity targetEntity = Bukkit.getEntity(tick.targetId());
            if (!(targetEntity instanceof LivingEntity target)
                || !target.isValid() || target.isDead()) {
                bleeds.remove(tick.targetId());
                continue;
            }
            Entity sourceEntity = Bukkit.getEntity(tick.sourceId());
            target.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR, target.getLocation().add(0.0, 0.8, 0.0), 1,
                0.15, 0.15, 0.15, 0.0);
            SyntheticCombatGuard.runBleed(() -> {
                if (sourceEntity instanceof LivingEntity source && source.isValid()) {
                    target.damage(tick.damage(), source);
                } else {
                    target.damage(tick.damage());
                }
            });
        }
    }

    private void applyDefense(EntityDamageByEntityEvent event, Player defender) {
        module.cachedProfile(defender.getUniqueId()).ifPresent(profile -> {
            int untrainedPieces = untrainedArmorPieces(defender, profile);
            if (untrainedPieces > 0) {
                event.setDamage(event.getDamage()
                    * EquipmentProficiencyPolicy.armorDamageMultiplier(untrainedPieces));
                firstUntrainedArmorRequirement(defender, profile).ifPresent(requirement ->
                    warn(defender, requirement, "Tuto zbroj ještě nemůžeš nosit"));
            }
        });
        Optional<SkillId> armorSkill = SkillEquipmentPolicy.armorSkill(defender.getInventory());
        if (armorSkill.isEmpty()) {
            return;
        }
        Optional<SkillRuntimeState> state = module.runtimeState(defender.getUniqueId(), armorSkill.get());
        if (state.isEmpty()) {
            module.preloadProfile(defender);
            return;
        }
        SkillRuntimeState runtime = state.get();
        double dodgeChance = runtime.stats().value(StatId.DODGE_CHANCE);
        if (dodgeChance > 0.0 && ThreadLocalRandom.current().nextDouble() < dodgeChance) {
            event.setCancelled(true);
            return;
        }
        double baseArmorEffectiveness = SkillEquipmentPolicy.wearsLeatherArmor(defender.getInventory())
            ? 1.0 : 0.90;
        int unfinishedPieces = 0;
        double craftedArmor = 0.0;
        for (ItemStack item : defender.getInventory().getArmorContents()) {
            if (SmithingTier.armorBonusActive(item, smithingTierKeys)) {
                craftedArmor += itemDouble(item, smithingArmorKey);
            } else if (SmithingTier.state(item, smithingTierKeys) != SmithingTier.ProcessingState.NONE) {
                unfinishedPieces++;
            }
        }
        baseArmorEffectiveness *= Math.max(0.65, 1.0 - unfinishedPieces * 0.12);
        double armorMultiplier = baseArmorEffectiveness * runtime.stats().value(StatId.ARMOR_MULTIPLIER)
            * (1.0 + craftedArmor * 0.04);
        event.setDamage(event.getDamage() / armorMultiplier);
        DamageTypeResolver.resolve(
            event.getDamager() instanceof Player attacker ? attacker.getInventory().getItemInMainHand() : null,
            event.getDamager() instanceof Projectile
        ).ifPresent(type -> event.setDamage(event.getDamage()
            * ArmorProtectionResolver.damageMultiplier(defender.getInventory().getArmorContents(), type)));
        double reflection = runtime.stats().value(StatId.DAMAGE_REFLECTION);
        if (reflection > 0.0) {
            livingDamager(event.getDamager()).ifPresent(attacker -> {
                double amount = Math.min(4.0, event.getFinalDamage() * reflection);
                if (amount > 0.0) {
                    Bukkit.getScheduler().runTask(plugin, () -> SyntheticCombatGuard.run(
                        () -> attacker.damage(amount, defender)));
                }
            });
        }
        Bukkit.getScheduler().runTask(plugin, () -> triggerSurvivalPerk(defender, armorSkill.get(), runtime));
    }

    private void triggerSurvivalPerk(Player player, SkillId skill, SkillRuntimeState state) {
        if (!player.isOnline() || player.isDead() || player.getHealth() > player.getMaxHealth() * 0.25) {
            return;
        }
        if (skill == SkillId.LIGHT_ARMOR && state.has(MechanicId.ADRENALINE)
            && acquire(player.getUniqueId(), "adrenaline", SURVIVAL_PROC_COOLDOWN_MILLIS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, false, true, true));
        } else if (skill == SkillId.HEAVY_ARMOR && state.has(MechanicId.RAGE)
            && acquire(player.getUniqueId(), "rage", SURVIVAL_PROC_COOLDOWN_MILLIS)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, false, true, true));
        }
    }

    private boolean acquire(UUID playerId, String ability, long cooldownMillis) {
        long now = System.currentTimeMillis();
        CooldownKey key = new CooldownKey(playerId, ability);
        Long expiresAt = cooldowns.get(key);
        if (expiresAt != null && expiresAt > now) {
            return false;
        }
        cooldowns.put(key, now + cooldownMillis);
        return true;
    }

    private void applyWeaponCoating(ItemStack weapon, LivingEntity target) {
        Integer charges = weapon.getPersistentDataContainer().get(
            coatingChargesKey, PersistentDataType.INTEGER);
        String effectKey = weapon.getPersistentDataContainer().get(
            coatingTypeKey, PersistentDataType.STRING);
        Integer duration = weapon.getPersistentDataContainer().get(
            coatingDurationKey, PersistentDataType.INTEGER);
        Integer amplifier = weapon.getPersistentDataContainer().get(
            coatingAmplifierKey, PersistentDataType.INTEGER);
        NamespacedKey key = effectKey == null ? null : NamespacedKey.fromString(effectKey);
        PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
        if (charges == null || charges < 1 || duration == null || amplifier == null || type == null) {
            return;
        }
        target.addPotionEffect(new PotionEffect(
            type, Math.max(1, duration), Math.max(0, amplifier), false, true, true));
        ItemMeta meta = weapon.getItemMeta();
        if (charges == 1) {
            meta.getPersistentDataContainer().remove(coatingTypeKey);
            meta.getPersistentDataContainer().remove(coatingDurationKey);
            meta.getPersistentDataContainer().remove(coatingAmplifierKey);
            meta.getPersistentDataContainer().remove(coatingChargesKey);
        } else {
            meta.getPersistentDataContainer().set(
                coatingChargesKey, PersistentDataType.INTEGER, charges - 1);
        }
        weapon.setItemMeta(meta);
    }

    private void removeArmorModifiers(Player player) {
        removeModifier(player, Attribute.MOVEMENT_SPEED, lightMobilityKey);
        removeModifier(player, Attribute.MOVEMENT_SPEED, weaponMobilityKey);
        removeModifier(player, Attribute.MOVEMENT_SPEED, proficiencyMobilityKey);
        removeModifier(player, Attribute.KNOCKBACK_RESISTANCE, heavyStabilityKey);
    }

    private void applyWeaponProficiency(EntityDamageByEntityEvent event, Player attacker) {
        ItemStack held = attacker.getInventory().getItemInMainHand();
        Optional<EquipmentProficiencyPolicy.Requirement> requirement = WeaponCatalog.resolve(held)
            .flatMap(EquipmentProficiencyPolicy::weapon)
            .or(() -> EquipmentProficiencyPolicy.heldTool(held));
        requirement.ifPresent(value -> {
            module.cachedProfile(attacker.getUniqueId()).ifPresent(profile -> {
                if (module.skillLevel(profile, value.skill()) < value.requiredLevel()) {
                    event.setDamage(event.getDamage() * EquipmentProficiencyPolicy.weaponDamageMultiplier());
                    warn(attacker, value, "Tento předmět ještě neumíš používat");
                }
            });
        });
    }

    private int untrainedArmorPieces(Player player, SkillProfile profile) {
        int untrained = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (EquipmentProficiencyPolicy.armor(item).filter(requirement ->
                module.skillLevel(profile, requirement.skill()) < requirement.requiredLevel()).isPresent()) {
                untrained++;
            }
        }
        return untrained;
    }

    private Optional<EquipmentProficiencyPolicy.Requirement> firstUntrainedArmorRequirement(
        Player player,
        SkillProfile profile
    ) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            Optional<EquipmentProficiencyPolicy.Requirement> requirement = EquipmentProficiencyPolicy.armor(item);
            if (requirement.isPresent()
                && module.skillLevel(profile, requirement.get().skill()) < requirement.get().requiredLevel()) {
                return requirement;
            }
        }
        return Optional.empty();
    }

    private static Optional<EquipmentProficiencyPolicy.Requirement> mobilityRequirement(
        WeaponDefinition weapon,
        ItemStack item
    ) {
        return weapon.family() == WeaponFamily.AXE
            ? EquipmentProficiencyPolicy.heldTool(item).or(() -> EquipmentProficiencyPolicy.weapon(weapon))
            : EquipmentProficiencyPolicy.weapon(weapon);
    }

    private void warn(Player player, EquipmentProficiencyPolicy.Requirement requirement, String message) {
        String key = requirement.skill().id() + ":" + requirement.requiredLevel();
        if (key.equals(proficiencyWarnings.put(player.getUniqueId(), key))) {
            return;
        }
        player.sendActionBar(net.kyori.adventure.text.Component.text(message + ": vyžaduje "
            + SkillPresentation.czechName(requirement.skill()) + " level " + requirement.requiredLevel() + ".",
            net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private static void addModifier(
        Player player,
        Attribute attribute,
        NamespacedKey key,
        double amount
    ) {
        if (amount == 0.0) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addTransientModifier(new AttributeModifier(
                key, amount, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private static void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(key);
        }
    }

    private static double itemDouble(ItemStack item, NamespacedKey key) {
        if (item == null || item.getType().isAir()) return 0.0;
        Double value = item.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return value == null ? 0.0 : Math.max(0.0, value);
    }

    private static Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static Optional<LivingEntity> livingDamager(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return Optional.of(living);
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living) {
            return Optional.of(living);
        }
        return Optional.empty();
    }

    private static boolean supported(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    private record CooldownKey(UUID playerId, String ability) {
    }
}
