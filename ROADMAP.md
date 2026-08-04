# Roadmap Nekara Plugins

Tento dokument určuje schválené pořadí další práce. Dlouhodobé technické smlouvy
zůstávají v `PROJECT_MEMORY.md` a aktuální release stav v `HANDOFF.md`.

## Aktuální stav

- Nejnovější stabilní release: **NekaraRPG 2.2.0**. Nativní Nekara Skills jsou
  výchozí autoritou postupu a 2.2.0 opravuje mannequin ležení, zobrazení XP,
  výrobu Tierů i škálovaný výnos stavebních materiálů.
- Všech 15 trénovaných dovedností má chráněnou runtime vertikálu, rozdělené
  konfigurace, 90 původních perků a vlastní SQLite úložiště. Živá Purpur akceptace
  a vyvážení zůstávají povinnou součástí nasazení.
- Modul zůstane součástí jediného `NekaraRPG.jar` a bude samostatně zapínatelný.
- Release 2.2.0 opravuje živě nalezenou orientaci/duplicitní výbavu mannequin
  ležení, přidává export profilů a XP telemetrii, vypíná ve výchozím profilu
  starou ValhallaMMO Echo Vein aktivitu ve prospěch nativního Žilobití a zavádí
  nativní stonecutter recepty i bezpečně škálovaný crafting stavebních materiálů.

## Nekara Skills 2.0 — aktuální priorita

NekaraRPG se stává autoritativní RPG platformou se šestnácti dovednostmi,
levelováním do 100, odvozeným Powerlevelem, původními perk stromy, statistikami,
bojovými efekty a bezpečnými aktivními schopnostmi. Detailní fáze a blokátory
plné aktivace jsou v `NekaraRPG/docs/SKILLS_2_0_ROADMAP.md`; architekturu a
clean-room hranice určuje `NekaraRPG/docs/adr/0001-native-skills-platform.md`.

Aktuálně je hotové čisté doménové jádro, SQLite adaptér s optimistic revision,
český katalog 90 původních perků, detailní 54slotové stezky a potvrzený
transakční nákup za Power body. Všech 15 trénovaných dovedností zapisuje nativní
XP z validovaných událostí a provádí vlastní runtime efekty. Sběrné vertikály
sledují původ bloků, používají omezené grafové průchody a chrání vanilla i custom
  loot před druhým odměnovým průchodem. Auditovaná správa používá SQLite schéma v2
  a bezpečnou migraci profilů z v1. Release 2.2.0 je nasazený jako nativní
  výchozí profil; další iterace stále vyžadují živé testy a průběžné vyvažování.

### Bezprostřední další krok po release 2.2.0

1. Hotovo v kódu: všech 15 trénovaných dovedností používá validované eventové
   zdroje XP napojené na společný `SkillExperienceService`.
2. Hotovo v kódu: perzistentní původ hráčem položených bloků, chunk heat,
   deduplikace a výtěžkové staty nad skutečnými finálními dropy.
3. Hotovo v kódu: operátorský staging nástroj pro inspect, grant XP/perku a
   reset se společnou transakcí profilu a auditního záznamu.
4. Hotovo v kódu: Žilobití, Řízený odstřel a Pád velikána respektují regionové
   eventy, limit bloků, dávkování, durability, cooldown a jediný odměnový průchod;
   rychlost nástroje a pece nepřepisuje silnější externí efekt. Bojové efekty mají
   ochranu před syntetickou rekurzí, krvácení má centrální kapacitu a perk GUI
   zobrazuje ikonku podle dominantního efektu.
5. Živě ověřit restart, migraci schématu i rozdělených configů, souběžné XP/admin
   změny, dvojklik, mannequin ležení, položené bloky, Fortune, Silk Touch, custom
   itemy, aktivní schopnosti a kompatibilitu s ValhallaMMO bez druhého loot
   průchodu. Potom na kopii stagingu provést čistě nativní test všech 15 vertikál
   bez ValhallaMMO a měřit MSPT.
6. Hotovo v kandidátu 2.2.0: `/nrpg skills admin metrics` měří provoz omezené XP
   fronty a `/nrpg skills admin export` vytváří konzistentní SQLite+CSV snapshot
   s hashem bez ruční editace živé databáze. Zbývá mapovací vrstva pro ValhallaMMO
   profily a porovnávací report obou systémů.

Nekara Skills jsou od release 2.2.0 výchozí autoritou postupu. ValhallaMMO se
nenasazuje ani paralelně neodměňuje; starý modul `mining` zůstává jen jako vypnutá
kompatibilní Echo Vein volba. Před výměnou JARu vždy vytvoř export, ověř zálohu a
na živém Purpuru projdi XP, perk efekty, restart a MSPT.

## NekaraMounts — aktuální rozsah

Cílem první verze je jeden důvěryhodný, trvalý vanilla kůň na hráče. Mount nemá
být jednorázový teleport ani nový bojový systém; má působit jako skutečný společník,
jehož stav přežije odvolání, restart i změnu chunku.

### Implementovaný rozsah

- modul ID `mounts` a přepínač `modules.mounts.enabled` v kořenovém `config.yml`,
- vlastní `mounts/config.yml`,
- nejvýše jeden vlastněný vanilla `HORSE` na hráče,
- virtuální vytvoření přes GUI s výběrem barvy a jména a jednoznačné vlastnictví,
- svázaná píšťalka, přivolání k místu hráče, odvolání a správa přes GUI,
- trvalé jméno, zdraví, maximální zdraví, rychlost, výška skoku, barva, styl,
  sedlo, koňské brnění, vlastník, stav smrti a cooldown,
- nejvýše jedna aktivní entita stejného mounta na celém serveru,
- bezpečné obnovení po restartu, odpojení hráče a načtení či odložení chunku,
- transakční SQLite úložiště za samostatným repository rozhraním a jednorázová
  migrace starého YAML se zachovanou zálohou,
- výchozí sedlo, volitelná truhla a virtuální brašny o 54 slotech,
- přirozené putování v okolí místa volání a bezpečný nový pokus při zaseknutí,
- typovaná konfigurace, české zprávy, unit testy a ruční Purpur akceptace.
- nasazení truhly, sedla a koňského brnění jedním kliknutím ze spodního inventáře.

## Táboření — sjednocený odpočinek

- Sitting už není samostatný modul; jeho životní cyklus a konfiguraci vlastní Campfire.
- Rybaření, Táboření a Těžba jsou v centrálním GUI seskupené pod Činnosti.
- Tábořiště nabízí sezení, ležení a vstávání bez nutnosti samostatných příkazů.
- Ležení u zapáleného ohně nabíjí Rested stejně jako sezení.
- Přeskočení noci je možné jen jedinému online hráči v Overworldu a bez volání CMI.
- Správa NekaraRPG zůstává skrytá za oprávněním `nekararpg.command.status`
  s výchozí hodnotou `op`.

### Bezpečnostní pravidla

- Přivolání ani odvolání nesmí fungovat během PvP nebo nastavenou dobu po boji.
- Odvolání nesmí mounta léčit, měnit jeho atributy ani odstranit negativní stav.
- Smrt vytvoří trvalý cooldown; přihlášení, restart ani reload jej nesmí obejít.
- Plugin musí zabránit duplikaci entity, sedla i brnění při souběžném příkazu,
  smrti, unloadu chunku, teleportu, restartu a pádu storage.
- Při chybě persistence se operace odmítne bezpečně a existující entita se nesmí
  přepsat neúplným stavem.
- Cizí hráč nesmí mounta zranit, převzít, odvolat, přejmenovat ani měnit vybavení.
- Modul musí při vypnutí a reloadu uklidit tasky a bezpečně uložit vlastněný stav.
- Nevznikne hlavní příkaz `/mount`; všechny příkazy zůstanou pod `/nekararpg` a
  `/nrpg`, aby se předešlo konfliktům s jinými pluginy.

### Záměrně mimo aktuální rozsah

- létající, vodní nebo bojoví mounti,
- hoglin nebo vlastní resource-pack model,
- breeding a obchodování mountů mezi hráči,
- globální tržiště nebo webový obchod,
- více současně vlastněných mountů,
- automatické nahrazování systémů CMI, Lands, ValhallaMMO nebo MythicMobs.

### Uzavřená rozhodnutí první implementace

1. Kůň vzniká virtuální evidencí přes hráčské GUI; ochočování neexistuje. Admin grant
   otevře tentýž výběr cílovému online hráči. Questovou logiku vlastní BetonQuest.
2. Smrt používá perzistentní minutový cooldown bez ceny a bez trvalé ztráty.
3. Píšťalka má 30sekundový perzistentní cooldown. Odvolaný kůň vznikne 7-12 bloků
   daleko a doběhne na místo písknutí; aktivní entita se přesměruje bez teleportu
   a používá krátkou ochranu proti spamu. Po doběhnutí se pohybuje v omezeném okolí.
   Píšťalka je svázaná s hráčem a správa udržuje jedinou nalezenou kopii.
4. PvP blokace používá vlastní jednoduché combat okno ukládané na disk.
5. Vlastník se váže na normalizovaný nick chráněný NekaraAuth; poslední známé UUID
   se ukládá jako doplňkový údaj a storage zůstává za repository rozhraním.

### Akceptační minimum

- Stejný mount se po odvolání a restartu vrátí se stejným zdravím, atributy,
  jménem, sedlem, brněním a obsahem brašen.
- Opakované nebo souběžné přivolání nikdy nevytvoří druhou entitu.
- PvP blokace funguje pro přivolání i odvolání a nelze ji obejít reconnectem.
- Smrt a cooldown přežijí restart serveru.
- Jiný hráč nemůže změnit vlastnictví ani získat uložené vybavení.
- Vypnutí `modules.mounts.enabled` odstraní runtime stav bez ztráty persistence.
- Upgrade vytvoří `mounts/config.yml` bez změny ostatních modulových configů.
- Upgrade jednou převede starý `mounts/data.yml` do `mounts/data.db`, ponechá
  původní data a vytvoří zálohu bez opakovaného importu.

## Odložené rozšiřování

Nové moduly, hráčská nastavení a vlastní quest engine nejsou aktuální priorita.
Questy, dialogy a příběhové udělení odměn nadále vlastní BetonQuest. Vlastní skill
platforma je výslovně schválená priorita 2.0; economy zůstává pouze budoucím
adaptérem pro konkrétní perk či službu, nikoliv novým ekonomickým systémem.

## Pozastavený backlog

1. `lockpicking` — interaktivní zámky pouze na pluginem označených objektech.
2. `world-events` a `rumors` — objevování krátkých událostí bez přesných quest šipek.
3. `wounds` — vzácná a čitelná zranění propojená s Campfire a léčivy.
4. `foraging` — sběr bylin podle biomu, počasí a denní doby.
5. `reputation` — reakce osad a frakcí, služby, ceny a pozdější webové propojení.

Tyto položky se nyní neřeší a nejsou schválené k implementaci, dokud se výslovně
nestanou novou prioritou.
