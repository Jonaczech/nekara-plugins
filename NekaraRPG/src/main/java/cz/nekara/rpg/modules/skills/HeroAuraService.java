package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.skills.milestones.PowerMilestoneId;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Lightweight original Nekara aura for players who complete the full Power journey. */
final class HeroAuraService {
    private static final double AURA_RADIUS = 0.72;
    private static final double PHASE_STEP = Math.PI / 12.0;
    private static final Particle.DustOptions HERO_ORANGE =
            new Particle.DustOptions(Color.fromRGB(255, 142, 38), 0.85f);

    private final NekaraRPGPlugin plugin;
    private final SkillsModule skills;
    private BukkitTask task;
    private double phase;
    private int pulse;

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
    }

    private void tick() {
        phase = (phase + PHASE_STEP) % (Math.PI * 2.0);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                    && skills.hasPowerMilestone(player.getUniqueId(), PowerMilestoneId.HERO_AURA)) {
                spawnAura(player);
            }
        }
    }

    private void spawnAura(Player player) {
        Location base = player.getLocation();
        double height = 0.55 + ((phase / (Math.PI * 2.0)) * 0.9);
        spawn(player, Particle.END_ROD, base, phase, AURA_RADIUS, height);
        if (++pulse % 3 == 0) {
            spawnOrangeDust(player, base, phase + Math.PI, AURA_RADIUS, 1.2);
        }
    }

    private static void spawnOrangeDust(
            Player player,
            Location base,
            double angle,
            double radius,
            double height
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
            HERO_ORANGE
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
