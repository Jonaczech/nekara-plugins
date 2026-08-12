package cz.nekara.rpg.modules.customitems;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.items.custom.CustomItemDefinition;
import cz.nekara.rpg.items.custom.CustomItemFactory;
import cz.nekara.rpg.items.custom.CustomItemRepository;
import cz.nekara.rpg.items.custom.CustomItemStat;
import cz.nekara.rpg.items.custom.CustomItemStats;
import cz.nekara.rpg.items.custom.YamlCustomItemRepository;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.modules.NekaraModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public final class CustomItemsModule implements NekaraModule, Listener {
    public static final String ID = "custom-items";

    private static final int HOME_CREATE_SLOT = 11;
    private static final int HOME_CATALOG_SLOT = 15;
    private static final int CATALOG_BACK_SLOT = 49;
    private static final int CATALOG_ITEM_LIMIT = 45;
    private static final int ID_SLOT = 10;
    private static final int NAME_SLOT = 12;
    private static final int MATERIAL_SLOT = 14;
    private static final int MODEL_SLOT = 16;
    private static final int LEGACY_MODEL_SLOT = 19;
    private static final int DAMAGE_SLOT = 28;
    private static final int SPEED_SLOT = 30;
    private static final int ARMOR_SLOT = 32;
    private static final int TOUGHNESS_SLOT = 34;
    private static final int HEALTH_SLOT = 36;
    private static final int PREVIEW_SLOT = 40;
    private static final int SAVE_SLOT = 48;
    private static final int CANCEL_SLOT = 50;

    private final NekaraRPGPlugin plugin;
    private final CustomItemFactory factory;
    private final Map<UUID, CustomItemDefinition> drafts = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final Set<UUID> editingExisting = new HashSet<>();
    private final Set<UUID> editorTransitions = new HashSet<>();
    private CustomItemRepository repository;
    private boolean enabled;

    public CustomItemsModule(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
        factory = new CustomItemFactory(plugin);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable() {
        if (enabled) {
            return;
        }
        try {
            repository = new YamlCustomItemRepository(
                    new File(plugin.getDataFolder(), "custom-items/items.yml"));
        } catch (IOException exception) {
            repository = null;
            plugin.getLogger().severe("Custom item storage is unavailable: " + exception.getMessage());
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        closeOpenEditors();
        HandlerList.unregisterAll(this);
        drafts.clear();
        prompts.clear();
        editingExisting.clear();
        editorTransitions.clear();
        repository = null;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void openCreateEditor(Player player) {
        if (!enabled || repository == null) {
            player.sendMessage(Component.text("Editor custom itemů není dostupný.", NamedTextColor.RED));
            return;
        }
        openHome(player);
    }

    private void createDraft(Player player) {
        Material material = heldMaterial(player);
        String id = "item_" + Long.toString(System.currentTimeMillis(), 36);
        drafts.put(player.getUniqueId(), new CustomItemDefinition(
                id, material, "Nový předmět", "items/" + id, null, CustomItemStats.EMPTY));
        editingExisting.remove(player.getUniqueId());
        openEditor(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof HomeHolder home) {
            event.setCancelled(true);
            if (!home.ownerId.equals(player.getUniqueId())) {
                player.closeInventory();
                return;
            }
            if (event.getRawSlot() == HOME_CREATE_SLOT) {
                createDraft(player);
            } else if (event.getRawSlot() == HOME_CATALOG_SLOT) {
                openCatalog(player);
            }
            return;
        }
        if (holder instanceof CatalogHolder catalog) {
            event.setCancelled(true);
            if (!catalog.ownerId.equals(player.getUniqueId())) {
                player.closeInventory();
                return;
            }
            handleCatalogClick(player, event.getRawSlot(), catalog.items);
            return;
        }
        if (holder instanceof EditorHolder editor) {
            event.setCancelled(true);
            if (!editor.ownerId.equals(player.getUniqueId())) {
                player.closeInventory();
                return;
            }
            handleEditorClick(player, event.getRawSlot());
            return;
        }
        Prompt prompt = prompts.get(player.getUniqueId());
        if (prompt != null && event.getView() instanceof AnvilView anvilView) {
            event.setCancelled(true);
            if (event.getRawSlot() == 2) {
                submitPrompt(player, prompt, anvilView);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EditorHolder
                || event.getView().getTopInventory().getHolder() instanceof HomeHolder
                || event.getView().getTopInventory().getHolder() instanceof CatalogHolder
                || (event.getWhoClicked() instanceof Player player
                && prompts.containsKey(player.getUniqueId()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)
                || !prompts.containsKey(player.getUniqueId())) {
            return;
        }
        event.getView().setRepairCost(0);
        event.getView().setMaximumRepairCost(0);
        event.getView().setBypassCost(true);
        event.setResult(item(Material.LIME_DYE, "Potvrdit hodnotu", NamedTextColor.GREEN,
                "Kliknutím uložíš zadaný text."));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getView() instanceof AnvilView) {
            Prompt prompt = prompts.remove(player.getUniqueId());
            if (prompt != null) {
                event.getInventory().clear();
                reopenEditor(player);
            }
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof EditorHolder) {
            if (editorTransitions.remove(player.getUniqueId())) {
                return;
            }
            if (!prompts.containsKey(player.getUniqueId())) {
                drafts.remove(player.getUniqueId());
                editingExisting.remove(player.getUniqueId());
            }
        }
    }

    private void handleEditorClick(Player player, int slot) {
        CustomItemDefinition draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            player.closeInventory();
            return;
        }
        switch (slot) {
            case ID_SLOT -> {
                if (editingExisting.contains(player.getUniqueId())) {
                    player.sendActionBar(Component.text("ID existujícího itemu je neměnné.", NamedTextColor.RED));
                } else {
                    openPrompt(player, new Prompt(Field.ID, null), draft.id());
                }
            }
            case NAME_SLOT -> openPrompt(player, new Prompt(Field.NAME, null), draft.displayName());
            case MATERIAL_SLOT -> {
                Material material = heldMaterial(player);
                drafts.put(player.getUniqueId(), draft.withMaterial(material));
                openEditor(player);
            }
            case MODEL_SLOT -> openPrompt(player, new Prompt(Field.MODEL, null), draft.modelKey());
            case LEGACY_MODEL_SLOT -> openPrompt(player, new Prompt(Field.CUSTOM_MODEL_DATA, null),
                    draft.customModelData() == null ? "-" : draft.customModelData().toString());
            case DAMAGE_SLOT -> openStatPrompt(player, CustomItemStat.ATTACK_DAMAGE, draft);
            case SPEED_SLOT -> openStatPrompt(player, CustomItemStat.ATTACK_SPEED, draft);
            case ARMOR_SLOT -> openStatPrompt(player, CustomItemStat.ARMOR, draft);
            case TOUGHNESS_SLOT -> openStatPrompt(player, CustomItemStat.ARMOR_TOUGHNESS, draft);
            case HEALTH_SLOT -> openStatPrompt(player, CustomItemStat.MAX_HEALTH_BONUS, draft);
            case SAVE_SLOT -> save(player, draft);
            case CANCEL_SLOT -> {
                drafts.remove(player.getUniqueId());
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    private void openStatPrompt(Player player, CustomItemStat stat, CustomItemDefinition draft) {
        Double value = draft.stats().value(stat);
        openPrompt(player, new Prompt(Field.STAT, stat), value == null ? "-" : format(value));
    }

    private void openHome(Player player) {
        HomeHolder holder = new HomeHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraRPG — custom itemy", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        inventory.setItem(HOME_CREATE_SLOT, item(Material.ANVIL, "Vytvořit nový item", NamedTextColor.GREEN,
                "Otevře editor nové definice."));
        inventory.setItem(HOME_CATALOG_SLOT, item(Material.BOOKSHELF, "Katalog itemů", NamedTextColor.AQUA,
                "Otevře uložené definice pro úpravu."));
        player.openInventory(inventory);
    }

    private void openCatalog(Player player) {
        List<CustomItemDefinition> definitions = repository.findAll();
        List<CustomItemDefinition> displayed = new ArrayList<>(
                definitions.subList(0, Math.min(definitions.size(), CATALOG_ITEM_LIMIT)));
        CatalogHolder holder = new CatalogHolder(player.getUniqueId(), List.copyOf(displayed));
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("NekaraRPG — katalog itemů", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        for (int slot = 0; slot < displayed.size(); slot++) {
            inventory.setItem(slot, factory.create(displayed.get(slot)));
        }
        inventory.setItem(CATALOG_BACK_SLOT, GuiItems.back("Zpět na správu itemů"));
        if (definitions.size() > CATALOG_ITEM_LIMIT) {
            inventory.setItem(53, item(Material.PAPER, "Zobrazeno prvních " + CATALOG_ITEM_LIMIT,
                    NamedTextColor.YELLOW, "Katalog má " + definitions.size() + " definic."));
        }
        player.openInventory(inventory);
    }

    private void handleCatalogClick(Player player, int slot, List<CustomItemDefinition> items) {
        if (slot == CATALOG_BACK_SLOT) {
            openHome(player);
            return;
        }
        if (slot < 0 || slot >= items.size()) {
            return;
        }
        CustomItemDefinition definition = items.get(slot);
        drafts.put(player.getUniqueId(), definition);
        editingExisting.add(player.getUniqueId());
        openEditor(player);
    }

    private void openEditor(Player player) {
        CustomItemDefinition draft = drafts.get(player.getUniqueId());
        if (draft == null || !player.isOnline() || !enabled) {
            return;
        }
        EditorHolder holder = new EditorHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("NekaraRPG — tvorba itemu", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        inventory.setItem(ID_SLOT, item(Material.NAME_TAG, "Custom ID", NamedTextColor.AQUA,
                draft.id(), "Stabilní serverová identita v PDC."));
        inventory.setItem(NAME_SLOT, item(Material.WRITABLE_BOOK, "Zobrazovaný název", NamedTextColor.YELLOW,
                draft.displayName(), "Klikni a napiš název v kovadlině."));
        inventory.setItem(MATERIAL_SLOT, item(draft.material(), "Vanilla základ", NamedTextColor.GOLD,
                draft.material().getKey().getKey(), "Kliknutím načteš předmět z hlavní ruky."));
        inventory.setItem(MODEL_SLOT, item(Material.ITEM_FRAME, "Resource-pack model", NamedTextColor.LIGHT_PURPLE,
                "nekararpg:" + draft.modelKey(), "Cesta k modernímu item_model."));
        inventory.setItem(LEGACY_MODEL_SLOT, item(Material.PAINTING, "CustomModelData (legacy)", NamedTextColor.GRAY,
                draft.customModelData() == null ? "Vypnuto" : draft.customModelData().toString(),
                "Volitelné číslo pro starší modely; '-' vypne."));
        inventory.setItem(DAMAGE_SLOT, statItem(Material.IRON_SWORD, CustomItemStat.ATTACK_DAMAGE, draft));
        inventory.setItem(SPEED_SLOT, statItem(Material.FEATHER, CustomItemStat.ATTACK_SPEED, draft));
        inventory.setItem(ARMOR_SLOT, statItem(Material.IRON_CHESTPLATE, CustomItemStat.ARMOR, draft));
        inventory.setItem(TOUGHNESS_SLOT, statItem(Material.NETHERITE_CHESTPLATE,
                CustomItemStat.ARMOR_TOUGHNESS, draft));
        inventory.setItem(HEALTH_SLOT, statItem(Material.GOLDEN_APPLE, CustomItemStat.MAX_HEALTH_BONUS, draft));
        inventory.setItem(PREVIEW_SLOT, factory.create(draft));
        inventory.setItem(SAVE_SLOT, item(Material.LIME_CONCRETE, "Vytvořit a uložit", NamedTextColor.GREEN,
                "Definice se uloží a item dostaneš do inventáře."));
        inventory.setItem(CANCEL_SLOT, item(Material.BARRIER, "Zrušit", NamedTextColor.RED,
                "Rozpracovaná definice se zahodí."));
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof EditorHolder) {
            editorTransitions.add(player.getUniqueId());
        }
        player.openInventory(inventory);
    }

    private ItemStack statItem(Material material, CustomItemStat stat, CustomItemDefinition draft) {
        Double value = draft.stats().value(stat);
        return item(material, stat.czechName(), value == null ? NamedTextColor.GRAY : NamedTextColor.GREEN,
                value == null ? "Výchozí vanilla hodnota" : format(value),
                "Klikni a zadej číslo; '-' atribut vypne.");
    }

    private void openPrompt(Player player, Prompt prompt, String currentValue) {
        prompts.put(player.getUniqueId(), prompt);
        InventoryView opened = player.openAnvil(null, true);
        if (!(opened instanceof AnvilView anvilView)) {
            prompts.remove(player.getUniqueId());
            player.sendMessage(Component.text("Textové GUI se nepodařilo otevřít.", NamedTextColor.RED));
            reopenEditor(player);
            return;
        }
        AnvilInventory inventory = anvilView.getTopInventory();
        inventory.setFirstItem(item(Material.PAPER, currentValue, NamedTextColor.WHITE,
                "Uprav text nahoře a potvrď výsledek."));
        anvilView.setRepairCost(0);
        anvilView.setMaximumRepairCost(0);
        anvilView.setBypassCost(true);
    }

    private void submitPrompt(Player player, Prompt prompt, AnvilView view) {
        String input = view.getRenameText() == null ? "" : view.getRenameText().trim();
        CustomItemDefinition draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            prompts.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        try {
            CustomItemDefinition updated = switch (prompt.field) {
                case ID -> draft.withId(input);
                case NAME -> draft.withDisplayName(input);
                case MODEL -> draft.withModelKey(stripNamespace(input));
                case CUSTOM_MODEL_DATA -> draft.withCustomModelData(parseCustomModelData(input));
                case STAT -> draft.withStat(prompt.stat, parseStat(input, prompt.stat));
            };
            drafts.put(player.getUniqueId(), updated);
        } catch (IllegalArgumentException exception) {
            player.sendActionBar(Component.text(czechValidationMessage(prompt), NamedTextColor.RED));
            return;
        }
        prompts.remove(player.getUniqueId());
        view.getTopInventory().clear();
        player.closeInventory();
        reopenEditor(player);
    }

    private void save(Player player, CustomItemDefinition draft) {
        boolean existing = editingExisting.contains(player.getUniqueId());
        try {
            if (existing) {
                repository.update(draft);
            } else if (repository.find(draft.id()).isPresent()) {
            player.sendActionBar(Component.text("Toto custom ID už existuje.", NamedTextColor.RED));
            return;
            } else {
                repository.create(draft);
            }
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().severe("Cannot save custom item " + draft.id() + ": " + exception.getMessage());
            player.sendActionBar(Component.text("Item se nepodařilo bezpečně uložit.", NamedTextColor.RED));
            return;
        }
        drafts.remove(player.getUniqueId());
        editingExisting.remove(player.getUniqueId());
        ItemStack created = factory.create(draft);
        player.closeInventory();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(created);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(Component.text("Vytvořen custom item ", NamedTextColor.GREEN)
                .append(Component.text(draft.id(), NamedTextColor.AQUA))
                .append(Component.text(". Definice je v custom-items/items.yml.", NamedTextColor.GREEN)));
    }

    private Integer parseCustomModelData(String input) {
        if (isDisabled(input)) {
            return null;
        }
        int value = Integer.parseInt(input);
        if (value < 1) {
            throw new IllegalArgumentException("positive value required");
        }
        return value;
    }

    private Double parseStat(String input, CustomItemStat stat) {
        if (isDisabled(input)) {
            return null;
        }
        double value = Double.parseDouble(input.replace(',', '.'));
        if (!Double.isFinite(value) || value < stat.minimum() || value > stat.maximum()) {
            throw new IllegalArgumentException("value outside range");
        }
        return value;
    }

    private boolean isDisabled(String input) {
        return input.isBlank() || input.equals("-") || input.equalsIgnoreCase("off")
                || input.equalsIgnoreCase("vypnout");
    }

    private String stripNamespace(String input) {
        return input.toLowerCase(Locale.ROOT).startsWith("nekararpg:") ? input.substring(10) : input;
    }

    private String czechValidationMessage(Prompt prompt) {
        if (prompt.field == Field.ID) {
            return "ID: 2–64 znaků a-z, 0-9, pomlčka nebo podtržítko.";
        }
        if (prompt.field == Field.NAME) {
            return "Název musí mít 1–64 tisknutelných znaků.";
        }
        if (prompt.field == Field.MODEL) {
            return "Model musí být malá resource cesta, např. items/ocelovy_mec.";
        }
        if (prompt.field == Field.CUSTOM_MODEL_DATA) {
            return "Zadej kladné celé číslo nebo '-' pro vypnutí.";
        }
        return "Zadej číslo " + format(prompt.stat.minimum()) + " až "
                + format(prompt.stat.maximum()) + ", nebo '-' pro vypnutí.";
    }

    private Material heldMaterial(Player player) {
        Material material = player.getInventory().getItemInMainHand().getType();
        return material.isAir() ? Material.STICK : material;
    }

    private ItemStack item(Material material, String name, NamedTextColor color, String... loreLines) {
        Component[] lore = java.util.Arrays.stream(loreLines)
                .map(line -> Component.text(line, NamedTextColor.GRAY)).toArray(Component[]::new);
        return GuiItems.item(material, Component.text(name, color), lore);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void reopenEditor(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> openEditor(player));
    }

    private void closeOpenEditors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof EditorHolder
                    || prompts.containsKey(player.getUniqueId())) {
                player.getOpenInventory().getTopInventory().clear();
                player.closeInventory();
            }
        }
    }

    private enum Field {
        ID,
        NAME,
        MODEL,
        CUSTOM_MODEL_DATA,
        STAT
    }

    private record Prompt(Field field, CustomItemStat stat) {
    }

    private static final class HomeHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private HomeHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class CatalogHolder implements InventoryHolder {
        private final UUID ownerId;
        private final List<CustomItemDefinition> items;
        private Inventory inventory;

        private CatalogHolder(UUID ownerId, List<CustomItemDefinition> items) {
            this.ownerId = ownerId;
            this.items = items;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class EditorHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private EditorHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
