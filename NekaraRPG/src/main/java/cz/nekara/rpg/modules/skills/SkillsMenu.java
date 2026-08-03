package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

final class SkillsMenu implements Listener {
    private static final int POWER_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final List<Integer> SKILL_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        31
    );
    private static final Map<SkillId, String> DISPLAY_NAMES = Map.ofEntries(
        Map.entry(SkillId.MARTIAL_ARTS, "Boj beze zbraně"),
        Map.entry(SkillId.TRADING, "Obchodování"),
        Map.entry(SkillId.SMITHING, "Kovářství"),
        Map.entry(SkillId.ENCHANTING, "Očarování"),
        Map.entry(SkillId.ALCHEMY, "Alchymie"),
        Map.entry(SkillId.MINING, "Těžba"),
        Map.entry(SkillId.WOODCUTTING, "Dřevorubectví"),
        Map.entry(SkillId.DIGGING, "Kopání"),
        Map.entry(SkillId.FARMING, "Zemědělství"),
        Map.entry(SkillId.FISHING, "Rybaření"),
        Map.entry(SkillId.LIGHT_WEAPONS, "Lehké zbraně"),
        Map.entry(SkillId.HEAVY_WEAPONS, "Těžké zbraně"),
        Map.entry(SkillId.ARCHERY, "Lukostřelba"),
        Map.entry(SkillId.LIGHT_ARMOR, "Lehká zbroj"),
        Map.entry(SkillId.HEAVY_ARMOR, "Těžká zbroj"),
        Map.entry(SkillId.POWER, "Moc")
    );

    private final NekaraRPGPlugin plugin;
    private boolean enabled;

    SkillsMenu(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
    }

    void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof SkillsHolder) {
                player.closeInventory();
            }
        }
    }

    void open(Player player, SkillProgressSnapshot snapshot, int spentPerkPoints) {
        SkillsHolder holder = new SkillsHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(
            holder,
            54,
            Component.text("Nekara — dovednosti", NamedTextColor.DARK_AQUA)
        );
        holder.inventory = inventory;
        GuiItems.fill(inventory);

        int availablePoints = Math.max(0, snapshot.power().level() - spentPerkPoints);
        inventory.setItem(POWER_SLOT, GuiItems.item(
            Material.NETHER_STAR,
            Component.text("Moc " + snapshot.power().level(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true),
            Component.text("Hlavní úroveň tvé cesty", NamedTextColor.GRAY),
            Component.text("Do další úrovně chybí " + snapshot.power().levelsUntilNext()
                + " dílčích úrovní", NamedTextColor.DARK_GRAY),
            Component.text("Volné body: " + availablePoints, NamedTextColor.GOLD)
        ));

        for (int index = 0; index < SkillId.gameplaySkills().size(); index++) {
            SkillId skill = SkillId.gameplaySkills().get(index);
            SkillLevelProgress progress = snapshot.skill(skill);
            inventory.setItem(SKILL_SLOTS.get(index), skillItem(skill, progress));
        }

        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        inventory.setItem(INFO_SLOT, GuiItems.item(
            Material.WRITABLE_BOOK,
            Component.text("Stezky rozvoje", NamedTextColor.GOLD),
            Component.text("Každá dovednost dosáhne nejvýše úrovně 100", NamedTextColor.GRAY),
            Component.text("Perkové větve se teprve rýsují", NamedTextColor.DARK_GRAY),
            Component.text("Tento náhled zatím nic neutratí", NamedTextColor.DARK_GRAY)
        ));
        player.openInventory(inventory);
    }

    void showUnavailable(Player player) {
        player.sendActionBar(Component.text(
            "Kronika dovedností je nyní uzavřená.", NamedTextColor.RED));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !(event.getView().getTopInventory().getHolder() instanceof SkillsHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!holder.ownerId.equals(player.getUniqueId())
            || event.getRawSlot() != BACK_SLOT) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.openMainMenu(player);
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SkillsHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack skillItem(SkillId skill, SkillLevelProgress progress) {
        Component next = progress.capped()
            ? Component.text("Stezka je završena", NamedTextColor.GOLD)
            : Component.text("XP " + progress.experienceIntoLevel() + "/"
                + progress.experienceForNextLevel(), NamedTextColor.DARK_GRAY);
        return GuiItems.item(
            material(skill),
            Component.text(displayName(skill), NamedTextColor.AQUA),
            Component.text("Úroveň " + progress.level() + "/100", NamedTextColor.GRAY),
            next,
            Component.text("Celkem XP " + progress.totalExperience(), NamedTextColor.DARK_GRAY)
        );
    }

    private Material material(SkillId skill) {
        return switch (skill) {
            case MARTIAL_ARTS -> Material.IRON_SWORD;
            case TRADING -> Material.EMERALD;
            case SMITHING -> Material.ANVIL;
            case ENCHANTING -> Material.ENCHANTING_TABLE;
            case ALCHEMY -> Material.BREWING_STAND;
            case MINING -> Material.IRON_PICKAXE;
            case WOODCUTTING -> Material.IRON_AXE;
            case DIGGING -> Material.IRON_SHOVEL;
            case FARMING -> Material.WHEAT;
            case FISHING -> Material.FISHING_ROD;
            case LIGHT_WEAPONS -> Material.IRON_SWORD;
            case HEAVY_WEAPONS -> Material.IRON_AXE;
            case ARCHERY -> Material.BOW;
            case LIGHT_ARMOR -> Material.CHAINMAIL_CHESTPLATE;
            case HEAVY_ARMOR -> Material.NETHERITE_CHESTPLATE;
            case POWER -> Material.NETHER_STAR;
        };
    }

    private String displayName(SkillId skill) {
        return DISPLAY_NAMES.get(skill);
    }

    private static final class SkillsHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private SkillsHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
