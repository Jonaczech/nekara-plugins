package cz.nekara.rpg.fishing;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class FishingCatchDeliveredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack catchItem;
    private final int vanillaExperience;
    private final UUID catchId;

    public FishingCatchDeliveredEvent(
        Player player,
        ItemStack catchItem,
        int vanillaExperience,
        UUID catchId
    ) {
        this.player = player;
        this.catchItem = catchItem.clone();
        this.vanillaExperience = vanillaExperience;
        this.catchId = catchId;
    }

    public Player player() {
        return player;
    }

    public ItemStack catchItem() {
        return catchItem.clone();
    }

    public int vanillaExperience() {
        return vanillaExperience;
    }

    public UUID catchId() {
        return catchId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
