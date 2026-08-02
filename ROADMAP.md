# Roadmap Nekara Plugins

Tento dokument určuje schválené pořadí další práce. Dlouhodobé technické smlouvy
zůstávají v `PROJECT_MEMORY.md` a aktuální release stav v `HANDOFF.md`.

## Aktuální stav

- Nejnovější stabilní release: **NekaraRPG 1.4.0**.
- Nejbližší nový herní modul: **NekaraMounts** (`mounts`).
- Modul zůstane součástí jediného `NekaraRPG.jar` a bude samostatně zapínatelný.

## NekaraMounts — první vydání

Cílem první verze je jeden důvěryhodný, trvalý vanilla kůň na hráče. Mount nemá
být jednorázový teleport ani nový bojový systém; má působit jako skutečný společník,
jehož stav přežije odvolání, restart i změnu chunku.

### Rozsah MVP

- modul ID `mounts` a přepínač `modules.mounts.enabled` v kořenovém `config.yml`,
- vlastní `mounts/config.yml`,
- nejvýše jeden vlastněný vanilla `HORSE` na hráče,
- řízené získání nebo registrace koně a jednoznačné vlastnictví,
- přivolání, odvolání a zobrazení stavu přes `/nekararpg mount ...`,
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
- Cizí hráč nesmí mounta převzít, odvolat, přejmenovat ani měnit jeho vybavení.
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

### Rozhodnutí před implementací

Před prvním kódem je potřeba uzavřít tyto herní volby:

1. Získání mounta: adopce ochočeného koně, stájové NPC, administrátorské přidělení,
   nebo kombinace těchto cest.
2. Smrt: pouze časový cooldown, cooldown s cenou za oživení, nebo trvalá ztráta.
3. Přivolání: povolené světy, maximální vzdálenost, potřeba volného prostoru a
   chování v regionech Lands.
4. PvP blokace: vlastní jednoduché combat okno, nebo integrace s konkrétním
   combat-tag pluginem používaným na serveru.
5. Vazba vlastníka: stabilní interní identita kompatibilní s NekaraAuth a pozdější
   databází/webem.

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

Po stabilizaci MVP lze zvážit stáje, kosmetiku, důvěru mounta, krmení, více plemen,
regionální pravidla a propojení s reputací nebo ekonomikou. Každé rozšíření musí
nejdřív zachovat jednoznačné vlastnictví a ochranu proti duplikaci.

## Další backlog po NekaraMounts

1. `lockpicking` — interaktivní zámky pouze na pluginem označených objektech.
2. `world-events` a `rumors` — objevování krátkých událostí bez přesných quest šipek.
3. `wounds` — vzácná a čitelná zranění propojená s Campfire a léčivy.
4. `foraging` — sběr bylin podle biomu, počasí a denní doby.
5. `reputation` — reakce osad a frakcí, služby, ceny a pozdější webové propojení.

Tyto položky nejsou schválené k implementaci, dokud se výslovně nestanou novou
prioritou.
