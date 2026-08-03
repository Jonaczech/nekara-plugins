# Roadmap Nekara Plugins

Tento dokument určuje schválené pořadí další práce. Dlouhodobé technické smlouvy
zůstávají v `PROJECT_MEMORY.md` a aktuální release stav v `HANDOFF.md`.

## Aktuální stav

- Nejnovější stabilní release: **NekaraRPG 1.5.1** s modulem **NekaraMounts** (`mounts`).
- Lokální kandidát **1.6.0** sjednocuje hráčský vstup do dynamického GUI
  `/nekararpg` a přidává změnu hesla v NekaraAuth; před vydáním vyžaduje živou akceptaci.
- Modul zůstane součástí jediného `NekaraRPG.jar` a bude samostatně zapínatelný.

## NekaraMounts — první vydání

Cílem první verze je jeden důvěryhodný, trvalý vanilla kůň na hráče. Mount nemá
být jednorázový teleport ani nový bojový systém; má působit jako skutečný společník,
jehož stav přežije odvolání, restart i změnu chunku.

### Rozsah MVP

- modul ID `mounts` a přepínač `modules.mounts.enabled` v kořenovém `config.yml`,
- vlastní `mounts/config.yml`,
- nejvýše jeden vlastněný vanilla `HORSE` na hráče,
- virtuální vytvoření přes GUI s výběrem barvy a jména a jednoznačné vlastnictví,
- svázaná píšťalka, přivolání k místu hráče, odvolání a správa přes GUI,
- trvalé jméno, zdraví, maximální zdraví, rychlost, výška skoku, barva, styl,
  sedlo, koňské brnění, vlastník, stav smrti a cooldown,
- nejvýše jedna aktivní entita stejného mounta na celém serveru,
- bezpečné obnovení po restartu, odpojení hráče a načtení či odložení chunku,
- lokální YAML úložiště za samostatným repository rozhraním, aby šlo později
  přejít na databázi bez přepsání herní logiky,
- typovaná konfigurace, české zprávy, unit testy a ruční Purpur akceptace.

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

### Záměrně mimo první verzi

- nákladní inventář,
- létající, vodní nebo bojoví mounti,
- hoglin nebo vlastní resource-pack model,
- breeding a obchodování mountů mezi hráči,
- globální tržiště nebo webový obchod,
- více současně vlastněných mountů,
- automatické nahrazování systémů CMI, Lands, ValhallaMMO nebo MythicMobs.

### Uzavřená rozhodnutí první implementace

1. Kůň vzniká virtuální evidencí přes hráčské GUI; ochočování neexistuje. Admin grant
   otevře tentýž výběr cílovému online hráči a později jej může nahradit quest.
2. Smrt používá perzistentní minutový cooldown bez ceny a bez trvalé ztráty.
3. Píšťalka má 30sekundový perzistentní cooldown. Odvolaný kůň vznikne 7-12 bloků
   daleko a doběhne na místo písknutí; aktivní entita se přesměruje bez teleportu.
   Píšťalka je svázaná s hráčem, nedropuje a správa udržuje jedinou nalezenou kopii.
4. PvP blokace používá vlastní jednoduché combat okno ukládané na disk.
5. Vlastník se váže na normalizovaný nick chráněný NekaraAuth; poslední známé UUID
   se ukládá jako doplňkový údaj a storage zůstává za repository rozhraním.

### Akceptační minimum

- Stejný mount se po odvolání a restartu vrátí se stejným zdravím, atributy,
  jménem, sedlem a brněním.
- Opakované nebo souběžné přivolání nikdy nevytvoří druhou entitu.
- PvP blokace funguje pro přivolání i odvolání a nelze ji obejít reconnectem.
- Smrt a cooldown přežijí restart serveru.
- Jiný hráč nemůže změnit vlastnictví ani získat uložené vybavení.
- Vypnutí `modules.mounts.enabled` odstraní runtime stav bez ztráty persistence.
- Upgrade vytvoří `mounts/config.yml` bez změny ostatních modulových configů.

## Pozdější rozšíření NekaraMounts

Po stabilizaci MVP lze zvážit questové získání, stáje, kosmetiku, důvěru mounta,
krmení, více plemen a nákup statistik za economy. Každé rozšíření musí
nejdřív zachovat jednoznačné vlastnictví a ochranu proti duplikaci.

## Další backlog po NekaraMounts

1. `lockpicking` — interaktivní zámky pouze na pluginem označených objektech.
2. `world-events` a `rumors` — objevování krátkých událostí bez přesných quest šipek.
3. `wounds` — vzácná a čitelná zranění propojená s Campfire a léčivy.
4. `foraging` — sběr bylin podle biomu, počasí a denní doby.
5. `reputation` — reakce osad a frakcí, služby, ceny a pozdější webové propojení.

Tyto položky nejsou schválené k implementaci, dokud se výslovně nestanou novou
prioritou.
