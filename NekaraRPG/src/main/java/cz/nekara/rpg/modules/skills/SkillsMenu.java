package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.SkillPresentation;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.perks.PerkPresentation;
import cz.nekara.rpg.skills.perks.PerkPurchaseDecision;
import cz.nekara.rpg.skills.perks.PerkPurchasePolicy;
import cz.nekara.rpg.skills.perks.PerkPurchaseStatus;
import cz.nekara.rpg.skills.perks.PerkRequirement;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final int PREVIOUS_SLOT = 47;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 51;
    private static final int CANCEL_SLOT = 20;
    private static final int CONFIRM_SLOT = 24;
    private static final List<Integer> SKILL_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        31
    );

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final DefaultPerkTree perkTree;
    private final PerkPurchasePolicy purchasePolicy = new PerkPurchasePolicy();
    private boolean enabled;

    SkillsMenu(NekaraRPGPlugin plugin, SkillsModule module, DefaultPerkTree perkTree) {
        this.plugin = plugin;
        this.module = module;
        this.perkTree = perkTree;
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

    void openOverview(Player player, SkillProfile profile, SkillProgressSnapshot snapshot) {
        SkillsHolder holder = new SkillsHolder(player.getUniqueId(), Screen.OVERVIEW, null, null);
        Inventory inventory = create(holder, 54, "Nekara — dovednosti");
        int availablePoints = availablePoints(profile, snapshot);
        inventory.setItem(POWER_SLOT, GuiItems.item(
            Material.NETHER_STAR,
            Component.text("Hlavní úroveň " + snapshot.power().level(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true),
            Component.text("Síla všech tvých naučených cest", NamedTextColor.GRAY),
            Component.text("Do další úrovně: " + snapshot.power().levelsUntilNext()
                + " dílčích úrovní", NamedTextColor.DARK_GRAY),
            Component.text("Volné body: " + availablePoints, NamedTextColor.GOLD)
        ));
        for (int index = 0; index < SkillId.gameplaySkills().size(); index++) {
            SkillId skill = SkillId.gameplaySkills().get(index);
            inventory.setItem(SKILL_SLOTS.get(index), skillItem(skill, snapshot.skill(skill)));
            holder.skillsBySlot.put(SKILL_SLOTS.get(index), skill);
        }
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        inventory.setItem(INFO_SLOT, GuiItems.item(
            Material.WRITABLE_BOOK,
            Component.text("Stezky rozvoje", NamedTextColor.GOLD),
            Component.text("Každá dovednost dosáhne nejvýše úrovně 100", NamedTextColor.GRAY),
            Component.text("Dílčí úrovně zvyšují hlavní úroveň", NamedTextColor.GRAY),
            Component.text("Hlavní úroveň dává body do perků", NamedTextColor.GOLD)
        ));
        player.openInventory(inventory);
    }

    void openTree(Player player, SkillProfile profile, SkillProgressSnapshot snapshot, SkillId skill) {
        SkillsHolder holder = new SkillsHolder(player.getUniqueId(), Screen.TREE, skill, null);
        Inventory inventory = create(holder, 54, SkillPresentation.czechName(skill) + " — stezka");
        SkillLevelProgress progress = snapshot.skill(skill);
        inventory.setItem(4, GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true),
            Component.text("Úroveň " + progress.level() + "/100", NamedTextColor.GRAY),
            Component.text("Volné body: " + availablePoints(profile, snapshot), NamedTextColor.GOLD)
        ));
        for (PerkDefinition perk : perkTree.catalog().forSkill(skill)) {
            int slot = perk.position().row() * 9 + perk.position().column();
            inventory.setItem(slot, perkItem(profile, snapshot, perk));
            holder.perksBySlot.put(slot, perk.id());
            holder.perkStatusesBySlot.put(
                slot, purchasePolicy.evaluate(profile, snapshot, perk).status());
        }
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět na dovednosti"));
        inventory.setItem(PREVIOUS_SLOT, GuiItems.item(
            Material.SPECTRAL_ARROW,
            Component.text("Předchozí stezka", NamedTextColor.YELLOW),
            Component.text(SkillPresentation.czechName(adjacent(skill, -1)), NamedTextColor.GRAY)
        ));
        inventory.setItem(INFO_SLOT, GuiItems.item(
            Material.BOOK,
            Component.text("Jak stezka funguje", NamedTextColor.GOLD),
            Component.text("Zelená: naučeno", NamedTextColor.GREEN),
            Component.text("Zlatá: lze naučit", NamedTextColor.GOLD),
            Component.text("Šedá: chybí podmínka", NamedTextColor.GRAY),
            Component.text("Kliknutí otevře potvrzení", NamedTextColor.DARK_GRAY)
        ));
        inventory.setItem(NEXT_SLOT, GuiItems.item(
            Material.SPECTRAL_ARROW,
            Component.text("Další stezka", NamedTextColor.YELLOW),
            Component.text(SkillPresentation.czechName(adjacent(skill, 1)), NamedTextColor.GRAY)
        ));
        player.openInventory(inventory);
    }

    void openConfirmation(
        Player player,
        SkillProfile profile,
        SkillProgressSnapshot snapshot,
        PerkDefinition perk
    ) {
        SkillsHolder holder = new SkillsHolder(
            player.getUniqueId(), Screen.CONFIRMATION, perk.skill(), perk.id());
        Inventory inventory = create(holder, 45, "Potvrdit perk");
        inventory.setItem(13, perkItem(profile, snapshot, perk));
        inventory.setItem(CANCEL_SLOT, GuiItems.item(
            Material.RED_STAINED_GLASS_PANE,
            Component.text("Ještě ne", NamedTextColor.RED),
            Component.text("Vrátit se na stezku", NamedTextColor.GRAY)
        ));
        inventory.setItem(CONFIRM_SLOT, GuiItems.item(
            Material.LIME_STAINED_GLASS_PANE,
            Component.text("Naučit se", NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true),
            Component.text("Cena: " + perk.pointCostPerRank() + " bodů", NamedTextColor.GOLD),
            Component.text("Volba se uloží natrvalo", NamedTextColor.DARK_GRAY)
        ));
        player.openInventory(inventory);
    }

    void showUnavailable(Player player) {
        player.sendActionBar(Component.text(
            "Kronika dovedností je nyní uzavřená.", NamedTextColor.RED));
    }

    void showPurchaseStatus(Player player, PerkPurchaseStatus status) {
        Component message = switch (status) {
            case PURCHASED -> Component.text("Nový perk byl zapsán do kroniky.", NamedTextColor.GREEN);
            case MAX_RANK -> Component.text("Tento perk už znáš naplno.", NamedTextColor.YELLOW);
            case LEVEL_REQUIRED -> Component.text("Nejdřív musíš pokročit v této dovednosti.", NamedTextColor.RED);
            case PREREQUISITE_REQUIRED -> Component.text("Nejdřív se nauč předchozí perk.", NamedTextColor.RED);
            case INSUFFICIENT_POINTS -> Component.text("Nemáš dost volných bodů.", NamedTextColor.RED);
        };
        player.sendActionBar(message);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !(event.getView().getTopInventory().getHolder() instanceof SkillsHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!holder.ownerId.equals(player.getUniqueId()) || event.getRawSlot() < 0
            || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        switch (holder.screen) {
            case OVERVIEW -> clickOverview(player, holder, slot);
            case TREE -> clickTree(player, holder, slot);
            case CONFIRMATION -> clickConfirmation(player, holder, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SkillsHolder) {
            event.setCancelled(true);
        }
    }

    private void clickOverview(Player player, SkillsHolder holder, int slot) {
        if (slot == BACK_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.openMainMenu(player));
            return;
        }
        SkillId skill = holder.skillsBySlot.get(slot);
        if (skill != null) {
            module.openSkillTree(player, skill);
        }
    }

    private void clickTree(Player player, SkillsHolder holder, int slot) {
        if (slot == BACK_SLOT) {
            module.openMenu(player);
        } else if (slot == PREVIOUS_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, -1));
        } else if (slot == NEXT_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, 1));
        } else {
            PerkId perkId = holder.perksBySlot.get(slot);
            if (perkId != null) {
                PerkPurchaseStatus status = holder.perkStatusesBySlot.get(slot);
                if (status == PerkPurchaseStatus.PURCHASED) {
                    module.openPerkConfirmation(player, perkId);
                } else {
                    showPurchaseStatus(player, status);
                }
            }
        }
    }

    private void clickConfirmation(Player player, SkillsHolder holder, int slot) {
        if (slot == CANCEL_SLOT) {
            module.openSkillTree(player, holder.skill);
        } else if (slot == CONFIRM_SLOT) {
            module.purchasePerk(player, holder.perkId);
        }
    }

    private ItemStack skillItem(SkillId skill, SkillLevelProgress progress) {
        Component next = progress.capped()
            ? Component.text("Stezka je završena", NamedTextColor.GOLD)
            : Component.text("XP " + progress.experienceIntoLevel() + "/"
                + progress.experienceForNextLevel(), NamedTextColor.DARK_GRAY);
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.AQUA),
            Component.text("Úroveň " + progress.level() + "/100", NamedTextColor.GRAY),
            next,
            Component.text("Klikni pro otevření stezky", NamedTextColor.YELLOW)
        );
    }

    private ItemStack perkItem(
        SkillProfile profile,
        SkillProgressSnapshot snapshot,
        PerkDefinition perk
    ) {
        int rank = profile.perkRank(perk.id());
        PerkPurchaseDecision decision = purchasePolicy.evaluate(profile, snapshot, perk);
        boolean maxed = rank >= perk.maxRank();
        Material material = maxed ? Material.LIME_DYE
            : decision.allowed() ? Material.GLOWSTONE_DUST : Material.GRAY_DYE;
        NamedTextColor color = maxed ? NamedTextColor.GREEN
            : decision.allowed() ? NamedTextColor.GOLD : NamedTextColor.GRAY;
        PerkPresentation presentation = perkTree.presentation(perk.id());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(presentation.description(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Hodnost " + rank + "/" + perk.maxRank(), color));
        lore.add(Component.text("Cena další hodnosti: " + perk.pointCostPerRank(), NamedTextColor.GOLD));
        lore.add(Component.text("Potřebná úroveň: " + perk.requiredSkillLevel(), NamedTextColor.DARK_GRAY));
        for (PerkRequirement requirement : perk.requirements()) {
            lore.add(Component.text("Vyžaduje: " + perkTree.presentation(requirement.perkId()).name()
                + " " + requirement.minimumRank(), NamedTextColor.DARK_GRAY));
        }
        if (!maxed) {
            lore.add(Component.text(statusText(decision.status()), color));
        }
        return GuiItems.item(
            material,
            Component.text(presentation.name(), color).decoration(TextDecoration.BOLD, true),
            lore.toArray(Component[]::new)
        );
    }

    private String statusText(PerkPurchaseStatus status) {
        return switch (status) {
            case PURCHASED -> "Klikni pro naučení";
            case MAX_RANK -> "Naučeno";
            case LEVEL_REQUIRED -> "Chybí úroveň dovednosti";
            case PREREQUISITE_REQUIRED -> "Chybí předchozí perk";
            case INSUFFICIENT_POINTS -> "Chybí volné body";
        };
    }

    private int availablePoints(SkillProfile profile, SkillProgressSnapshot snapshot) {
        return Math.max(0, snapshot.power().level() - profile.spentPerkPoints());
    }

    private SkillId adjacent(SkillId skill, int direction) {
        List<SkillId> skills = SkillId.gameplaySkills();
        int index = skills.indexOf(skill);
        return skills.get(Math.floorMod(index + direction, skills.size()));
    }

    private Inventory create(SkillsHolder holder, int size, String title) {
        Inventory inventory = Bukkit.createInventory(
            holder, size, Component.text(title, NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        return inventory;
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

    private enum Screen {
        OVERVIEW,
        TREE,
        CONFIRMATION
    }

    private static final class SkillsHolder implements InventoryHolder {
        private final UUID ownerId;
        private final Screen screen;
        private final SkillId skill;
        private final PerkId perkId;
        private final Map<Integer, SkillId> skillsBySlot = new HashMap<>();
        private final Map<Integer, PerkId> perksBySlot = new HashMap<>();
        private final Map<Integer, PerkPurchaseStatus> perkStatusesBySlot = new HashMap<>();
        private Inventory inventory;

        private SkillsHolder(UUID ownerId, Screen screen, SkillId skill, PerkId perkId) {
            this.ownerId = ownerId;
            this.screen = screen;
            this.skill = skill;
            this.perkId = perkId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
