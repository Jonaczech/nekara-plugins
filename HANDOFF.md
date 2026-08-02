# Předání projektu Nekara Plugins

## Aktuální stav

Repozitář je publikovaný jako
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
Výchozí větev je `main`.

Nejnovější vydaná verze je **NekaraRPG 1.3.0**:

- tag: `v1.3.0`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.3.0>
- nasazovaný soubor: `NekaraRPG.jar`
- zdrojové PR: <https://github.com/Jonaczech/nekara-plugins/pull/18>
- release merge commit: `d16386f16a73ed08c87ffb543c67b56f84ac56eb`
- SHA-256 vydaného JARu:
  `8B60373F4839C48A5523017117A3EF6F41F1BBA641BBE3CFB90B95A4A776541D`
- velikost release assetu: `230454` bajtů
- automatické ověření: 49 úspěšných testů

Verze 1.3.0 přidává modul `auth` (NekaraAuth): GUI registraci a přihlášení přes
virtuální kovadlinu, ochranu nicku pro offline-mode, PBKDF2-HMAC-SHA256,
per-nick lockout a fail-closed souborové repository
`plugins/NekaraRPG/auth/accounts.yml`. Pokud je při startu aktivní AuthMe,
NekaraAuth zůstane vypnutý a převezme autentizaci až po odstranění AuthMe a
následujícím plném restartu.

Metadata GitHub release, hash zpětně staženého JARu, stabilní stav, jediný asset
i cíl tagu byly ověřeny. Zbývá živě ověřit zvuky, pořadí událostí a vyvážení na
Purpur serveru s ValhallaMMO a celý NekaraAuth průchod z `TESTING.md`.

Čísla verzí patří do metadat pluginu, changelogu a Git tagů. Název JARu zůstává
záměrně stabilní, aby při nasazení nevznikla druhá verzovaná kopie vedle
aktivního pluginu.

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

Nejdřív živě otestuj verzi 1.3.0 podle `NekaraRPG/TESTING.md` a
`NekaraRPG/LIVE_TESTING.md`, zejména:

- registraci, login, lockout, přesný zápis nicku a blokaci akcí NekaraAuth,
- bezpečný přechod s aktivním AuthMe a převzetí po jeho odstranění,
- že se vysoké rudy neobjevují mimo vanilla rozsahy,
- že jsou tři zvuky Echo Vein jasně rozlišitelné,
- že +25 % XP vzniká právě jednou z označeného bloku,
- že 50% řetězení a bonusový drop nejsou příliš štědré.

Po akceptaci je schváleným dalším návrhovým směrem modul `mounts`. První verze
má použít vanilla koně, jednoho trvalého mounta na hráče, bez nákladního
inventáře a bez modelu hoglina. Musí uchovávat sedlo, brnění, jméno, zdraví,
vlastnictví a cooldown po smrti a zabránit zneužití přivolání, odvolání,
odhlášení a léčení v PvP.

Možná pozdější rozšíření Campfire:

- unikátní herní bonusy pro smoker, barrel, cauldron, cartography table a grindstone,
- bohatší postup kvality tábora podle staveb kolem ohně.

Před další změnou spusť `git status`, stáhni `origin/main` a znovu ověř nejnovější
GitHub release, aby tento snapshot nebyl omylem považovaný za živý stav.
