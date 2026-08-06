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
            node("craft", "Poctivé řemeslo", "Vlastnoručně vyrobená výbava získává vyšší kvalitu.", stat(StatId.ITEM_QUALITY, 0.05)),
            node("economy", "Úsporný výkovek", "Při výrobě a úpravách se šetří materiál.", stat(StatId.RESOURCE_COST_REDUCTION, 0.012)),
            node("recipes", "Zapomenuté nákresy", "Odemkne řemeslnickou soupravu a efektivní výrobu stavebních surovin.", mechanic(MechanicId.SMITHING_RECIPES), mechanic(MechanicId.BULK_CRAFTING)),
            node("fine_work", "Jemná práce", "Další zlepšení vlastností vyrobených předmětů.", stat(StatId.ITEM_QUALITY, 0.08)),
            node("tinkering", "Dílenské úpravy", "Odemkne bezpečnou opravu výbavy řemeslnickou soupravou.", mechanic(MechanicId.TINKERING)),
            node("masterwork", "Mistrovský kus", "Vrcholné výrobky dosahují nejlepší možné jakosti.", stat(StatId.ITEM_QUALITY, 0.20)));
        builder.tree(SkillId.ENCHANTING,
            node("runes", "Čitelné runy", "Zvyšuje sílu vložených očarování.", stat(StatId.ENCHANTMENT_POWER, 0.05)),
            node("experience", "Šetrný zápis", "Runotepectví spotřebuje méně zkušeností.", stat(StatId.EXPERIENCE_COST_REDUCTION, 0.012)),
            node("lapis", "Čistý pigment", "Při práci se spotřebuje méně surovin.", stat(StatId.RESOURCE_COST_REDUCTION, 0.012)),
            node("insight", "Ozvěna poznání", "Zvyšuje zisk zkušeností dovednosti i koulí.", stat(StatId.EXPERIENCE_MULTIPLIER, 0.03), stat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.04)),
            node("limits", "Za hranou písma", "Runy mohou překročit běžné hranice své síly.", stat(StatId.ENCHANTMENT_POWER, 0.10)),
            node("hexblade", "Čepel živlu", "Odemkne přeměnu části zásahu na živlové zranění.", mechanic(MechanicId.HEXBLADE)));
        builder.tree(SkillId.ALCHEMY,
            node("potency", "Čistá esence", "Vařené lektvary získávají vyšší účinek.", stat(StatId.POTION_POWER, 0.05)),
            node("brew_speed", "Rychlý var", "Varné stojany pracují rychleji.", stat(StatId.BREWING_SPEED, 0.08)),
            node("ingredients", "Střídmá dávka", "Při vaření se šetří přísady.", stat(StatId.RESOURCE_COST_REDUCTION, 0.012)),
            node("vials", "Letící sklo", "Zlepšuje vrhací a přetrvávající lektvary.", stat(StatId.THROWING_SPEED, 0.06), stat(StatId.POTION_POWER, 0.04)),
            node("recipes", "Herbář Nekary", "Odemkne recept Tonika vitality ze vzácných plodů.", mechanic(MechanicId.ALCHEMY_RECIPES)),
            node("merging", "Spojené esence", "Odemkne sloučení účinků dvou lektvarů.", mechanic(MechanicId.POTION_MERGING)));
        builder.tree(SkillId.MINING,
            node("yield", "Křehká skála", "Zrychluje těžbu a za vytěžený blok dává vanilkové zkušenosti.",
                stat(StatId.MINING_SPEED, 0.02), stat(StatId.MINING_BLOCK_EXPERIENCE, 0.4)),
            node("tempo", "Trpasličí nadání", "Dále zrychluje těžbu, posiluje zkušenosti a odemyká Drilling.",
                stat(StatId.MINING_SPEED, 0.04), stat(StatId.MINING_BLOCK_EXPERIENCE, 0.8),
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
                stat(StatId.WOODCUTTING_SPEED, 0.02), stat(StatId.WOODCUTTING_LOG_EXPERIENCE, 0.4)),
            node("tempo", "Jistý zásek", "Arborista zrychluje kácení a růst zasazených saplingů.",
                stat(StatId.WOODCUTTING_SPEED, 0.04), stat(StatId.SAPLING_GROWTH_MULTIPLIER, 0.20)),
            node("recipes", "Aktivní život", "Jídlo lépe zasytí, hlad ubývá pomaleji a zdraví se rychleji obnovuje.",
                stat(StatId.FOOD_SATURATION_MULTIPLIER, 0.30), stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.20),
                stat(StatId.HEALTH_REGENERATION, 0.25)),
            node("feller", "Pád velikána", "Při plížení porazí propojený přírodní strom.", mechanic(MechanicId.TREE_FELLER)),
            node("leaves", "Zlaté listí", "Listí může skrýt zlaté jablko a vydává vanilkové zkušenosti.",
                mechanic(MechanicId.RARE_LEAF_DROPS), mechanic(MechanicId.GOLDEN_LEAVES),
                stat(StatId.GOLDEN_LEAF_APPLE_CHANCE, 0.0025), stat(StatId.WOODCUTTING_LEAF_EXPERIENCE, 2.0)),
            node("triple", "Křišťálové listí", "Zvyšuje šanci Zlatého listí a sekera listí ničí okamžitě.",
                mechanic(MechanicId.INSTANT_LEAF_BREAK), stat(StatId.GOLDEN_LEAF_APPLE_CHANCE, 0.0025)));
        builder.tree(SkillId.DIGGING,
            node("yield", "Kopáč", "Zrychluje práci lopatou a za vykopaný blok dává vanilkové zkušenosti.",
                stat(StatId.DIGGING_SPEED, 0.06), stat(StatId.DIGGING_BLOCK_EXPERIENCE, 0.4)),
            node("tempo", "Bagr", "Dále zrychluje kopání hlíny, písku a štěrku.", stat(StatId.DIGGING_SPEED, 0.10)),
            node("finds", "Síto", "Odemkne běžné poklady z kopání a přidává vanilkové zkušenosti.",
                stat(StatId.RARE_DROP_CHANCE, 0.025), stat(StatId.DIGGING_BLOCK_EXPERIENCE, 2.0)),
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
                stat(StatId.CROP_GROWTH_MULTIPLIER, 0.05), stat(StatId.FARMING_HARVEST_EXPERIENCE, 0.4),
                stat(StatId.FOOD_COOKING_SPEED, 0.25)),
            node("husbandry", "Péče o stádo", "Zrychluje chov, posiluje řeznictví a rozmnožování zvířat.",
                stat(StatId.ANIMAL_GROWTH_MULTIPLIER, 0.60), stat(StatId.ANIMAL_BONUS_DROP_CHANCE, 0.30),
                stat(StatId.BREEDING_EXPERIENCE_MULTIPLIER, 1.0), stat(StatId.ANIMAL_DAMAGE_MULTIPLIER, 3.0)),
            node("instant", "Obratná sklizeň", "Pravým tlačítkem sklidí plodinu a ihned ji znovu zasadí.", mechanic(MechanicId.INSTANT_HARVEST)),
            node("triple", "Včelařova péče", "Včely neútočí a úl má šanci ihned obnovit med.",
                mechanic(MechanicId.BEEKEEPER), stat(StatId.BEEKEEPING_HONEY_REFILL_CHANCE, 0.50)),
            node("field", "Záběr pole", "Při plížení spustí dočasnou sklizeň a opětovné sázení v ploše 5×5.", mechanic(MechanicId.FIELD_HARVEST)));
        builder.tree(SkillId.FISHING,
            node("luck", "Čtení proudu", "Dává 1 bod globálního štěstí pro vzácné nálezy.", stat(StatId.LUCK, 1.0)),
            node("speed", "Napjatý vlasec", "Zkracuje čekání na záběr.", stat(StatId.FISHING_SPEED, 0.05)),
            node("wisdom", "Moudrost hlubin", "Úlovky vydají více zkušenostních koulí.", stat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.05)),
            node("equipment", "Ztracená výstroj", "Do úlovků přidává nalezenou výbavu.", mechanic(MechanicId.EQUIPMENT_FISHING)),
            node("salvage", "Druhý život", "Odemkne rozebrání vylovené výbavy.", mechanic(MechanicId.EQUIPMENT_SALVAGING)),
            node("master", "Pán tichých vod", "Spojuje nejvyšší rychlost, globální štěstí a výtěžek.", stat(StatId.LUCK, 1.0), stat(StatId.FISHING_SPEED, 0.20), stat(StatId.DOUBLE_DROP_CHANCE, 0.05)));
        builder.tree(SkillId.LIGHT_WEAPONS,
            node("damage", "Ostrá odpověď", "Zvyšuje poškození a šanci na krvácení.", stat(StatId.DAMAGE_MULTIPLIER, 0.03), stat(StatId.BLEED_CHANCE, 0.006)),
            node("critical", "Mezera v obraně", "Zvyšuje šanci a sílu kritického zásahu; odemyká volný pohyb se železnými a zlatými lehkými zbraněmi.", stat(StatId.CRITICAL_CHANCE, 0.012), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.04), mechanic(MechanicId.LIGHT_WEAPON_IRON_MOBILITY)),
            node("parry", "Včasný kryt", "Odemkne volný pohyb s diamantovými lehkými zbraněmi.", mechanic(MechanicId.LIGHT_WEAPON_DIAMOND_MOBILITY)),
            node("coating", "Jed na ostří", "Odemkne nanášení lektvarových účinků na zbraň.", mechanic(MechanicId.WEAPON_COATING)),
            node("immunity", "Pod kůži", "Prohlubuje krvácení, zvyšuje šanci na jeho obnovení a odemyká volný pohyb s netheritovými lehkými zbraněmi.", stat(StatId.BLEED_DAMAGE_MULTIPLIER, 0.20), stat(StatId.BLEED_CHANCE, 0.02), mechanic(MechanicId.LIGHT_WEAPON_NETHERITE_MOBILITY)),
            node("master", "Sto rychlých ran", "Vrchol stezky posiluje kritické zásahy i krvácení.", stat(StatId.CRITICAL_CHANCE, 0.08), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.20), stat(StatId.BLEED_CHANCE, 0.07), stat(StatId.BLEED_DAMAGE_MULTIPLIER, 0.25)));
        builder.tree(SkillId.HEAVY_WEAPONS,
            node("damage", "Váha rozsudku", "Zvyšuje poškození těžkých zbraní.", stat(StatId.DAMAGE_MULTIPLIER, 0.035)),
            node("power", "Drtivý nápřah", "Zesiluje silové útoky a odemyká volný pohyb se železnými a zlatými těžkými zbraněmi.", stat(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER, 0.05), mechanic(MechanicId.HEAVY_WEAPON_IRON_MOBILITY)),
            node("critical", "Prasklá obrana", "Zvyšuje šanci a sílu kritického zásahu; odemyká volný pohyb s diamantovými těžkými zbraněmi.", stat(StatId.CRITICAL_CHANCE, 0.01), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.05), mechanic(MechanicId.HEAVY_WEAPON_DIAMOND_MOBILITY)),
            node("penetration", "Průlom plátu", "Část úderu pronikne zbrojí.", stat(StatId.ARMOR_PENETRATION, 0.015)),
            node("coating", "Nános zkázy", "Odemkne nanášení lektvarových účinků na zbraň a volný pohyb s netheritovými těžkými zbraněmi.", mechanic(MechanicId.WEAPON_COATING), mechanic(MechanicId.HEAVY_WEAPON_NETHERITE_MOBILITY)),
            node("master", "Otřes země", "Vrchol stezky spojuje průraznost a kritickou sílu.", stat(StatId.ARMOR_PENETRATION, 0.08), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.25)));
        builder.tree(SkillId.ARCHERY,
            node("damage", "Pevná tětiva", "Zvyšuje poškození luků a kuší.", stat(StatId.DAMAGE_MULTIPLIER, 0.03)),
            node("accuracy", "Klidný dech", "Zlepšuje přesnost střelby.", stat(StatId.ACCURACY, 0.025)),
            node("critical", "Smrtící úhel", "Zvyšuje šanci a sílu kritického zásahu.", stat(StatId.CRITICAL_CHANCE, 0.012), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.04)),
            node("arrows", "Šípařova brašna", "Odemkne Šíp průzkumníka, který označí zasažený cíl.", mechanic(MechanicId.CUSTOM_ARROW_RECIPES), stat(StatId.AMMO_CONSUMPTION_REDUCTION, 0.02)),
            node("charged", "Zadržený výstřel", "Odemkne nabité výstřely s volitelným posílením.", mechanic(MechanicId.CHARGED_SHOT)),
            node("master", "Oko bouře", "Vrchol stezky posiluje přesnost i kritický zásah.", stat(StatId.ACCURACY, 0.12), stat(StatId.CRITICAL_CHANCE, 0.08)));
        builder.tree(SkillId.LIGHT_ARMOR,
            node("armor", "Pružná ochrana", "Zvyšuje ochranu lehké výstroje.", stat(StatId.ARMOR_MULTIPLIER, 0.04)),
            node("mobility", "Beztížný krok", "Odemyká volný pohyb v chainmailové lehké výstroji.", mechanic(MechanicId.LIGHT_ARMOR_CHAINMAIL_MOBILITY)),
            node("dodge", "Prázdné místo", "Zvyšuje šanci zcela uniknout zásahu.", stat(StatId.DODGE_CHANCE, 0.012)),
            node("sustenance", "Dlouhý dech", "Snižuje spotřebu hladu při pohybu a boji; odemyká volný pohyb v diamantové lehké výstroji.", stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.015), mechanic(MechanicId.LIGHT_ARMOR_SET_BONUS), mechanic(MechanicId.LIGHT_ARMOR_DIAMOND_MOBILITY)),
            node("adrenaline", "Poslední únik", "Odemkne Adrenalin při nízkém zdraví.", mechanic(MechanicId.ADRENALINE)),
            node("master", "Stín mezi čepelemi", "Vrchol stezky spojuje úhyb a ochranu.", stat(StatId.DODGE_CHANCE, 0.08), stat(StatId.ARMOR_MULTIPLIER, 0.15)));
        builder.tree(SkillId.HEAVY_ARMOR,
            node("armor", "Vrstvený plát", "Zvyšuje ochranu těžké výstroje.", stat(StatId.ARMOR_MULTIPLIER, 0.05)),
            node("burden", "Nesené břemeno", "Odemyká volný pohyb v železné a zlaté těžké výstroji.", mechanic(MechanicId.HEAVY_ARMOR_IRON_MOBILITY)),
            node("reflection", "Odplata oceli", "Část přijatého poškození se vrací útočníkovi.", stat(StatId.DAMAGE_REFLECTION, 0.012)),
            node("recovery", "Nezlomný dech", "Zlepšuje obnovu zdraví a bonus celé sady; odemyká volný pohyb v netheritové těžké výstroji.", stat(StatId.HEALTH_REGENERATION, 0.04), mechanic(MechanicId.HEAVY_ARMOR_SET_BONUS), mechanic(MechanicId.HEAVY_ARMOR_NETHERITE_MOBILITY)),
            node("rage", "Hněv pod plátem", "Odemkne Hněv při nízkém zdraví.", mechanic(MechanicId.RAGE)),
            node("master", "Kráčející hradba", "Vrchol stezky spojuje ochranu a odplatu.", stat(StatId.ARMOR_MULTIPLIER, 0.20), stat(StatId.DAMAGE_REFLECTION, 0.06)));
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
