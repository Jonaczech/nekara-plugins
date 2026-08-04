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
    private static final PerkPosition ROOT = new PerkPosition(10, 2);
    private static final PerkPosition LEFT = new PerkPosition(5, 7);
    private static final PerkPosition RIGHT = new PerkPosition(15, 7);
    private static final PerkPosition LEFT_DEEP = new PerkPosition(3, 14);
    private static final PerkPosition RIGHT_DEEP = new PerkPosition(17, 14);
    private static final PerkPosition CROWN = new PerkPosition(10, 15);

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
            node("yield", "Hlas kamene", "Zvyšuje šanci na násobný výtěžek.", stat(StatId.DOUBLE_DROP_CHANCE, 0.025)),
            node("tempo", "Rytmus krumpáče", "Zrychluje dobývání kamene a rud.", stat(StatId.MINING_SPEED, 0.05)),
            node("furnace", "Žhavá směna", "Pece otevřené horníkem pracují rychleji.", stat(StatId.FURNACE_SPEED, 0.08)),
            node("vein", "Žilobití", "Při plížení vytěží propojenou rudnou žílu.", mechanic(MechanicId.VEIN_MINING)),
            node("blast", "Řízený odstřel", "Bezpečně posílí vlastní TNT s omezeným záběrem.", stat(StatId.TNT_POWER, 0.08), mechanic(MechanicId.DRILLING)),
            node("triple", "Srdce hory", "Přidává šanci na trojitý výtěžek.", stat(StatId.TRIPLE_DROP_CHANCE, 0.08)));
        builder.tree(SkillId.WOODCUTTING,
            node("yield", "Míza lesa", "Zvyšuje šanci na násobný výtěžek dřeva.", stat(StatId.DOUBLE_DROP_CHANCE, 0.025)),
            node("tempo", "Jistý zásek", "Zrychluje práci se sekerou.", stat(StatId.WOODCUTTING_SPEED, 0.05)),
            node("recipes", "Úsporné trámy", "Recept z jednoho kmene vydá pět prken místo čtyř.", mechanic(MechanicId.WOOD_RECIPES)),
            node("feller", "Pád velikána", "Při plížení porazí propojený přírodní strom.", mechanic(MechanicId.TREE_FELLER)),
            node("leaves", "Koruna tajemství", "Listí může skrývat vzácnou odměnu.", mechanic(MechanicId.RARE_LEAF_DROPS), stat(StatId.RARE_DROP_CHANCE, 0.015)),
            node("triple", "Dědictví hvozdu", "Přidává šanci na trojitý výtěžek.", stat(StatId.TRIPLE_DROP_CHANCE, 0.08)));
        builder.tree(SkillId.DIGGING,
            node("yield", "Úrodná zem", "Zvyšuje šanci na násobný výtěžek zeminy.", stat(StatId.DOUBLE_DROP_CHANCE, 0.025)),
            node("tempo", "Lehká lopata", "Zrychluje kopání hlíny, písku a štěrku.", stat(StatId.DIGGING_SPEED, 0.05)),
            node("finds", "Třpyt v prachu", "Při kopání lze nalézt vzácné suroviny.", stat(StatId.RARE_DROP_CHANCE, 0.012)),
            node("archaeology", "Paměť střepů", "Rozšiřuje možné archeologické nálezy.", mechanic(MechanicId.ARCHAEOLOGY_FINDS)),
            node("deep_soil", "Hluboká vrstva", "Dále posiluje výtěžek a rychlost práce.", stat(StatId.DOUBLE_DROP_CHANCE, 0.04), stat(StatId.DIGGING_SPEED, 0.08)),
            node("triple", "Poklad pod nohama", "Přidává šanci na trojitý výtěžek.", stat(StatId.TRIPLE_DROP_CHANCE, 0.08)));
        builder.tree(SkillId.FARMING,
            node("yield", "Plná ošatka", "Zvyšuje šanci na násobnou sklizeň.", stat(StatId.DOUBLE_DROP_CHANCE, 0.025)),
            node("growth", "Živá půda", "Plodiny v péči hráče rostou rychleji.", stat(StatId.CROP_GROWTH_MULTIPLIER, 0.05)),
            node("husbandry", "Péče o stádo", "Zvyšuje výtěžek chovu a včelaření.", stat(StatId.ANIMAL_DAMAGE_MULTIPLIER, 0.04), stat(StatId.BEEKEEPING_YIELD, 0.05)),
            node("instant", "Obratná sklizeň", "Odemkne sklizeň a nové zasazení jedním dotykem.", mechanic(MechanicId.INSTANT_HARVEST)),
            node("field", "Záběr pole", "Odemkne sklizeň sousedících zralých plodin.", mechanic(MechanicId.FIELD_HARVEST), stat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.05)),
            node("triple", "Roh hojnosti", "Přidává šanci na trojitý výtěžek.", stat(StatId.TRIPLE_DROP_CHANCE, 0.08)));
        builder.tree(SkillId.FISHING,
            node("luck", "Čtení proudu", "Zvyšuje štěstí při rybolovu.", stat(StatId.FISHING_LUCK, 0.08)),
            node("speed", "Napjatý vlasec", "Zkracuje čekání na záběr.", stat(StatId.FISHING_SPEED, 0.05)),
            node("wisdom", "Moudrost hlubin", "Úlovky vydají více zkušenostních koulí.", stat(StatId.EXPERIENCE_ORB_MULTIPLIER, 0.05)),
            node("equipment", "Ztracená výstroj", "Do úlovků přidává nalezenou výbavu.", mechanic(MechanicId.EQUIPMENT_FISHING)),
            node("salvage", "Druhý život", "Odemkne rozebrání vylovené výbavy.", mechanic(MechanicId.EQUIPMENT_SALVAGING)),
            node("master", "Pán tichých vod", "Spojuje nejvyšší rychlost a štěstí rybáře.", stat(StatId.FISHING_LUCK, 0.25), stat(StatId.FISHING_SPEED, 0.20)));
        builder.tree(SkillId.LIGHT_WEAPONS,
            node("damage", "Ostrá odpověď", "Zvyšuje poškození a šanci na krvácení.", stat(StatId.DAMAGE_MULTIPLIER, 0.03), stat(StatId.BLEED_CHANCE, 0.006)),
            node("critical", "Mezera v obraně", "Zvyšuje šanci a sílu kritického zásahu.", stat(StatId.CRITICAL_CHANCE, 0.012), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.04)),
            node("parry", "Včasný kryt", "Odemkne odražení útoku a krátké omráčení útočníka.", mechanic(MechanicId.PARRY)),
            node("coating", "Jed na ostří", "Odemkne nanášení lektvarových účinků na zbraň.", mechanic(MechanicId.WEAPON_COATING)),
            node("immunity", "Pod kůži", "Prohlubuje krvácení a zvyšuje šanci na jeho obnovení.", stat(StatId.BLEED_DAMAGE_MULTIPLIER, 0.20), stat(StatId.BLEED_CHANCE, 0.02)),
            node("master", "Sto rychlých ran", "Vrchol stezky posiluje kritické zásahy i krvácení.", stat(StatId.CRITICAL_CHANCE, 0.08), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.20), stat(StatId.BLEED_CHANCE, 0.07), stat(StatId.BLEED_DAMAGE_MULTIPLIER, 0.25)));
        builder.tree(SkillId.HEAVY_WEAPONS,
            node("damage", "Váha rozsudku", "Zvyšuje poškození těžkých zbraní.", stat(StatId.DAMAGE_MULTIPLIER, 0.035)),
            node("power", "Drtivý nápřah", "Zesiluje silové útoky.", stat(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER, 0.05)),
            node("critical", "Prasklá obrana", "Zvyšuje šanci a sílu kritického zásahu.", stat(StatId.CRITICAL_CHANCE, 0.01), stat(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.05)),
            node("penetration", "Průlom plátu", "Část úderu pronikne zbrojí.", stat(StatId.ARMOR_PENETRATION, 0.015)),
            node("coating", "Nános zkázy", "Odemkne nanášení lektvarových účinků na zbraň.", mechanic(MechanicId.WEAPON_COATING)),
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
            node("mobility", "Beztížný krok", "Snižuje pohybový postih lehké výstroje.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.04)),
            node("dodge", "Prázdné místo", "Zvyšuje šanci zcela uniknout zásahu.", stat(StatId.DODGE_CHANCE, 0.012)),
            node("sustenance", "Dlouhý dech", "Snižuje spotřebu hladu při pohybu a boji.", stat(StatId.HUNGER_CONSUMPTION_REDUCTION, 0.015), mechanic(MechanicId.LIGHT_ARMOR_SET_BONUS)),
            node("adrenaline", "Poslední únik", "Odemkne Adrenalin při nízkém zdraví.", mechanic(MechanicId.ADRENALINE)),
            node("master", "Stín mezi čepelemi", "Vrchol stezky spojuje úhyb a ochranu.", stat(StatId.DODGE_CHANCE, 0.08), stat(StatId.ARMOR_MULTIPLIER, 0.15)));
        builder.tree(SkillId.HEAVY_ARMOR,
            node("armor", "Vrstvený plát", "Zvyšuje ochranu těžké výstroje.", stat(StatId.ARMOR_MULTIPLIER, 0.05)),
            node("burden", "Nesené břemeno", "Snižuje pohybový postih těžké výstroje.", stat(StatId.MOVEMENT_PENALTY_REDUCTION, 0.035)),
            node("reflection", "Odplata oceli", "Část přijatého poškození se vrací útočníkovi.", stat(StatId.DAMAGE_REFLECTION, 0.012)),
            node("recovery", "Nezlomný dech", "Zlepšuje obnovu zdraví a bonus celé sady.", stat(StatId.HEALTH_REGENERATION, 0.04), mechanic(MechanicId.HEAVY_ARMOR_SET_BONUS)),
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
            PerkId rootId = add(skill, root, 5, 1, 0, Set.of(), ROOT);
            PerkId leftId = add(skill, left, 5, 1, 20, Set.of(new PerkRequirement(rootId, 2)), LEFT);
            PerkId rightId = add(skill, right, 1, 2, 20, Set.of(new PerkRequirement(rootId, 2)), RIGHT);
            PerkId leftDeepId = add(skill, leftDeep, 1, 3, 50, Set.of(new PerkRequirement(leftId, 3)), LEFT_DEEP);
            PerkId rightDeepId = add(skill, rightDeep, 1, 3, 50, Set.of(new PerkRequirement(rightId, 1)), RIGHT_DEEP);
            add(skill, crown, 1, 5, 100, Set.of(
                new PerkRequirement(leftDeepId, 1),
                new PerkRequirement(rightDeepId, 1)
            ), CROWN);
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
