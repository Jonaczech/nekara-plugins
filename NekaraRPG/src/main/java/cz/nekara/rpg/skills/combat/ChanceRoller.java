package cz.nekara.rpg.skills.combat;

@FunctionalInterface
public interface ChanceRoller {
    boolean succeeds(double chance);
}
