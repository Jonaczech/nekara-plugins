package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.skills.milestones.PowerMilestoneId;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Lightweight original Nekara aura for players who complete the full Power journey. */
final class HeroAuraService {
    private static final double AURA_RADIUS = 0.72;
    private static final double PHASE_STEP = Math.PI / 12.0;
    private static final int GOLD_SPARK_INTERVAL = 4;
    private static final int GROUND_PULSE_INTERVAL = 18;
    private static final int MOVEMENT_TRAIL_INTERVAL = 2;
    private static final Particle.DustOptions HERO_ORANGE =
            new Particle.DustOptions(Color.fromRGB(255, 142, 38), 0.85f);
    private static final Particle.DustOptions HERO_GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 208, 72), 0.65f);

    private final NekaraRPGPlugin plugin;
    private final SkillsModule skills;
    private final Map<UUID, Location> previousLocations = new HashMap<>();
    private BukkitTask task;
    private double phase;
    private long auraTick;

    HeroAuraService(NekaraRPGPlugin plugin, SkillsModule skills) {
        this.plugin = plugin;
        this.skills = skills;
    }

    void enable() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
        }
    }

    void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        previousLocations.clear();
    }

    private void tick() {
        phase = (phase + PHASE_STEP) % (Math.PI * 2.0);
        auraTick++;
        Set<UUID> activeHeroes = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                    && skills.hasPowerMilestone(player.getUniqueId(), PowerMilestoneId.HERO_AURA)) {
                activeHeroes.add(player.getUniqueId());
                spawnAura(player);
            }
        }
        previousLocations.keySet().retainAll(activeHeroes);
    }

    private void spawnAura(Player player) {
        Location base = player.getLocation();
        double height = 0.55 + ((phase / (Math.PI * 2.0)) * 0.9);
        if (auraTick % 4 != 0) {
            spawn(player, Particle.END_ROD, base, phase, AURA_RADIUS, height);
        }
        if (auraTick % 2 == 0) {
            spawnOrangeDust(player, base, phase + Math.PI, AURA_RADIUS, 1.2);
        }
        if (auraTick % GOLD_SPARK_INTERVAL == 0) {
            spawnDust(player, base, phase + (Math.PI / 2.0), AURA_RADIUS * 0.8, 0.85, HERO_GOLD);
        }

        Location previous = previousLocations.put(player.getUniqueId(), base.clone());
        boolean moving = previous != null && previous.getWorld().equals(base.getWorld())
                && previous.distanceSquared(base) > 0.04;
        boolean ridingDragon = plugin.dragonsModule().isRidingDragon(player);
        if (ridingDragon && moving) {
            spawnDragonTrail(player, base);
        } else if (moving && auraTick % MOVEMENT_TRAIL_INTERVAL == 0) {
            spawnMovementSpark(player, base);
        }
        if (!ridingDragon && auraTick % GROUND_PULSE_INTERVAL == 0) {
            spawnGroundPulse(player, base);
        }
    }

    private static void spawnMovementSpark(Player player, Location base) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Location spark = base.clone().add(Math.sin(yaw) * 0.35, 0.18, -Math.cos(yaw) * 0.35);
        player.getWorld().spawnParticle(Particle.DUST, spark, 1, 0.0, 0.0, 0.0, 0.0, HERO_GOLD);
    }

    private static void spawnDragonTrail(Player player, Location base) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        for (double distance : new double[]{0.85, 1.45}) {
            Location spark = base.clone().add(Math.sin(yaw) * distance, 0.75, -Math.cos(yaw) * distance);
            player.getWorld().spawnParticle(Particle.DUST, spark, 1, 0.0, 0.0, 0.0, 0.0, HERO_GOLD);
        }
    }

    private static void spawnGroundPulse(Player player, Location base) {
        for (int index = 0; index < 4; index++) {
            double angle = (Math.PI / 2.0) * index;
            spawnDust(player, base, angle, 0.95, 0.08, index % 2 == 0 ? HERO_GOLD : HERO_ORANGE);
        }
    }

    private static void spawnOrangeDust(
            Player player,
            Location base,
            double angle,
            double radius,
            double height
    ) {
        spawnDust(player, base, angle, radius, height, HERO_ORANGE);
    }

    private static void spawnDust(
            Player player,
            Location base,
            double angle,
            double radius,
            double height,
            Particle.DustOptions dust
    ) {
        player.getWorld().spawnParticle(
            Particle.DUST,
            base.getX() + Math.cos(angle) * radius,
            base.getY() + height,
            base.getZ() + Math.sin(angle) * radius,
            1,
            0.0,
            0.0,
            0.0,
            0.0,
            dust
        );
    }

    private static void spawn(
            Player player,
            Particle particle,
            Location base,
            double angle,
            double radius,
            double height
    ) {
        player.getWorld().spawnParticle(
            particle,
            base.getX() + Math.cos(angle) * radius,
            base.getY() + height,
            base.getZ() + Math.sin(angle) * radius,
            1,
            0.0,
            0.0,
            0.0,
            0.0
        );
    }
}
