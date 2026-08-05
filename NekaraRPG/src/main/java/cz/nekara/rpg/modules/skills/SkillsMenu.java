package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.AttributePresentation;
import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.SkillPresentation;
import cz.nekara.rpg.skills.SkillProgressBar;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkConnectionPath;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.PerkEffectPresentation;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.perks.PerkIconResolver;
import cz.nekara.rpg.skills.perks.PerkPresentation;
import cz.nekara.rpg.skills.perks.PerkPosition;
import cz.nekara.rpg.skills.perks.PerkPurchaseDecision;
import cz.nekara.rpg.skills.perks.PerkPurchasePolicy;
import cz.nekara.rpg.skills.perks.PerkPurchaseStatus;
import cz.nekara.rpg.skills.perks.PerkRequirement;
import cz.nekara.rpg.skills.perks.PerkTreeViewport;
import cz.nekara.rpg.skills.perks.PerkTreeLayout;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
    private static final int OVERVIEW_INFO_SLOT = 46;
    private static final int PLAYER_OVERVIEW_SKILLS_SLOT = 49;
    private static final int SKILL_SCROLL_PREVIOUS_SLOT = 47;
    private static final int PREVIOUS_SKILL_SLOT = 48;
    private static final int CURRENT_SKILL_SLOT = 49;
    private static final int NEXT_SKILL_SLOT = 50;
    private static final int SKILL_SCROLL_NEXT_SLOT = 51;
    private static final int TREE_NORTH_WEST_SLOT = 0;
    private static final int TREE_NORTH_SLOT = 4;
    private static final int TREE_NORTH_EAST_SLOT = 8;
    private static final int TREE_WEST_SLOT = 18;
    private static final int TREE_EAST_SLOT = 26;
    private static final int TREE_SOUTH_WEST_SLOT = 36;
    private static final int TREE_SOUTH_SLOT = 40;
    private static final int TREE_SOUTH_EAST_SLOT = 44;
    private static final int CANCEL_SLOT = 20;
    private static final int CONFIRM_SLOT = 24;
    private static final int TREE_FIRST_ROW = 0;
    private static final int TREE_VIEWPORT_WIDTH = 9;
    private static final int TREE_VIEWPORT_HEIGHT = 5;
    private static final List<Integer> SKILL_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        31
    );
    private static final Map<Integer, TreeMove> TREE_MOVES = Map.of(
        TREE_NORTH_WEST_SLOT, new TreeMove(-1, -1),
        TREE_NORTH_SLOT, new TreeMove(0, -1),
        TREE_NORTH_EAST_SLOT, new TreeMove(1, -1),
        TREE_WEST_SLOT, new TreeMove(-1, 0),
        TREE_EAST_SLOT, new TreeMove(1, 0),
        TREE_SOUTH_WEST_SLOT, new TreeMove(-1, 1),
        TREE_SOUTH_SLOT, new TreeMove(0, 1),
        TREE_SOUTH_EAST_SLOT, new TreeMove(1, 1)
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
        inventory.setItem(OVERVIEW_INFO_SLOT, GuiItems.item(
            Material.WRITABLE_BOOK,
            Component.text("Jak stezka funguje", NamedTextColor.GOLD),
            Component.text("Kliknutím na dovednost otevřeš její stezku", NamedTextColor.GRAY),
            Component.text("Zelená: naučeno | Zlatá: lze naučit", NamedTextColor.GREEN),
            Component.text("Šedá: chybí podmínka", NamedTextColor.GRAY),
            Component.text("Bílá cesta: nenaučená vazba", NamedTextColor.WHITE),
            Component.text("Zelená cesta: propojené naučené perky", NamedTextColor.GREEN),
            Component.text("Kompas posouvá pohled v osmi směrech", NamedTextColor.YELLOW),
            Component.text("V action baru uvidíš připsané XP a postup", NamedTextColor.GRAY),
            Component.text("Např. +2 XP | ▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱ 2 %", NamedTextColor.DARK_GRAY),
            Component.text("Dílčí úrovně zvyšují hlavní úroveň", NamedTextColor.GRAY),
            Component.text("Hlavní úroveň dává body do perků", NamedTextColor.GOLD)
        ));
        player.openInventory(inventory);
    }

    void openPlayerOverview(Player player, SkillProfile profile, SkillProgressSnapshot snapshot) {
        SkillsHolder holder = new SkillsHolder(player.getUniqueId(), Screen.PLAYER_OVERVIEW, null, null);
        Inventory inventory = create(holder, 54, "Nekara — můj přehled");
        int availablePoints = availablePoints(profile, snapshot);
        Optional<SkillId> weaponSkill = SkillEquipmentPolicy.meleeSkill(player.getInventory().getItemInMainHand());
        Optional<SkillId> armorSkill = SkillEquipmentPolicy.armorSkill(player.getInventory());

        inventory.setItem(4, GuiItems.item(
            Material.PLAYER_HEAD,
            Component.text(player.getName(), NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true),
            Component.text("Hlavní úroveň: " + snapshot.power().level(), NamedTextColor.LIGHT_PURPLE),
            Component.text("Volné body: " + availablePoints, NamedTextColor.GOLD),
            Component.text("Hodnoty se obnovují při každém otevření", NamedTextColor.DARK_GRAY)
        ));
        inventory.setItem(10, GuiItems.item(
            Material.GOLDEN_APPLE,
            Component.text("Životy", NamedTextColor.RED),
            Component.text("Aktuálně: " + AttributePresentation.decimal(player.getHealth() / 2.0)
                + "/" + AttributePresentation.decimal(attribute(player, Attribute.MAX_HEALTH, 20.0) / 2.0)
                + " srdcí", NamedTextColor.WHITE),
            Component.text("Regenerace z těžké zbroje: " + AttributePresentation.signedPercentage(
                stat(player, armorSkill, StatId.HEALTH_REGENERATION)), NamedTextColor.GRAY)
        ));
        inventory.setItem(11, GuiItems.item(
            Material.IRON_SWORD,
            Component.text("Poškození", NamedTextColor.RED),
            Component.text("Základ zbraně: " + AttributePresentation.decimal(
                attribute(player, Attribute.ATTACK_DAMAGE, 1.0)), NamedTextColor.WHITE),
            Component.text("Aktivní cesta: " + activeSkillName(weaponSkill), NamedTextColor.GRAY),
            Component.text("Bonus poškození: " + AttributePresentation.bonusPercentage(
                stat(player, weaponSkill, StatId.DAMAGE_MULTIPLIER)), NamedTextColor.AQUA),
            Component.text("Kritická šance: " + AttributePresentation.percentage(
                stat(player, weaponSkill, StatId.CRITICAL_CHANCE)), NamedTextColor.AQUA),
            Component.text("Krvácení: " + AttributePresentation.percentage(
                stat(player, weaponSkill, StatId.BLEED_CHANCE)), NamedTextColor.AQUA)
        ));
        inventory.setItem(12, GuiItems.item(
            Material.NETHERITE_CHESTPLATE,
            Component.text("Zbroj", NamedTextColor.BLUE),
            Component.text("Brnění: " + AttributePresentation.decimal(attribute(player, Attribute.ARMOR, 0.0))
                + " | Odolnost: " + AttributePresentation.decimal(
                    attribute(player, Attribute.ARMOR_TOUGHNESS, 0.0)), NamedTextColor.WHITE),
            Component.text("Aktivní cesta: " + activeSkillName(armorSkill), NamedTextColor.GRAY),
            Component.text("Účinnost zbroje: " + AttributePresentation.bonusPercentage(
                stat(player, armorSkill, StatId.ARMOR_MULTIPLIER)), NamedTextColor.AQUA),
            Component.text("Úhyb: " + AttributePresentation.percentage(
                stat(player, armorSkill, StatId.DODGE_CHANCE)), NamedTextColor.AQUA)
        ));
        inventory.setItem(13, GuiItems.item(
            Material.FEATHER,
            Component.text("Pohyb a stabilita", NamedTextColor.GREEN),
            Component.text("Rychlost: " + AttributePresentation.percentage(speedRatio(player)), NamedTextColor.WHITE),
            Component.text("Odolnost proti odhození: " + AttributePresentation.percentage(
                attribute(player, Attribute.KNOCKBACK_RESISTANCE, 0.0)), NamedTextColor.WHITE),
            Component.text("Snížení penalizace zbroje: " + AttributePresentation.percentage(
                stat(player, armorSkill, StatId.MOVEMENT_PENALTY_REDUCTION)), NamedTextColor.AQUA)
        ));
        inventory.setItem(14, GuiItems.item(
            Material.SHIELD,
            Component.text("Obrana", NamedTextColor.BLUE),
            Component.text("Odraz poškození: " + AttributePresentation.percentage(
                stat(player, armorSkill, StatId.DAMAGE_REFLECTION)), NamedTextColor.AQUA),
            Component.text("Úspora hladu: " + AttributePresentation.percentage(
                stat(player, armorSkill, StatId.HUNGER_CONSUMPTION_REDUCTION)), NamedTextColor.AQUA),
            Component.text("Hodnoty platí při kompletní vhodné zbroji", NamedTextColor.DARK_GRAY)
        ));

        inventory.setItem(19, gatheringItem(player, SkillId.MINING, "Rychlost těžby", StatId.MINING_SPEED));
        inventory.setItem(20, gatheringItem(player, SkillId.WOODCUTTING, "Rychlost kácení", StatId.WOODCUTTING_SPEED));
        inventory.setItem(21, gatheringItem(player, SkillId.DIGGING, "Rychlost kopání", StatId.DIGGING_SPEED));
        inventory.setItem(22, farmingItem(player));
        inventory.setItem(23, fishingItem(player));
        inventory.setItem(24, archeryItem(player));

        inventory.setItem(28, craftsmanshipItem(player));
        inventory.setItem(29, productionItem(player, SkillId.ENCHANTING, "Runotepectví", Material.ENCHANTING_TABLE,
            "Síla očarování", StatId.ENCHANTMENT_POWER,
            "Úspora úrovní XP", StatId.EXPERIENCE_COST_REDUCTION));
        inventory.setItem(30, productionItem(player, SkillId.ALCHEMY, "Alchymie", Material.BREWING_STAND,
            "Rychlost vaření", StatId.BREWING_SPEED,
            "Síla lektvarů", StatId.POTION_POWER));
        inventory.setItem(31, productionItem(player, SkillId.TRADING, "Obchodování", Material.EMERALD,
            "Sleva u obchodníků", StatId.TRADE_DISCOUNT,
            "Zisk reputace", StatId.REPUTATION_GAIN));
        inventory.setItem(32, GuiItems.item(
            Material.NETHER_STAR,
            Component.text("Jak číst hodnoty", NamedTextColor.LIGHT_PURPLE),
            Component.text("Bíle: skutečná aktuální vanilla hodnota", NamedTextColor.WHITE),
            Component.text("Tyrkysově: právě aktivní bonus z perků", NamedTextColor.AQUA),
            Component.text("Podmíněné bonusy se ukážou až se správnou výbavou", NamedTextColor.GRAY)
        ));
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        inventory.setItem(PLAYER_OVERVIEW_SKILLS_SLOT, GuiItems.item(
            Material.EXPERIENCE_BOTTLE,
            Component.text("Dovednosti", NamedTextColor.LIGHT_PURPLE),
            Component.text("Otevřít úrovně, XP a perk stezky", NamedTextColor.GRAY)
        ));
        player.openInventory(inventory);
    }

    void openTree(Player player, SkillProfile profile, SkillProgressSnapshot snapshot, SkillId skill) {
        openTree(player, profile, snapshot, skill,
            PerkTreeViewport.initial(perkTree.catalog().forSkill(skill), TREE_VIEWPORT_WIDTH, TREE_VIEWPORT_HEIGHT));
    }

    void openTree(
        Player player,
        SkillProfile profile,
        SkillProgressSnapshot snapshot,
        SkillId skill,
        PerkTreeViewport viewport
    ) {
        List<PerkDefinition> perks = perkTree.catalog().forSkill(skill);
        PerkPosition newGamePlusPosition = PerkTreeLayout.forSkill(skill).newGamePlus();
        SkillsHolder holder = new SkillsHolder(player.getUniqueId(), Screen.TREE, skill, null, viewport);
        Inventory inventory = create(holder, 54, SkillPresentation.czechName(skill) + " — stezka");
        renderTreeBackground(inventory);
        Set<Integer> graphSlots = renderConnections(inventory, profile, perks, viewport);
        for (PerkDefinition perk : perks) {
            if (!viewport.contains(perk.position())) {
                continue;
            }
            int slot = treeSlot(viewport, perk.position());
            inventory.setItem(slot, perkItem(profile, snapshot, perk));
            holder.perksBySlot.put(slot, perk.id());
            holder.perkStatusesBySlot.put(
                slot, purchasePolicy.evaluate(profile, snapshot, perk).status());
            graphSlots.add(slot);
        }
        if (viewport.contains(newGamePlusPosition)) {
            int slot = treeSlot(viewport, newGamePlusPosition);
            inventory.setItem(slot, newGamePlusItem(profile, snapshot, skill));
            holder.newGamePlusSlot = slot;
            graphSlots.add(slot);
        }
        holder.graphSlots.addAll(graphSlots);
        // Navigation is an overlay: it intentionally takes visual and click priority over the graph.
        renderTreeNavigation(inventory, viewport, perks);
        inventory.setItem(BACK_SLOT, treeBackItem());
        renderSkillSlider(inventory, snapshot, skill);
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
        inventory.setItem(CONFIRM_SLOT, GuiItems.modeledItem(
            "skills/tree/button_confirm_selection", Material.LIME_STAINED_GLASS_PANE,
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

    void openNewGamePlusConfirmation(Player player, SkillProfile profile, SkillProgressSnapshot snapshot, SkillId skill) {
        SkillsHolder holder = new SkillsHolder(player.getUniqueId(), Screen.NEW_GAME_PLUS_CONFIRMATION, skill, null);
        Inventory inventory = create(holder, 45, "Potvrdit Novou hru+");
        inventory.setItem(13, newGamePlusItem(profile, snapshot, skill));
        inventory.setItem(CANCEL_SLOT, GuiItems.item(Material.RED_STAINED_GLASS_PANE,
            Component.text("Ještě ne", NamedTextColor.RED), Component.text("Vrátit se na stezku", NamedTextColor.GRAY)));
        inventory.setItem(CONFIRM_SLOT, GuiItems.modeledItem("skills/tree/button_confirm_selection", Material.LIME_STAINED_GLASS_PANE,
            Component.text("Znovuzrodit dovednost", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true),
            Component.text("XP a perky této dovednosti se resetují", NamedTextColor.YELLOW),
            Component.text("Body z jejích perků se vrátí", NamedTextColor.GOLD)));
        player.openInventory(inventory);
    }

    void showNewGamePlusStatus(Player player, SkillId skill, cz.nekara.rpg.skills.newgameplus.NewGamePlusResult result) {
        Component message = switch (result.status()) {
            case REBORN -> Component.text("Nová hra+ pro " + SkillPresentation.czechName(skill)
                + ": vráceno " + result.refundedPoints() + " bodů.", NamedTextColor.GREEN);
            case NOT_MAX_LEVEL -> Component.text("Nová hra+ vyžaduje úroveň 100.", NamedTextColor.RED);
            case MAXIMUM_RANK_REACHED -> Component.text("Tato dovednost už dosáhla maxima Nové hry+.", NamedTextColor.RED);
            case DISABLED -> Component.text("Nová hra+ je nyní vypnutá správcem.", NamedTextColor.RED);
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
            case PLAYER_OVERVIEW -> clickPlayerOverview(player, slot);
            case TREE -> clickTree(player, holder, slot);
            case CONFIRMATION -> clickConfirmation(player, holder, slot);
            case NEW_GAME_PLUS_CONFIRMATION -> clickNewGamePlusConfirmation(player, holder, slot);
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
        } else if (slot == holder.newGamePlusSlot) {
            module.openNewGamePlusConfirmation(player, holder.skill);
        } else if (holder.perksBySlot.containsKey(slot)) {
            PerkPurchaseStatus status = holder.perkStatusesBySlot.get(slot);
            if (status == PerkPurchaseStatus.PURCHASED) {
                module.openPerkConfirmation(player, holder.perksBySlot.get(slot));
            } else {
                showPurchaseStatus(player, status);
            }
        } else if (TREE_MOVES.containsKey(slot)) {
            TreeMove move = TREE_MOVES.get(slot);
            moveTree(player, holder, move.horizontal(), move.vertical());
        } else if (slot == SKILL_SCROLL_PREVIOUS_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, -3));
        } else if (slot == PREVIOUS_SKILL_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, -1));
        } else if (slot == NEXT_SKILL_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, 1));
        } else if (slot == SKILL_SCROLL_NEXT_SLOT) {
            module.openSkillTree(player, adjacent(holder.skill, 3));
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
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.AQUA),
            Component.text("Úroveň: " + progress.level() + "/100", NamedTextColor.GRAY),
            totalExperienceLine(progress),
            progressBarLine(progress),
            Component.text(SkillProgressBar.remainingText(progress), NamedTextColor.GRAY),
            Component.text("Klikni pro otevření stezky", NamedTextColor.YELLOW)
        );
    }

    private void clickNewGamePlusConfirmation(Player player, SkillsHolder holder, int slot) {
        if (slot == CANCEL_SLOT) {
            module.openSkillTree(player, holder.skill);
        } else if (slot == CONFIRM_SLOT) {
            module.requestNewGamePlus(player, holder.skill);
        }
    }

    private ItemStack newGamePlusItem(SkillProfile profile, SkillProgressSnapshot snapshot, SkillId skill) {
        int rank = profile.newGamePlusRank(skill);
        boolean ready = module.canUseNewGamePlus(profile, skill);
        double xp = module.newGamePlusExperienceMultiplier(profile, skill);
        return GuiItems.item(ready ? Material.NETHER_STAR : Material.GRAY_DYE,
            Component.text("Nová hra+ " + rank, ready ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY),
            Component.text("Trvalý bonus perk statistik: +" + Math.round(module.newGamePlusStatBonus(profile, skill) * 100) + " %", NamedTextColor.AQUA),
            Component.text("Další běh: " + Math.round(xp * 100) + " % XP", NamedTextColor.GOLD),
            Component.text(ready ? "Klikni pro bezpečné potvrzení resetu" : "Vyžaduje úroveň 100 a dosud nepoužitou Novou hru+", ready ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
    }

    private void moveTree(Player player, SkillsHolder holder, int horizontal, int vertical) {
        List<PerkDefinition> perks = perkTree.catalog().forSkill(holder.skill);
        PerkTreeViewport moved = holder.viewport.move(horizontal, vertical, perks);
        if (!moved.equals(holder.viewport)) {
            module.openSkillTree(player, holder.skill, moved);
        }
    }

    private void clickPlayerOverview(Player player, int slot) {
        if (slot == BACK_SLOT) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.openMainMenu(player));
        } else if (slot == PLAYER_OVERVIEW_SKILLS_SLOT) {
            module.openMenu(player);
        }
    }

    private void renderTreeBackground(Inventory inventory) {
        ItemStack background = GuiItems.item(
            Material.GRAY_STAINED_GLASS_PANE,
            Component.text("Pozadí stezky", NamedTextColor.DARK_GRAY)
        );
        for (int row = TREE_FIRST_ROW; row < TREE_FIRST_ROW + TREE_VIEWPORT_HEIGHT; row++) {
            for (int column = 0; column < TREE_VIEWPORT_WIDTH; column++) {
                inventory.setItem(row * TREE_VIEWPORT_WIDTH + column, background);
            }
        }
    }

    private void renderTreeNavigation(
        Inventory inventory,
        PerkTreeViewport viewport,
        List<PerkDefinition> perks
    ) {
        inventory.setItem(TREE_NORTH_WEST_SLOT, treeArrow("nw", "Posunout strom vlevo nahoru",
            viewport.canMove(-1, -1, perks)));
        inventory.setItem(TREE_NORTH_SLOT, treeArrow("n", "Posunout strom nahoru",
            viewport.canMove(0, -1, perks)));
        inventory.setItem(TREE_NORTH_EAST_SLOT, treeArrow("ne", "Posunout strom vpravo nahoru",
            viewport.canMove(1, -1, perks)));
        inventory.setItem(TREE_WEST_SLOT, treeArrow("w", "Posunout strom doleva",
            viewport.canMove(-1, 0, perks)));
        inventory.setItem(TREE_EAST_SLOT, treeArrow("e", "Posunout strom doprava",
            viewport.canMove(1, 0, perks)));
        inventory.setItem(TREE_SOUTH_WEST_SLOT, treeArrow("sw", "Posunout strom vlevo dolů",
            viewport.canMove(-1, 1, perks)));
        inventory.setItem(TREE_SOUTH_SLOT, treeArrow("s", "Posunout strom dolů",
            viewport.canMove(0, 1, perks)));
        inventory.setItem(TREE_SOUTH_EAST_SLOT, treeArrow("se", "Posunout strom vpravo dolů",
            viewport.canMove(1, 1, perks)));
    }

    private ItemStack treeBackItem() {
        return GuiItems.modeledItem("skills/tree/button_return_to_menu", Material.SPECTRAL_ARROW,
            Component.text("Zpět na dovednosti", NamedTextColor.GOLD),
            Component.text("Otevřít přehled všech dovedností", NamedTextColor.GRAY)
        );
    }

    private ItemStack gatheringItem(Player player, SkillId skill, String speedLabel, StatId speed) {
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.GREEN),
            Component.text(speedLabel + ": " + AttributePresentation.bonusPercentage(stat(player, skill, speed)),
                NamedTextColor.AQUA),
            Component.text("Dvojitý výtěžek: " + AttributePresentation.percentage(
                stat(player, skill, StatId.DOUBLE_DROP_CHANCE)), NamedTextColor.AQUA),
            Component.text("Trojitý výtěžek: " + AttributePresentation.percentage(
                stat(player, skill, StatId.TRIPLE_DROP_CHANCE)), NamedTextColor.AQUA)
        );
    }

    private ItemStack farmingItem(Player player) {
        SkillId skill = SkillId.FARMING;
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.GREEN),
            Component.text("Dvojitá sklizeň: " + AttributePresentation.percentage(
                stat(player, skill, StatId.DOUBLE_DROP_CHANCE)), NamedTextColor.AQUA),
            Component.text("Růst plodin: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.CROP_GROWTH_MULTIPLIER)), NamedTextColor.AQUA),
            Component.text("Výtěžek včel: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.BEEKEEPING_YIELD)), NamedTextColor.AQUA)
        );
    }

    private ItemStack fishingItem(Player player) {
        SkillId skill = SkillId.FISHING;
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.BLUE),
            Component.text("Rychlost záběru: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.FISHING_SPEED)), NamedTextColor.AQUA),
            Component.text("Štěstí při lovu: " + AttributePresentation.percentage(
                stat(player, skill, StatId.FISHING_LUCK)), NamedTextColor.AQUA),
            Component.text("Více vanilla XP: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.EXPERIENCE_ORB_MULTIPLIER)), NamedTextColor.AQUA)
        );
    }

    private ItemStack archeryItem(Player player) {
        SkillId skill = SkillId.ARCHERY;
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.YELLOW),
            Component.text("Přesnost: " + AttributePresentation.percentage(stat(player, skill, StatId.ACCURACY)),
                NamedTextColor.AQUA),
            Component.text("Úspora šípů: " + AttributePresentation.percentage(
                stat(player, skill, StatId.AMMO_CONSUMPTION_REDUCTION)), NamedTextColor.AQUA),
            Component.text("Síla útoku: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.DAMAGE_MULTIPLIER)), NamedTextColor.AQUA)
        );
    }

    private ItemStack productionItem(
        Player player,
        SkillId skill,
        String label,
        Material icon,
        String firstLabel,
        StatId first,
        String secondLabel,
        StatId second
    ) {
        return GuiItems.item(
            icon,
            Component.text(label, NamedTextColor.GOLD),
            productionStatLine(player, firstLabel, first, skill),
            productionStatLine(player, secondLabel, second, skill)
        );
    }

    private ItemStack craftsmanshipItem(Player player) {
        SkillId skill = SkillId.SMITHING;
        return GuiItems.item(
            Material.ANVIL,
            Component.text(SkillPresentation.czechName(skill), NamedTextColor.GOLD),
            Component.text("Lepší Tier při výrobě: " + AttributePresentation.percentage(
                stat(player, skill, StatId.ITEM_QUALITY) - 1.0), NamedTextColor.AQUA),
            Component.text("Úspora surovin: " + AttributePresentation.percentage(
                stat(player, skill, StatId.RESOURCE_COST_REDUCTION)), NamedTextColor.AQUA),
            Component.text("Rychlost pece: " + AttributePresentation.bonusPercentage(
                stat(player, skill, StatId.FURNACE_SPEED)), NamedTextColor.AQUA)
        );
    }

    private Component productionStatLine(Player player, String label, StatId stat, SkillId skill) {
        double value = stat(player, skill, stat);
        boolean multiplier = stat.defaultValue() == 1.0;
        return Component.text(label + ": " + (multiplier
            ? AttributePresentation.bonusPercentage(value)
            : AttributePresentation.percentage(value)), NamedTextColor.AQUA);
    }

    private String activeSkillName(Optional<SkillId> skill) {
        return skill.map(SkillPresentation::czechName).orElse("žádná vhodná výbava");
    }

    private double stat(Player player, SkillId skill, StatId stat) {
        return stat(player, Optional.of(skill), stat);
    }

    private double stat(Player player, Optional<SkillId> skill, StatId stat) {
        return skill.flatMap(active -> module.runtimeState(player.getUniqueId(), active))
            .map(state -> state.stats().value(stat))
            .orElse(stat.defaultValue());
    }

    private double attribute(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    private double speedRatio(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null || speed.getBaseValue() <= 0.0) {
            return 1.0;
        }
        return speed.getValue() / speed.getBaseValue();
    }

    private void renderSkillSlider(Inventory inventory, SkillProgressSnapshot snapshot, SkillId current) {
        SkillId previous = adjacent(current, -1);
        SkillId next = adjacent(current, 1);
        inventory.setItem(SKILL_SCROLL_PREVIOUS_SLOT, skillSliderArrow("button_prevpage", "Posunout nabídku o tři dovednosti vlevo"));
        inventory.setItem(PREVIOUS_SKILL_SLOT, skillSliderItem(previous, snapshot.skill(previous), false));
        inventory.setItem(CURRENT_SKILL_SLOT, skillSliderItem(current, snapshot.skill(current), true));
        inventory.setItem(NEXT_SKILL_SLOT, skillSliderItem(next, snapshot.skill(next), false));
        inventory.setItem(SKILL_SCROLL_NEXT_SLOT, skillSliderArrow("button_nextpage", "Posunout nabídku o tři dovednosti vpravo"));
    }

    private ItemStack skillSliderArrow(String model, String label) {
        return GuiItems.modeledItem(
            "skills/tree/" + model, Material.SPECTRAL_ARROW,
            Component.text(label, NamedTextColor.GOLD),
            Component.text("Přesunout výběr dovedností", NamedTextColor.GRAY)
        );
    }

    private ItemStack skillSliderItem(SkillId skill, SkillLevelProgress progress, boolean current) {
        NamedTextColor color = current ? NamedTextColor.GOLD : NamedTextColor.AQUA;
        return GuiItems.item(
            material(skill),
            Component.text(SkillPresentation.czechName(skill), color).decoration(TextDecoration.BOLD, current),
            Component.text(current ? "Aktuálně zobrazená stezka" : "Klikni pro otevření stezky", NamedTextColor.GRAY),
            Component.text("Úroveň: " + progress.level() + "/100", NamedTextColor.DARK_GRAY)
        );
    }

    private Set<Integer> renderConnections(
        Inventory inventory,
        SkillProfile profile,
        List<PerkDefinition> perks,
        PerkTreeViewport viewport
    ) {
        Set<PerkPosition> perkPositions = new HashSet<>();
        perks.forEach(perk -> perkPositions.add(perk.position()));
        Map<PerkPosition, Integer> connected = new HashMap<>();
        for (PerkDefinition perk : perks) {
            for (PerkRequirement requirement : perk.requirements()) {
                PerkDefinition prerequisite = perkTree.catalog().require(requirement.perkId());
                boolean prerequisiteUnlocked = profile.perkRank(prerequisite.id()) >= requirement.minimumRank();
                int state = prerequisiteUnlocked ? (profile.perkRank(perk.id()) > 0 ? 2 : 1) : 0;
                PerkConnectionPath.BendOrder bendOrder = perk.requirements().size() > 1
                    ? PerkConnectionPath.BendOrder.VERTICAL_FIRST
                    : PerkConnectionPath.BendOrder.HORIZONTAL_FIRST;
                for (PerkPosition position : PerkConnectionPath.between(
                    prerequisite.position(), perk.position(), bendOrder)) {
                    if (!perkPositions.contains(position)) {
                        connected.merge(position, state, Math::max);
                    }
                }
            }
        }
        Set<PerkPosition> connectionNeighbors = new HashSet<>(connected.keySet());
        connectionNeighbors.addAll(perkPositions);
        Set<Integer> slots = new HashSet<>();
        connected.forEach((position, state) -> {
            if (!viewport.contains(position)) {
                return;
            }
            int slot = treeSlot(viewport, position);
            inventory.setItem(slot, GuiItems.modeledItem(
                "skills/tree/" + connectionStateName(state) + "/" + connectionShape(position, connectionNeighbors),
                state == 2 ? Material.LIME_STAINED_GLASS_PANE : state == 1 ? Material.WHITE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                Component.text(state == 2 ? "Odemčená vazba" : state == 1 ? "Přístupná vazba" : "Zamčená vazba",
                    state == 2 ? NamedTextColor.GREEN : state == 1 ? NamedTextColor.WHITE : NamedTextColor.GRAY)
            ));
            slots.add(slot);
        });
        return slots;
    }

    private static String connectionStateName(int state) {
        return switch (state) { case 2 -> "unlocked"; case 1 -> "unlockable"; default -> "locked"; };
    }

    private int treeSlot(PerkTreeViewport viewport, PerkPosition position) {
        return (TREE_FIRST_ROW * TREE_VIEWPORT_WIDTH) + viewport.slot(position);
    }

    private ItemStack treeArrow(String direction, String label, boolean available) {
        return GuiItems.modeledItem("skills/tree/button_" + direction,
            available ? Material.SPECTRAL_ARROW : Material.ARROW,
            Component.text(label, available ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY),
            Component.text(available ? "Posunout pohled po stezce" : "Na tomto okraji není další část", NamedTextColor.GRAY)
        );
    }

    private String connectionShape(PerkPosition position, Set<PerkPosition> positions) {
        boolean left = position.column() > 0 && positions.contains(new PerkPosition(position.column() - 1, position.row()));
        boolean right = positions.contains(new PerkPosition(position.column() + 1, position.row()));
        boolean up = position.row() > 0 && positions.contains(new PerkPosition(position.column(), position.row() - 1));
        boolean down = positions.contains(new PerkPosition(position.column(), position.row() + 1));
        if (left || right) {
            if (up || down) {
                if (right && down) return "continuous_corner_toplft";
                if (left && down) return "continuous_corner_toprgt";
                if (right && up) return "continuous_corner_botlft";
                return "continuous_corner_botrgt";
            }
            return "continuous_horizontal";
        }
        return "continuous_vertical";
    }

    private Component totalExperienceLine(SkillLevelProgress progress) {
        if (progress.capped()) {
            return Component.text("Celkem zkušeností: " + progress.totalExperience() + " XP (maximum)",
                NamedTextColor.GOLD);
        }
        return Component.text("Celkem zkušeností: " + progress.totalExperience() + "/"
            + progress.totalExperienceForNextLevel() + " XP", NamedTextColor.DARK_AQUA);
    }

    private Component progressBarLine(SkillLevelProgress progress) {
        return Component.text("Postup: ", NamedTextColor.DARK_AQUA)
            .append(SkillProgressBar.component(progress))
            .append(Component.text(" " + SkillProgressBar.amountText(progress), NamedTextColor.WHITE));
    }

    private ItemStack perkItem(
        SkillProfile profile,
        SkillProgressSnapshot snapshot,
        PerkDefinition perk
    ) {
        int rank = profile.perkRank(perk.id());
        PerkPurchaseDecision decision = purchasePolicy.evaluate(profile, snapshot, perk);
        boolean maxed = rank >= perk.maxRank();
        Material material = PerkIconResolver.resolve(perk);
        NamedTextColor color = maxed ? NamedTextColor.GREEN
            : decision.allowed() ? NamedTextColor.GOLD : NamedTextColor.GRAY;
        int skillLevel = snapshot.skill(perk.skill()).level();
        int freePoints = availablePoints(profile, snapshot);
        PerkPresentation presentation = perkTree.presentation(perk.id());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(presentation.description(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Přesný účinek", NamedTextColor.LIGHT_PURPLE));
        for (String effect : PerkEffectPresentation.describe(perk, rank)) {
            lore.add(Component.text(effect, NamedTextColor.AQUA));
        }
        lore.add(Component.empty());
        lore.add(Component.text(perkStateText(maxed, decision.status()), perkStateColor(maxed, decision.status())));
        lore.add(Component.text(skillLevel >= perk.requiredSkillLevel()
            ? "Úroveň dovednosti splněna" : "Chybí úroveň dovednosti", skillLevel >= perk.requiredSkillLevel()
                ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Component.text(maxed || freePoints >= perk.pointCostPerRank()
            ? "Dostatek volných bodů" : "Chybí volné body", maxed || freePoints >= perk.pointCostPerRank()
                ? NamedTextColor.GOLD : NamedTextColor.RED));
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

    private String perkStateText(boolean maxed, PerkPurchaseStatus status) {
        if (maxed) {
            return "Stav: Odemčeno";
        }
        return switch (status) {
            case PURCHASED -> "Stav: Připraveno k odemčení";
            case MAX_RANK -> "Stav: Odemčeno";
            case LEVEL_REQUIRED -> "Stav: Zamčeno — chybí úroveň";
            case PREREQUISITE_REQUIRED -> "Stav: Zamčeno — chybí předchozí perk";
            case INSUFFICIENT_POINTS -> "Stav: Zamčeno — chybí body";
        };
    }

    private NamedTextColor perkStateColor(boolean maxed, PerkPurchaseStatus status) {
        if (maxed || status == PerkPurchaseStatus.MAX_RANK) {
            return NamedTextColor.GREEN;
        }
        return switch (status) {
            case PURCHASED -> NamedTextColor.GOLD;
            case LEVEL_REQUIRED, PREREQUISITE_REQUIRED -> NamedTextColor.RED;
            case INSUFFICIENT_POINTS -> NamedTextColor.YELLOW;
            case MAX_RANK -> NamedTextColor.GREEN;
        };
    }

    private int availablePoints(SkillProfile profile, SkillProgressSnapshot snapshot) {
        return Math.max(0, snapshot.power().level() + profile.adminBonusPerkPoints()
            - profile.spentPerkPoints());
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
        PLAYER_OVERVIEW,
        TREE,
        CONFIRMATION,
        NEW_GAME_PLUS_CONFIRMATION
    }

    private record TreeMove(int horizontal, int vertical) {
    }

    private static final class SkillsHolder implements InventoryHolder {
        private final UUID ownerId;
        private final Screen screen;
        private final SkillId skill;
        private final PerkId perkId;
        private final PerkTreeViewport viewport;
        private final Map<Integer, SkillId> skillsBySlot = new HashMap<>();
        private final Map<Integer, PerkId> perksBySlot = new HashMap<>();
        private final Map<Integer, PerkPurchaseStatus> perkStatusesBySlot = new HashMap<>();
        private final Set<Integer> graphSlots = new HashSet<>();
        private int newGamePlusSlot = -1;
        private Inventory inventory;

        private SkillsHolder(UUID ownerId, Screen screen, SkillId skill, PerkId perkId) {
            this(ownerId, screen, skill, perkId, null);
        }

        private SkillsHolder(UUID ownerId, Screen screen, SkillId skill, PerkId perkId, PerkTreeViewport viewport) {
            this.ownerId = ownerId;
            this.screen = screen;
            this.skill = skill;
            this.perkId = perkId;
            this.viewport = viewport;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
