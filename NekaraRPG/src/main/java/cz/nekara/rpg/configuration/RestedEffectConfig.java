package cz.nekara.rpg.configuration;

public record RestedEffectConfig(
        boolean hasteEnabled,
        int hasteAmplifier,
        boolean ambient,
        boolean particles,
        boolean icon
) {
}
