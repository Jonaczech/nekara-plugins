package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultPerkTree {

    private final PerkCatalog catalog;
    private final Map<PerkId, PerkPresentation> presentations;

    private DefaultPerkTree(List<PerkDefinition> definitions, Map<PerkId, PerkPresentation> presentations) {
        this.catalog = new PerkCatalog(definitions);
        this.presentations = Map.copyOf(presentations);
        if (catalog.size() != this.presentations.size()) {
            throw new IllegalArgumentException("Every perk must have a presentation");
        }
    }

    public static DefaultPerkTree create() {
        Builder builder = new Builder();
        builder.tree(SkillId.MARTIAL_ARTS,
            node("discipline", "Kázeň pěsti", "Trpělivý boj dává úderům větší sílu.", stat(StatId.DAMAGE_MULTIPLIER, 0.03), stat(StatId.STUN_CHANCE, 0.01)),
            node("footwork", "Tichý krok", "Pohyb mezi ranami zvyšuje šanci na úhyb.", stat(StatId.DODGE_CHANCE, 0.012)),
            node("held_punch", "Zadržený úder", "Odemkne zesílení úderu po vyčkání.", mechanic(MechanicId.PUNCH_HOLDING)),
            node("air_combo", "Vzdušný rozsudek", "Odemkne zvedák a navazující odkopnutí.", mechanic(MechanicId.UPPERCUT), mechanic(MechanicId.DROPKICK)),
            node("grapple", "Železné sevření", "Odemkne zpomalení a odzbrojení protivníka.", mechanic(MechanicId.GRAPPLE)),
            node("meditation", "Klid nad krajinou", "Odemkne meditaci a její volitelné požehnání.", mechanic(MechanicId.MEDITATION)));
        builder.tree(SkillId.TRADING,
            node("reputation", "Dobré jméno", "Obchodování rychleji buduje důvěru vesničanů.", stat(StatId.REPUTATION_GAIN, 0.06)),
            node("discount", "Jistá ruka", "Vyjednané ceny jsou příznivější.", stat(StatId.TRADE_DISCOUNT, 0.01)),
            node("selection", "Širší nabídka", "Vesničané zpřístupní více nabídek a lepší zboží.", stat(StatId.TRADE_SELECTION_BONUS, 1.0), stat(StatId.VILLAGER_SKILL, 0.05)),
            node("ordering", "Objednávková kniha", "Odemkne objednávání známých obchodů.", mechanic(MechanicId.VILLAGER_ORDERING), mechanic(MechanicId.VILLAGER_GIFTS)),
            node("services", "Mistrovské služby", "Odemkne opravy, vylepšení a výcvik.", mechanic(MechanicId.VILLAGER_UPGRADING), mechanic(MechanicId.VILLAGER_TRAINING)),
            node("black_market", "Šeptaný trh", "Odemkne vzácné nabídky na hlavní úrovni stezky.", mechanic(MechanicId.BLACK_MARKET)));
        builder.tree(SkillId.SMITHING,
            node("craft", "Poctivé řemeslo", "Výbava má nejméně Neobyčejnou kvalitu; od III. hodnosti může být i Vzácná.", stat(StatId.ITEM_QUALITY, 0.05)),
            node("economy", "Úsporný výkovek", "Při výrobě a úpravách se šetří materiál.", stat(StatId.RESOURCE_COST_REDUCTION, 0.012)),
            node("recipes", "Zapomenuté nákresy", "Odemkne řemeslnickou soupravu a efektivní výrobu stavebních surovin.", mechanic(MechanicId.SMITHING_RECIPES), mechanic(MechanicId.BULK_CRAFTING)),
            node("fine_work", "Jemná práce", "Odemyká Epickou kvalitu vyrobené výbavy.", stat(StatId.ITEM_QUALITY, 0.08)),
            node("tinkering", "Dílenské úpravy", "Odemkne bezpečnou opravu výbavy řemeslnickou soupravou.", mechanic(MechanicId.TINKERING)),
            node("masterwork", "Mistrovský kus", "Odemyká Legendární kvalitu vyrobené výbavy.", stat(StatId.ITEM_QUALITY, 0.20)));
        builder.enchantingTree(
            node("runes", "Čitelné runy", "I. hodnost odemkne Tier I runy; každá hodnost zlevňuje vanilla očarování o 3 %.", stat(StatId.EXPERIENCE_COST_REDUCTION, 0.03)),
            node("experience", "Šetrný zápis", "Každá hodnost zlevňuje tvorbu run o 4 %; III. hodnost odemkne Tier II.", stat(StatId.RUNE_EXPERIENCE_COST_REDUCTION, 0.04)),
            node("lapis", "Čistý pigment", "Každá hodnost přidává 4% šanci zachovat lapis při vanilla očarování.", stat(StatId.RESOURCE_COST_REDUCTION, 0.04)),
            node("limits", "Za hranou písma", "Každá hodnost přidává 5% šanci zachovat barvivo; III. hodnost odemkne Tier III.", stat(StatId.RUNE_DYE_PRESERVATION_CHANCE, 0.05)),
            node("insight", "Výklad magie", "Postupně posiluje zkušenostní koule a zisk XP do Runotepectví.",
                rankedStat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.05, 0.05, 0.10),
                rankedStat(StatId.EXPERIENCE_MULTIPLIER, 0.00, 0.05, 0.10)),
            node("hexblade", "Runová paměť", "Při úspěšném vrytí má runa 10 % šanci vrátit se a obnovit 25 % základní ceny jejího zápisu.", mechanic(MechanicId.RUNE_MEMORY)));
        builder.tree(SkillId.ALCHEMY,
            node("potency", "Čistá esence", "Vařené lektvary získávají vyšší účinek.", stat(StatId.POTION_POWER, 0.05)),
            node("brew_speed", "Rychlý var", "Varné stojany pracují rychleji.", stat(StatId.BREWING_SPEED, 0.08)),
            node("ingredients", "Střídmá dávka", "Při vaření se šetří přísady.", stat(StatId.RESOURCE_COST_REDUCTION, 0.012)),
            node("vials", "Letící sklo", "Zlepšuje vrhací a přetrvávající lektvary.", stat(StatId.THROWING_SPEED, 0.06), stat(StatId.POTION_POWER, 0.04)),
            node("recipes", "Herbář Nekary", "Odemkne recept Tonika vitality ze vzácných plodů.", mechanic(MechanicId.ALCHEMY_RECIPES)),
            node("merging", "Bojové esence", "Odemkne sloučení lektvarů i nanášení jejich účinků na zbraně.",
                mechanic(MechanicId.POTION_MERGING), mechanic(MechanicId.WEAPON_COATING)));
        builder.tree(SkillId.MINING,
            node("yield", "Křehká skála", "Zrychluje těžbu a za vytěžený blok dává vanilkové zkušenosti.",
                stat(StatId.MINING_SPEED, 0.02), stat(StatId.MINING_BLOCK_EXPERIENCE, 0.04)),
            node("tempo", "Trpasličí nadání", "Dále zrychluje těžbu, posiluje zkušenosti a odemyká Drilling.",
                stat(StatId.MINING_SPEED, 0.04), stat(StatId.MINING_BLOCK_EXPERIENCE, 0.06),
                stat(StatId.DRILLING_SPEED_MULTIPLIER, 0.60), stat(StatId.DRILLING_COOLDOWN_REDUCTION, 0.10),
                mechanic(MechanicId.DRILLING)),
            node("furnace", "Jeskynní průzkum", "Přidává výtěžek z Těžby, štěstí a urychluje tavení rud.",
                stat(StatId.DOUBLE_DROP_CHANCE, 0.10), stat(StatId.LUCK, 1.0), stat(StatId.FURNACE_SPEED, 0.25)),
            node("vein", "Těžba žil", "Odemkne Vein Mining, posílí výtěžek, štěstí a tavení rud.",
                mechanic(MechanicId.VEIN_MINING), stat(StatId.DOUBLE_DROP_CHANCE, 0.10),
                stat(StatId.LUCK, 3.0), stat(StatId.FURNACE_SPEED, 0.25)),
            node("blast", "Demoliční mistr", "Posílí řízené TNT, jeho výtěžek a výrobu.",
                stat(StatId.TNT_POWER, 1.0), stat(StatId.TNT_BONUS_DROP_CHANCE, 0.40), mechanic(MechanicId.TNT_MASTERY)),
            node("triple", "Aspekt hlubin", "Vein Mining rozšíří na kamenné clustery a další tavení rud.",
                mechanic(MechanicId.VEIN_CLUSTER_EXTRACTION), stat(StatId.FURNACE_SPEED, 0.25)));
        builder.tree(SkillId.WOODCUTTING,
            node("yield", "Míza lesa", "Zrychluje kácení a za pokácený log dává vanilkové zkušenosti.",
                stat(StatId.WOODCUTTING_SPEED, 0.02), stat(StatId.WOODCUTTING_LOG_EXPERIENCE, 0.05)),
            node("tempo", "Jistý zásek", "Arborista zrychluje kácení a růst zasazených saplingů.",
                stat(StatId.WOODCUTTING_SPEED, 0.04), stat(StatId.SAPLING_GROWTH_MULTIPLIER, 0.20)),
            node("recipes", "Aktivní život", "Jídlo lépe zasytí, hlad ubývá pomaleji a zdraví se rychleji obnovuje.",
                stat(StatId.FOOD_SATURATION_MULTIPLIER, 0.30), stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.20),
                stat(StatId.HEALTH_REGENERATION, 0.25)),
            node("feller", "Pád velikána", "Plížením a pravým kliknutím aktivuje krátké kácení propojených přírodních stromů.", mechanic(MechanicId.TREE_FELLER)),
            node("leaves", "Zlaté listí", "Listí může skrýt zlaté jablko a vydává vanilkové zkušenosti.",
                mechanic(MechanicId.RARE_LEAF_DROPS), mechanic(MechanicId.GOLDEN_LEAVES),
                stat(StatId.GOLDEN_LEAF_APPLE_CHANCE, 0.0025), stat(StatId.WOODCUTTING_LEAF_EXPERIENCE, 0.04)),
            node("triple", "Křišťálové listí", "Zvyšuje šanci Zlatého listí a sekera listí ničí okamžitě.",
                mechanic(MechanicId.INSTANT_LEAF_BREAK), stat(StatId.GOLDEN_LEAF_APPLE_CHANCE, 0.0025)));
        builder.tree(SkillId.DIGGING,
            node("yield", "Kopáč", "Zrychluje práci lopatou a za vykopaný blok dává vanilkové zkušenosti.",
                stat(StatId.DIGGING_SPEED, 0.06), stat(StatId.DIGGING_BLOCK_EXPERIENCE, 0.04)),
            node("tempo", "Bagr", "Dále zrychluje kopání hlíny, písku a štěrku.", stat(StatId.DIGGING_SPEED, 0.10)),
            node("finds", "Síto", "Odemkne běžné poklady z kopání a přidává vanilkové zkušenosti.",
                stat(StatId.RARE_DROP_CHANCE, 0.025), stat(StatId.DIGGING_BLOCK_EXPERIENCE, 0.06)),
            node("archaeology", "Archeolog", "Po vyčištění má podezřelý blok 20% šanci se obnovit.",
                mechanic(MechanicId.ARCHAEOLOGY_FINDS), mechanic(MechanicId.SUSPICIOUS_BLOCK_RESTORATION)),
            node("deep_soil", "Replikace zeminy", "Odemkne recepty na obnovu travnaté a zakořeněné zeminy.",
                mechanic(MechanicId.GROUND_REPLICATION)),
            node("triple", "Skrytý poklad", "Výrazně zvyšuje šanci na běžný poklad z kopání.",
                stat(StatId.RARE_DROP_CHANCE, 0.050)));
        builder.tree(SkillId.FARMING,
            node("yield", "Plná ošatka", "Požehnaná sklizeň přidává výtěžek a vanilkové zkušenosti.",
                stat(StatId.FARMING_BONUS_DROP_CHANCE, 0.04)),
            node("growth", "Živá půda", "Plodiny v péči hráče rostou rychleji a vydají zkušenosti.",
                stat(StatId.CROP_GROWTH_MULTIPLIER, 0.05), stat(StatId.FARMING_HARVEST_EXPERIENCE, 0.05),
                stat(StatId.FOOD_COOKING_SPEED, 0.25)),
            node("husbandry", "Péče o stádo", "Zrychluje chov, posiluje řeznictví a rozmnožování zvířat.",
                stat(StatId.ANIMAL_GROWTH_MULTIPLIER, 0.60), stat(StatId.ANIMAL_BONUS_DROP_CHANCE, 0.30),
                stat(StatId.BREEDING_EXPERIENCE_MULTIPLIER, 0.05), stat(StatId.ANIMAL_DAMAGE_MULTIPLIER, 3.0)),
            node("instant", "Obratná sklizeň", "Pravým tlačítkem sklidí plodinu a ihned ji znovu zasadí.", mechanic(MechanicId.INSTANT_HARVEST)),
            node("triple", "Včelařova péče", "Včely neútočí a úl má šanci ihned obnovit med.",
                mechanic(MechanicId.BEEKEEPER), stat(StatId.BEEKEEPING_HONEY_REFILL_CHANCE, 0.50)),
            node("field", "Záběr pole", "Při plížení spustí dočasnou sklizeň a opětovné sázení v ploše 5×5.", mechanic(MechanicId.FIELD_HARVEST)));
        builder.tree(SkillId.FISHING,
            node("luck", "Čtení proudu", "Zvyšuje šanci na poklad z vlastní rybářské tabulky.", stat(StatId.FISHING_TREASURE_CHANCE, 0.10)),
            node("speed", "Napjatý vlasec", "Zkracuje čekání na záběr.", stat(StatId.FISHING_SPEED, 0.05)),
            node("wisdom", "Moudrost hlubin", "Úlovky vydají více zkušenostních koulí.", stat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.03)),
            node("equipment", "Potopená schránka", "Po úspěšném úlovku může vedle rybáře vytvořit dočasnou truhlu s vybavením nebo spotřebními zásobami.", mechanic(MechanicId.FISHING_CHEST)),
            node("salvage", "Naladění vody", "Úspěšné úlovky budují až 10 stacků Naladění vody a zvyšují šanci na poklad.", mechanic(MechanicId.FISHING_WATER_ATTUNEMENT)),
            node("master", "Pán tichých vod", "Spojuje nejvyšší rychlost, poklady, výtěžek a vyšší šanci Potopené schránky.", stat(StatId.FISHING_TREASURE_CHANCE, 0.15), stat(StatId.FISHING_SPEED, 0.20), stat(StatId.DOUBLE_DROP_CHANCE, 0.05), mechanic(MechanicId.FISHING_MASTER_CHEST)));
        builder.tree(SkillId.LIGHT_WEAPONS,
            node("damage", "Čepel v pohybu", "Zvyšuje poškození lehkých zbraní a šanci na krvácení.", stat(StatId.DAMAGE_MULTIPLIER, 0.02), stat(StatId.BLEED_CHANCE, 0.008)),
            node("critical", "Rytmus souboje", "Zrychluje útoky lehkou zbraní, zvyšuje kritickou šanci a odemyká volný pohyb se železnými a zlatými lehkými zbraněmi.", stat(StatId.LIGHT_WEAPON_ATTACK_SPEED, 0.02), stat(StatId.CRITICAL_CHANCE, 0.01), mechanic(MechanicId.LIGHT_WEAPON_IRON_MOBILITY)),
            node("parry", "Lehký krok", "Dále zrychluje útoky lehkou zbraní a odemyká volný pohyb s diamantovými lehkými zbraněmi.", stat(StatId.LIGHT_WEAPON_ATTACK_SPEED, 0.04), mechanic(MechanicId.LIGHT_WEAPON_DIAMOND_MOBILITY)),
            node("coating", "Hluboký řez", "Zvyšuje šanci a poškození krvácení.", stat(StatId.BLEED_CHANCE, 0.03), stat(StatId.BLEED_DAMAGE_MULTIPLIER, 0.25)),
            node("immunity", "Vražedný úhel", "Kritické zásahy mají vlastní šanci vyvolat krvácení a odemyká volný pohyb s netheritovými lehkými zbraněmi.", stat(StatId.CRITICAL_BLEED_CHANCE, 0.35), mechanic(MechanicId.LIGHT_WEAPON_NETHERITE_MOBILITY)),
            node("master", "Tisíc řezů", "Vrchol stezky posiluje kritické zásahy a krvácení po kritickém úderu.", stat(StatId.CRITICAL_CHANCE, 0.06), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.25), stat(StatId.CRITICAL_BLEED_CHANCE, 0.35), stat(StatId.BLEED_FLAT_DAMAGE, 1.0)));
        builder.tree(SkillId.HEAVY_WEAPONS,
            node("damage", "Váha rozsudku", "Zvyšuje poškození těžkých zbraní.", stat(StatId.DAMAGE_MULTIPLIER, 0.02)),
            node("power", "Drtivý nápřah", "Zesiluje plně nabité útoky, zvyšuje kritickou šanci a odemyká volný pohyb se železnými a zlatými těžkými zbraněmi.", stat(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER, 0.04), stat(StatId.CRITICAL_CHANCE, 0.006), mechanic(MechanicId.HEAVY_WEAPON_IRON_MOBILITY)),
            node("critical", "Prasklá obrana", "Zvyšuje průraznost zbroje a odemyká volný pohyb s diamantovými těžkými zbraněmi.", stat(StatId.ARMOR_PENETRATION, 0.08), mechanic(MechanicId.HEAVY_WEAPON_DIAMOND_MOBILITY)),
            node("penetration", "Široký rozmach", "Plně nabitý útok získá rodinový plošný účinek: sekera krátký rozmach, obouruční meč širší cleave a kladivo rázovou vlnu.", mechanic(MechanicId.HEAVY_POWER_SWEEP)),
            node("coating", "Drtivý průlom", "Zvyšuje průraznost, šanci omráčit a odemyká volný pohyb s netheritovými těžkými zbraněmi.", stat(StatId.ARMOR_PENETRATION, 0.12), stat(StatId.STUN_CHANCE, 0.12), mechanic(MechanicId.HEAVY_WEAPON_NETHERITE_MOBILITY)),
            node("master", "Otřes země", "Vrchol stezky dále posiluje plně nabité útoky, průraznost a kritické zásahy.", stat(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER, 0.20), stat(StatId.ARMOR_PENETRATION, 0.10), stat(StatId.CRITICAL_CHANCE, 0.04)));
        builder.tree(SkillId.ARCHERY,
            node("damage", "Pevná tětiva", "Zvyšuje poškození luků a kuší.", stat(StatId.DAMAGE_MULTIPLIER, 0.03)),
            node("accuracy", "Klidný dech", "Zlepšuje přesnost střelby.", stat(StatId.ACCURACY, 0.025)),
            node("critical", "Smrtící úhel", "Zvyšuje šanci a sílu kritického zásahu.", stat(StatId.CRITICAL_CHANCE, 0.012), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.04)),
            node("arrows", "Šípařova brašna", "Odemkne Šíp průzkumníka, který označí zasažený cíl.", mechanic(MechanicId.CUSTOM_ARROW_RECIPES), stat(StatId.AMMO_CONSUMPTION_REDUCTION, 0.02)),
            node("charged", "Zadržený výstřel", "Odemkne nabité výstřely s volitelným posílením.", mechanic(MechanicId.CHARGED_SHOT)),
            node("master", "Oko bouře", "Vrchol stezky posiluje přesnost i kritický zásah.", stat(StatId.ACCURACY, 0.12), stat(StatId.CRITICAL_CHANCE, 0.08)));
        builder.tree(SkillId.LIGHT_ARMOR,
            node("armor", "Železné cvoky", "Zvyšuje účinnost lehké výstroje.", stat(StatId.ARMOR_MULTIPLIER, 0.02)),
            node("mobility", "Nespoutaný krok", "Snižuje zátěž lehké výstroje o polovinu a odemyká volný pohyb v chainmailové lehké výstroji.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.50), mechanic(MechanicId.LIGHT_ARMOR_CHAINMAIL_MOBILITY)),
            node("dodge", "Řízený metabolismus", "Kompletní lehká sada šetří hlad, zvyšuje ochranu a odemyká volný pohyb v diamantové lehké výstroji.", stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.30), stat(StatId.ARMOR_MULTIPLIER, 0.10), mechanic(MechanicId.LIGHT_ARMOR_SET_BONUS), mechanic(MechanicId.LIGHT_ARMOR_DIAMOND_MOBILITY)),
            node("sustenance", "Vynalézavý tulák", "Pro bonusy lehké sady stačí tři kusy výstroje a lehká výstroj už nezpůsobuje pohybovou zátěž.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.50), mechanic(MechanicId.LIGHT_ARMOR_THREE_PIECE_SET_BONUS)),
            node("adrenaline", "Adrenalin", "Při nízkém zdraví krátce udělí Rychlost II a Regeneraci I; znovu jej lze spustit po 60 s.", mechanic(MechanicId.ADRENALINE)),
            node("master", "Bleskové reflexy", "Aktivní lehká sada získá +5% rychlost pohybu, šanci zcela uhnout zásahu a další ochranu.", stat(StatId.DODGE_CHANCE, 0.20), stat(StatId.ARMOR_MULTIPLIER, 0.10), stat(StatId.LIGHT_ARMOR_MOVEMENT_SPEED, 0.05)));
        builder.tree(SkillId.HEAVY_ARMOR,
            node("armor", "Zpevněná výstroj", "Zvyšuje účinnost těžké výstroje.", stat(StatId.ARMOR_MULTIPLIER, 0.02)),
            node("burden", "Nohy z oceli", "Snižuje zátěž těžké výstroje o polovinu, zlepšuje regeneraci, úsporu hladu a přesnost střelby; odemyká pohyb v železné a zlaté zbroji.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.50), stat(StatId.HEALTH_REGENERATION, 0.10), stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.10), stat(StatId.ACCURACY, 0.10), mechanic(MechanicId.HEAVY_ARMOR_IRON_MOBILITY)),
            node("recovery", "Vitální ocel", "Kompletní těžká sada zlepší regeneraci a ochranu; odemyká pohyb v netheritové zbroji.", stat(StatId.HEALTH_REGENERATION, 0.40), stat(StatId.ARMOR_MULTIPLIER, 0.10), mechanic(MechanicId.HEAVY_ARMOR_SET_BONUS), mechanic(MechanicId.HEAVY_ARMOR_NETHERITE_MOBILITY)),
            node("reflection", "Hněv", "Při nízkém zdraví krátce udělí Sílu I a Odolnost I; znovu jej lze spustit po 60 s.", mechanic(MechanicId.RAGE)),
            node("rage", "Vynalézavý pěšák", "Pro běžné bonusy těžké sady stačí tři kusy výstroje; zátěž těžké výstroje se dále sníží.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.25), mechanic(MechanicId.HEAVY_ARMOR_THREE_PIECE_SET_BONUS)),
            node("master", "Ostnatý Juggernaut", "Kompletní těžká sada má 10% šanci vrátit 20% poškození, získá +40% odolnost proti odhození a imunitu vůči Slowness, Weakness a Levitation.", mechanic(MechanicId.HEAVY_ARMOR_JUGGERNAUT)));
        return builder.build();
    }

    public PerkCatalog catalog() {
        return catalog;
    }

    public PerkPresentation presentation(PerkId id) {
        PerkPresentation presentation = presentations.get(id);
        if (presentation == null) {
            throw new IllegalArgumentException("Missing perk presentation: " + id);
        }
        return presentation;
    }

    private static Node node(String suffix, String name, String description, PerkEffectDefinition... effects) {
        return new Node(suffix, name, description, List.of(effects));
    }

    private static StatPerkEffect stat(StatId id, double amount) {
        return new StatPerkEffect(id, ModifierOperation.ADD, amount);
    }

    private static RankedStatPerkEffect rankedStat(StatId id, Double... amountsByRank) {
        return new RankedStatPerkEffect(id, ModifierOperation.ADD, List.of(amountsByRank));
    }

    private static MechanicPerkEffect mechanic(MechanicId id) {
        return new MechanicPerkEffect(id);
    }

    private record Node(String suffix, String name, String description, List<PerkEffectDefinition> effects) {
    }

    private static final class Builder {
        private final List<PerkDefinition> definitions = new ArrayList<>();
        private final Map<PerkId, PerkPresentation> presentations = new HashMap<>();

        void tree(SkillId skill, Node root, Node left, Node right, Node leftDeep, Node rightDeep, Node crown) {
            PerkTreeLayout layout = PerkTreeLayout.forSkill(skill);
            PerkId rootId = add(skill, root, 5, 1, 0, Set.of(), layout.root());
            PerkId leftId = add(skill, left, 5, 1, 20, Set.of(new PerkRequirement(rootId, 2)), layout.left());
            PerkId rightId = add(skill, right, 1, 2, 20, Set.of(new PerkRequirement(rootId, 2)), layout.right());
            PerkId leftDeepId = add(skill, leftDeep, 1, 3, 50, Set.of(new PerkRequirement(leftId, 3)), layout.leftDeep());
            PerkId rightDeepId = add(skill, rightDeep, 1, 3, 50, Set.of(new PerkRequirement(rightId, 1)), layout.rightDeep());
            add(skill, crown, 1, 5, 100, Set.of(
                new PerkRequirement(leftDeepId, 1),
                new PerkRequirement(rightDeepId, 1)
            ), layout.crown());
        }

        void enchantingTree(Node root, Node left, Node right, Node leftDeep, Node rightDeep, Node crown) {
            SkillId skill = SkillId.ENCHANTING;
            PerkTreeLayout layout = PerkTreeLayout.forSkill(skill);
            PerkId rootId = add(skill, root, 5, 1, List.of(0, 10, 20, 35, 50), Set.of(), layout.root());
            PerkId leftId = add(skill, left, 5, 1, List.of(20, 25, 30, 45, 60),
                Set.of(new PerkRequirement(rootId, 2)), layout.left());
            PerkId rightId = add(skill, right, 5, 2, List.of(20, 35, 50, 70, 85),
                Set.of(new PerkRequirement(rootId, 2)), layout.right());
            PerkId leftDeepId = add(skill, leftDeep, 3, 3, List.of(50, 60, 70),
                Set.of(new PerkRequirement(leftId, 3)), layout.leftDeep());
            PerkId rightDeepId = add(skill, rightDeep, 3, 3, List.of(50, 65, 80),
                Set.of(new PerkRequirement(rightId, 3)), layout.rightDeep());
            add(skill, crown, 1, 5, List.of(100), Set.of(
                new PerkRequirement(leftDeepId, 3),
                new PerkRequirement(rightDeepId, 3)
            ), layout.crown());
        }

        private PerkId add(
            SkillId skill,
            Node node,
            int maxRank,
            int pointCost,
            List<Integer> levels,
            Set<PerkRequirement> requirements,
            PerkPosition position
        ) {
            PerkId id = new PerkId(skill.id() + "." + node.suffix());
            definitions.add(new PerkDefinition(
                id, skill, maxRank, pointCost, levels.getFirst(), levels, requirements, node.effects(), position));
            presentations.put(id, new PerkPresentation(node.name(), node.description()));
            return id;
        }

        private PerkId add(
            SkillId skill,
            Node node,
            int maxRank,
            int pointCost,
            int level,
            Set<PerkRequirement> requirements,
            PerkPosition position
        ) {
            PerkId id = new PerkId(skill.id() + "." + node.suffix());
            definitions.add(new PerkDefinition(
                id, skill, maxRank, pointCost, level, requirements, node.effects(), position));
            presentations.put(id, new PerkPresentation(node.name(), node.description()));
            return id;
        }

        DefaultPerkTree build() {
            return new DefaultPerkTree(definitions, presentations);
        }
    }
}
