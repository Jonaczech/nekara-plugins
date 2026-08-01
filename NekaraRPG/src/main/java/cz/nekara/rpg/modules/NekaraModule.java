package cz.nekara.rpg.modules;

public interface NekaraModule {
    String id();

    void enable();

    void disable();

    default void reload() {
        disable();
        enable();
    }

    boolean isEnabled();
}
