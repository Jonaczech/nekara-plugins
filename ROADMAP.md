# Roadmap Nekara Plugins

Tento dokument určuje schválené pořadí další práce. Dlouhodobé technické smlouvy
zůstávají v `PROJECT_MEMORY.md` a aktuální release stav v `HANDOFF.md`.

## Aktuální stav

- Nejnovější stabilní release: **NekaraRPG 2.0.0** s vypnutým základem Nekara
  Skills, opravou výbavy koně, obrazovkou Činnosti a rozšířeným Tábořením.
- Release je dostupný updateru; migrace SQLite, brašny, pathfinding
  ještě vyžadují živou Purpur akceptaci.
- Modul zůstane součástí jediného `NekaraRPG.jar` a bude samostatně zapínatelný.

## Nekara Skills 2.0 — aktuální priorita

NekaraRPG se stává autoritativní RPG platformou se šestnácti dovednostmi,
levelováním do 100, odvozeným Powerlevelem, původními perk stromy, statistikami,
bojovými efekty a bezpečnými aktivními schopnostmi. Detailní fáze a blokátory
plné aktivace jsou v `NekaraRPG/docs/SKILLS_2_0_ROADMAP.md`; architekturu a
clean-room hranice určuje `NekaraRPG/docs/adr/0001-native-skills-platform.md`.

Aktuálně je hotové čisté doménové jádro, SQLite adaptér s optimistic revision,
český katalog 90 původních perků, detailní 54slotové stezky a potvrzený
transakční nákup za Power body. Vývojová verze je 2.1.0, ale nejnovějším
publikovaným releasem zůstává 2.0.0. Následuje první úplná XP vertikála a živé
provedení efektů jedné dovednosti; do té doby modul zůstává vypnutý.

### Bezprostřední další krok po GUI 2.1.0

1. Vybrat jednu dovednost jako úplnou vertikálu; doporučené je Hornictví, protože
   už existuje ochranná logika aktivních blokových schopností a Echo Vein.
2. Napojit důvěryhodné Paper eventy na `SkillExperienceService`, včetně původu
   bloku, hráčem položených zdrojů, chunk heat a deduplikace.
3. Složit zakoupené ranky přes stat engine a skutečně provést pouze efekty dané
   vertikály. Vein Mining a Drilling musí respektovat region, limit bloků,
   durability, cooldown a jediný odměnový průchod.
4. Doplnit administrátorský staging nástroj pro auditovatelný grant/reset XP a
   perků; nesmí být dostupný běžnému hráči ani obcházet stejnou doménovou policy.
5. Ověřit restart, souběžné XP, dvojklik, packet replay, položené bloky, Fortune,
   Silk Touch, custom itemy a kompatibilitu s ValhallaMMO bez dvojí odměny.

ValhallaMMO zůstane na produkčním serveru až do ověřené migrace a nebude se
odstraňovat ani paralelně odměňovat bez exportu, zálohy, rollback plánu a živé
akceptace. Release 2.0.0 proto vydává modul `skills` vypnutý a ponechává
ValhallaMMO jako autoritativní produkční systém.

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
