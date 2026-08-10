# Projektová paměť Nekara Plugins

## Release 2.8.0

- `DragonFlightCollisionPolicy` je čistá policy pro trasu draka. Vyžaduje průchodný koridor 5×5×5, otevřenou oblohu nad cílem i cestou a respektuje maximální výšku; `DragonsModule` nesmí teleportovat nosič přes zamítnutou trasu.
- `SmithingTier.ProcessingState` zachovává původní uložená ID. Nový `HAMMERED` má samostatné ID, aby dřívější `TEMPERED` a `SHARPENED` předměty zůstaly kompatibilní. Kovadlina vyžaduje setrvání, Shift, stejný předmět a vzdálenost do 3 bloků.
- Chainmail recipe registry používá stabilní `nekararpg:armor/chainmail_*` klíče; chainmail je lehká zbroj s materiálovou i uživatelskou bránou na levelu 20.
- `LIGHT_ARMOR_MOVEMENT_SPEED` je samostatný atributový modifier. Aplikuje se jen při aktivním lehkém setovém bonusu, bezpečně se odstraňuje při refreshi a nesmí nahrazovat nebo přepisovat Adrenalin.
## Release 2.7.0

- `DragonsModule` je server-only létající mount pro Power milník `dragon_bond` na
  úrovni 100. Neviditelný `HappyGhast` drží jezdce a synchronizovaný `EnderDragon`
  je pouze neškodný vizuál: bez kolize, damage, explozí, blokových změn a bossbaru.
- `ActiveMountCoordinator` je společná autorita koně a draka. Volání jednoho vždy
  bezpečně odvolá druhý; nesmí vzniknout dva aktivní mounti téhož hráče. Sdílená
  píšťalka podle milníku otevírá volbu, jinak dál volá koně přímo.
- Draka pohání vstup jezdce na serveru. `WASD` pohybuje, mezerník stoupá, Shift
  klesá a `F` sesedá. Nezobrazuj nosičův harness klientovi ani neobnovuj ghastí
  zvuky; jezdec dostává pouze střídmé ender-dračí zvuky.
- `HeroAuraService` je stále čistě vizuální odměna Power 200. Zlatá je pouze
  akcent: občasná jiskra, úsporný puls a stopa při skutečném pohybu/letu. Nezaváděj
  světelný sloup, permanentní zvuk, statistiky, XP ani combat bonus.
- Vlastní zbraně zachovávají PDC identitu i po úpravě katalogu díky legacy dřevěným
  definicím. Diamantové custom zbraně se mohou povýšit na netheritové ve smithing
  table. `ArmorProtectionResolver` aplikuje dodatkovou typovou ochranu až po
  vanilla ochraně; penetrace ji maximálně částečně snižuje a nikdy nezvýší škodu
  neobrněného cíle.

## Release 2.6.0

- Dobrovolné plazení, ležení na zemi, jejich příkazy i položky `/nrpg` byly odstraněny:
  čistě serverový plugin nemůže stabilně vynutit klientskou plazivou nebo ležící pózu.
  Nezasahuj do nativního Minecraft plazení pod překážkou ani do spaní v posteli.
- `HeroAuraService` je čistě vizuální odměna za Power 200. Používá decentní bílooranžové
  částice; nesmí přidávat statistiky ani další XP.
- `GatheringDropChanceMath` dává tooltipům stejnou kombinovanou šanci na `Dvojitý drop`,
  jakou používá runtime včetně samostatného New Game+ hodu. `SupplementalVanillaExperience`
  sčítá malé zlomkové odměny bez časového stropu; přirozené vanilla XP nemění.
- `FarmingDropPolicy` drží explicitní seznam plodin, rostlin a střihatelného porostu pro
  pasivní drop Statkářství. Aktivní sklizeň i Záběr pole nadále vyžadují motyku.
- `TimedAbilityWindow` řídí ruční Pád velikána: Shift + pravé tlačítko se sekerou otevře
  desetisekundové okno, po němž následuje 12sekundový cooldown. `MountRecallPolicy`
  rozhoduje, zda se aktivní kůň vzdálený alespoň tři chunky při zavolání teleportuje.

## Release 2.5.1

- Plazení je serverově autoritativní, nemá permission gate a může se zapnout příkazem,
  z `/nrpg` nebo klientskou klávesou `C`. Shift jej ruší; dočasný bonus step height
  umožňuje přejít jeden blok vysokou překážku bez skoku.
- Ležící hráč používá native mannequin jako vizuál a ne vynucenou `Player` `SLEEPING`
  pózu, protože tu klient může při změně kamery přepsat. `yaw-offset-degrees: 0.0`
  znamená nohy vpřed a hlavu za hráčem; dosavadní výchozí `-90.0` se migruje při startu.
- `PerkIconResolver` musí zachovat unikátní materiál pro každé stabilní perk ID.

## Release 2.4.2

- `SmithingTier` drží pět kvalit: Běžná, Neobyčejná, Vzácná, Epická a Legendární.
  Po perku `smithing.craft` je minimum Neobyčejná; kvalita je nezávislá na levelové
  bráně materiálu.
- Kovová výbava používá stav dokončení výkovu: pec, kotlík a u zbraní broušení.
  Neobcházej ho pouze GUI; dokončení a výsledná kvalita jsou server-authoritative.
- Globální `LuckConfig` má `craftingQualityChanceBonusPerPoint`. Výchozí `0.05`
  přidává šanci na povýšení kvality Řemesla, nikoliv další loot nebo XP.
- `SkillsMenu` u vybrané dovednosti zobrazuje kompaktní skutečné bonusy z runtime
  stavu a aktivní ranky. Hlavní přehled používá titul `Dovednosti`; spodní řada
  obsahuje Řemeslo, Statkářství a Rybaření.
- `docs/SKILL_ICON_BRIEFS.md` je hráčsky stručný podklad pro budoucí generování
  originálních ikon; žádné nové bitmapové ikony zatím nepřidávej.

## Release 2.4.1

- V hlavním menu je vstup do Dovedností `WRITABLE_BOOK` ve středovém slotu. Tooltip
  dovednosti neukazuje celkové XP: obsahuje úroveň, stávající progress bar, zbývající
  XP a odkaz na perky; při capu jen `100/100 • MAX`.
- Těžba, Lesnictví a Kopání při běžném rozbití nevyžadují správný nástroj pro XP,
  pasivní násobení dropů ani vzácný nález. Aktivní Vein Mining a Tree Feller zůstávají
  vázané na krumpáč a sekeru.
- Pasivní sklizňové dropy Statkářství nejsou vázané na motyku. Aktivní Obratná sklizeň
  a Field Harvest motyku vyžadují; XP za rozbíjení a foraging je nadále vázané na motyku.
- Výchozí `skills/kopani/loot-tables.yml` obsahuje globální fallback a tabulky podle
  zdrojového bloku. Při upgradu se existující soubor nepřepisuje, proto je nutné
  konfiguraci na serveru sloučit ručně.

## Architektura

- `NekaraRPG` je modulární Purpur/Paper plugin pro Minecraft 26.1 a Java 25.
- Nativní `skills` jsou jedinou autoritou RPG XP, perků a Power. Nevracej externí
  skill bridge ani paralelní XP pipeline.
- Rybaření musí zachovat serverem vytvořený vanilla úlovek a XP; minihra pouze
  řídí okamžik vrácení reálného catchu.
- Perk nákup je server-authoritative přes SQLite optimistic revision. GUI nikdy
  není autoritou pro rank, cenu, podmínky ani body.
- Custom itemy používají stabilní namespaced ID a statistiky řeší server.

## Perk-tree pravidla

- Pětihodnostní uzel drží explicitní level pro každou další hodnost. Kořenové
  uzly používají `0/10/20/35/50`, jejich navazující pětihodnostní uzly
  `20/35/50/70/85`. `PerkPurchasePolicy` musí ověřovat požadavek právě
  kupované hodnosti, nikoliv pouze minimální level uzlu.
- Aktivních je 13 dovedností. `MARTIAL_ARTS` a `TRADING` jsou zachované pouze
  kvůli profilu/datové kompatibilitě a nesmí se zobrazovat ani získávat XP.
- Každá aktivní dovednost má vlastní tematický layout; rozestupy cest jsou 2–4
  buněk a New Game+ je vedle startovního perku.
- Cesty: šedá zamčená, bílá dostupná, zelená odemčená.
- Osm navigačních šipek je mimo viditelný graf a vždy má klikací prioritu.
- Tooltip ukazuje u mechanik jediný konkrétní účinek; u statových perků doplňuje
  přesnou hodnotu krátkým popisem.
- Hlavní úroveň používá hráčskou hlavu. Milník Tábořiště/Rested je automatický na
  Power 1, milník Můj kůň na Power 25; položka mounta se před jeho odemčením nezobrazuje.
- Všechny návraty GUI používají `skills/tree/button_return_to_menu` přes `GuiItems.back`.

## Gathering a New Game+

- Těžba, Lesnictví, Kopání, Statkářství a Rybaření mají `0,20 %` přirozeného
  double dropu za úroveň, nejvýše `20 %` na levelu 100.
- Lesnictví násobí pouze logy a stemy; Rybaření kopíruje pouze skutečný vanilla catch,
  nikoliv treasure nebo Luck loot.
- Nová hra+ má XP multiplikátor `0.50`, všechny perk statistiky posiluje o `25 %`
  za rank a přirozený gathering double drop násobí `1.25` za rank. Statkářství
  navíc po aktivaci Nové hry+ získává samostatný trvalý `30 %` roll dodatečných
  dropů ze sklizně a ze zvířat zabitých hráčem.

## Statkářství – pracovní změna před releasem

- Strom stále používá šest uzlů se stabilními ID: `yield` Plná ošatka, `growth`
  Živá půda, `husbandry` Péče o stádo, `instant` Obratná sklizeň, `triple`
  Včelařova péče a `field` Záběr pole.
- Dodatečný výtěžek Plné ošatky i NG+ je samostatný roll vedle capované běžné
  šance; nepřičítá se tedy do jediné šance nad `100 %`.

## Lesnictví – pracovní změna před releasem

- Stabilní uzly `yield`, `tempo`, `recipes`, `feller`, `leaves` a `triple` nyní
  představují Mízu lesa, Arboristu, Aktivní život, Pád velikána, Zlaté listí a
  Křišťálové listí. Starý recept na pět prken byl zrušen.
- Zlaté jablko z listí je samostatný nízký roll bez globálního Lucku. Křišťálové
  listí šanci zdvojnásobuje a sekera s ním ničí listí okamžitě se zachováním dropů.
- Nová hra+ Lesnictví má vlastní trvalý `0.30` roll dalšího finálního dropu pouze
  pro přirozené logy a stemy; Tree Feller ani listí jej nespouští.

## Kopání – pracovní změna před releasem

- Stabilní uzly `yield`, `tempo`, `finds`, `archaeology`, `deep_soil` a `triple`
  znamenají Kopáče, Bagr, Síto, Archeologa, Replikaci zeminy a Skrytý poklad.
- Archeologova obnova suspicious sand/gravel je jednorázově rozhodnutá při začátku
  čištění a blok se obnoví pouze tehdy, když se po dokončení skutečně změnil na
  odpovídající sand/gravel. Během vypnutí modulu nezůstává žádná čekající obnova.
- Replikace zeminy má pouze dva explicitní recepty: `4 dirt + moss block + bone
  meal → 4 grass block` a `4 dirt + hanging roots + bone meal → 4 rooted dirt`.
  NG+ Kopání používá nezávislý `0.30` roll další kopie finálních dropů.
- `skills/kopani/loot-tables.yml` drží globální fallback a tabulky specifické pro
  zdrojový blok. Při existenci specifické tabulky se globální položky nemíchají;
  archeologické Brick/Echo Shard se přidávají pouze do písku a štěrku.

## Resource pack a release

- Textury a modely NekaraRPG patří do
  [`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack).
  Plugin obsahuje jen jejich ID a vanilla fallbacky.
- Build prováděj `NekaraRPG/scripts/build-release.cmd`.
- Před FTP výměnou vždy vyžádej potvrzení, že server neběží, ověř zálohu a hash
  finálně staženého souboru. GitHub release a živý server jsou nezávislé stavy.
