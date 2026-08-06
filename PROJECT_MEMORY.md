# Projektová paměť Nekara Plugins

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
