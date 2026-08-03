# Předání projektu Nekara Plugins

## Aktuální stav

Repozitář je publikovaný jako
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
Výchozí větev je `main`.

Nejnovější stabilní a publikovaná verze je **NekaraRPG 1.8.0**. Vestavěný
updater ji rozpozná jako nástupce verze 1.6.0:

- tag: `v1.8.0`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.8.0>
- nasazovaný soubor: `NekaraRPG.jar`
- release commit: `c3ad9fc5c36686d4fdcaecb8749c761a6073ed67`
- SHA-256 vydaného JARu:
  `2BC4FB04B736B0338B67FDA3855535A45607E551AA3019B72CAC6F638C05AEC8`
- velikost release assetu: `12302405` bajtů
- automatické ověření: 70 úspěšných testů

Verze 1.8.0 rozšiřuje centrální GUI o osobní přehled a administrátorskou
diagnostiku. NekaraMounts přidává přirozené putování, přesměrování aktivního koně,
výchozí sedlo, truhlu a 54slotové virtuální brašny. Mounts persistence přechází
z YAML na SQLite s jednorázovým importem a zálohou původních dat.

Metadata GitHub release, hash zpětně staženého JARu, verze uvnitř `plugin.yml`,
přítomnost SQLite ovladače, stabilní `latest` stav, jediný asset a cíl tagu byly
ověřeny. Zbývá živě ověřit pořadí událostí, migraci a chování na Purpur serveru.

Čísla verzí patří do metadat pluginu, changelogu a Git tagů. Název JARu zůstává
záměrně stabilní, aby při nasazení nevznikla druhá verzovaná kopie vedle
aktivního pluginu.

## NekaraRPG 1.8.0

Centrální menu propojuje NekaraAuth, NekaraMounts, Fishing, Sitting, Campfire a
Mining bez přesunu doménové logiky. Hráčský přehled ukazuje účet, Rested, aktivní
činnost a koně. Oprávněná diagnostika ukazuje moduly, integrace, updater, paměť a
stav Mounts úložiště. Při načteném ValhallaMMO lze otevřít jeho vlastní `/skills`.

Vydaný modul NekaraMounts přidává virtuální
vytvoření jednoho vanilla koně přes GUI pro barvu a jméno, svázanou píšťalku,
management výbavy, virtuální 54slotové brašny a transakční SQLite backend za
`MountRepository`. Staré `mounts/data.yml` se importuje jednou a zůstane zachované
i jako `data.yml.pre-sqlite.bak`. Selhání zápisu modul uzamkne proti ztrátě nebo
duplikaci. Zbývající živé Purpur scénáře jsou v `TESTING.md` a `LIVE_TESTING.md`.

Release postup prošel se 70 testy. Vydaný asset má velikost `12302405` bajtů
a SHA-256 `2BC4FB04B736B0338B67FDA3855535A45607E551AA3019B72CAC6F638C05AEC8`.
Release build ověřuje verzi, manifest a přítomnost zabaleného SQLite JDBC
ovladače. Smrt koně má minutový cooldown a píšťalka zůstává soulbound.

Rozhodnutí MVP jsou uzavřená v `ROADMAP.md`: žádné ochočování, GUI plus admin grant,
30sekundová píšťalka, vzdálený spawn s doběhnutím na místo volání, vlastní
perzistentní combat okno a normalizovaný nick jako stabilní offline identita.

## Vydané moduly NekaraRPG

### NekaraAuth

- Chrání offline-mode nick registrací a přihlášením přes herní GUI.
- Hesla ukládá pouze jako salted PBKDF2-HMAC-SHA256 hash.
- Před přihlášením blokuje pohyb, interakce, inventář, chat, boj a běžné příkazy.
- Aktivní AuthMe má při přechodu přednost; existující účty se automaticky nemigrují.

### Rybaření

- Používá odložené dokončení úlovku a zachovává původní vanilla item i XP.
- Zachovává volitelné ValhallaMMO loot odměny, profesní XP a škálování obtížnosti.
- Časování zobrazuje v action baru a používá vlastní bossbar minihry.

### Sezení

- Příkazy: `/nekararpg sit`, `/nrpg sit` a `/nekararpg stand`.
- Neregistruje hlavní `/sit`; vlastníkem tohoto příkazu zůstává CMI.
- Rozpoznává nakonfigurovaná externí sedadla; výchozí typ pro CMI je `ARMOR_STAND`.
- Aktuální serverový posun sedadla na ose Y je `0.20`.

### Táboření

- Přijímá NekaraRPG i podporované externí sezení do pěti bloků od zapáleného
  campfire nebo soul campfire.
- Pomalu léčí, blokuje ztrátu hladu, mírně doplňuje hlad a používá omezený
  skupinový násobitel pro každý oheň.
- Udělí stav Rested po 20 skutečných sekundách. Základní délka je pět minut.
- Každý unikátní typ táborového prvku přidá jednu minutu: crafting table, bed,
  smoker, barrel, water cauldron, cartography table a grindstone.
- Duplicitní bloky stejného typu se nesčítají. Všech sedm výchozích prvků dává
  maximální délku Rested 12 minut.
- Crafting table přidává řízený Haste I. Bed vytváří ochranný táborový radius
  24 bloků proti přirozenému spawnu nepřátel.
- Volitelná podpora MythicMobs blokuje náhodný přirozený spawn frakce
  `NekaraHostile`, ale neovlivňuje `NekaraFauna`, existující moby ani skriptovaná
  setkání.
- Rested používá text v action baru: `Odpočatý | m:ss`. Ustupuje nabíjení
  odpočinku a rybářské minihře. Nevytváří bossbar.

### NekaraMining / Echo Vein

- Používá ID modulu `mining`; staré `modules.echo-vein.enabled` slouží jako
  fallback, dokud není výslovně uvedeno `modules.mining.enabled`.
- Vyžaduje skutečnou ValhallaMMO Mining XP akci, ale nemá podmínku úrovně.
- Spouští se jen z `STONE`, `DEEPSLATE`, `NETHERRACK` a `END_STONE` v Paper tagu
  `MINEABLE_PICKAXE` a vybírá pouze tyto bloky. Nemá cooldown.
- Viditelnou stěnu cíle označuje silněji než okolí v radiusu jednoho bloku.
  Přirozený pokus je dokončen pouze skutečným vytěžením cíle.
- Úspěch přidá 25 % finálních Mining XP označeného bloku s důvodem `PLUGIN`.
- Volitelný bonus je jeden item z finálních Mining dropů označeného bloku.
  Nepoužívá Digging tabulku, vlastní loot tabulku ani nový Fortune hod.
- Dokončený cíl má 50% šanci pokračovat na viditelný blok sousedící stěnou.
- První poškození cíle provede jediný 25% hod na odhalení rudy. Stone a
  deepslate používají výškově odpovídající Overworld rudy, netherrack quartz
  nebo gold pouze od Y 10 do 117 a end stone zůstává beze změny. Badlands mohou
  odhalit zlato i ve vyšší poloze.
- Těžba nebo úder do jiného bloku aktivní cíl nezruší.
- Přirozené pokusy nepíšou do chatu. Objevení žíly, odhalení rudy a finální
  úspěch mají odlišné zvuky; timeout je tichý. Časovač zůstává v action baru.
- `/nekararpg test vein` ověřuje vizuál bez XP, dropu, přeměny rudy a řetězení.

## Sestavení a nasazení

V adresáři `NekaraRPG` spusť:

```powershell
scripts\build-release.cmd
```

Skript sestaví a ověří jediný artefakt
`NekaraRPG/dist/NekaraRPG.jar`. Pokud lokální TLS inspekce způsobí chybu PKIX,
předej lokální PKCS12 truststore parametrem `-JavaTrustStore`; truststore nikdy
necommituj a nevypínej TLS ověřování.

Nasazení na server:

1. Zastav server.
2. Ověř, že v `plugins` není druhý `NekaraRPG*.jar` ani starý `NekaraFishing*.jar`.
3. Nahraď pouze `plugins/NekaraRPG.jar`.
4. Spusť server a zkontroluj startovací logy.
5. `/nekararpg reload` používej jen pro konfiguraci a zprávy, nikdy k výměně JARu.

Při běžném upgradu nemaž `plugins/NekaraRPG`. Chybějící nové klíče získají
výchozí hodnoty zabalené v pluginu.

Od verze 1.2.0 může updater stáhnout novější stabilní GitHub release do
aktualizační složky Paperu. Kontroluje přesný název assetu, velikost, SHA-256,
identitu JARu a sémantickou verzi. Server nikdy nerestartuje; administrátor musí
provést úplný restart a zkontrolovat startovací logy.

## Další práce

Nejdřív živě otestuj vydanou verzi 1.8.0 podle `NekaraRPG/TESTING.md` a
`NekaraRPG/LIVE_TESTING.md`, zejména:

- registraci, login, lockout, přesný zápis nicku a blokaci akcí NekaraAuth,
- bezpečný přechod s aktivním AuthMe a převzetí po jeho odstranění,
- že se vysoké rudy neobjevují mimo vanilla rozsahy,
- že jsou tři zvuky Echo Vein jasně rozlišitelné,
- že +25 % XP vzniká právě jednou z označeného bloku,
- že 50% řetězení a bonusový drop nejsou příliš štědré.

Součástí akceptace je osobní přehled, administrátorská diagnostika a celý bezpečný
průchod změny hesla. U Mounts ověř migraci YAML do SQLite, zachování brašen přes
restart a smrt, putování, zotavení pathfindingu, dvojí volání, PvP a cizí manipulaci.

Možná pozdější rozšíření Campfire:

- unikátní herní bonusy pro smoker, barrel, cauldron, cartography table a grindstone,
- bohatší postup kvality tábora podle staveb kolem ohně.

Před další změnou spusť `git status`, stáhni `origin/main` a znovu ověř nejnovější
GitHub release, aby tento snapshot nebyl omylem považovaný za živý stav.
