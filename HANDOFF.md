# Předání projektu Nekara Plugins

## Aktuální stav

Repozitář je publikovaný jako
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
Výchozí větev je `main`.

Nejnovější vydaná verze je **NekaraRPG 1.2.5**:

- tag: `v1.2.5`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.2.5>
- nasazovaný soubor: `NekaraRPG.jar`
- zdrojové PR: <https://github.com/Jonaczech/nekara-plugins/pull/15>
- release merge commit: `47944ab70e7a88a07376a9945449ea5470c80e73`
- SHA-256 vydaného JARu:
  `D6D95DBE342DF9F9161EA154DC1709286BC50611C3B8C5B8AEF7166AD0A4AFA2`
- velikost release assetu: `190588` bajtů
- automatické ověření: 42 úspěšných testů

Verze 1.2.5 zachovává tok Echo Vein z 1.2.4 a přidává výškově řízené
odhalování rud. Kandidáti respektují vanilla rozsahy Y a relativní výškové
váhy, včetně zlata ve vyšších polohách Badlands a netherového rozsahu Y 10-117.
Diamant ani redstone se nemohou objevit vysoko mimo svoji přirozenou oblast.
Seedový noise a vanilla potlačení rud vystavených vzduchu se záměrně
nesimulují. Úspěšná přeměna na rudu používá jasnější ametystový zvuk, odlišný od
hlubšího zvuku objevení žíly.

Metadata GitHub release, hash zpětně staženého JARu, stabilní stav, jediný asset
i cíl tagu byly ověřeny. Zbývá živě ověřit zvuky, pořadí událostí a vyvážení na
Purpur serveru s ValhallaMMO.

Čísla verzí patří do metadat pluginu, changelogu a Git tagů. Název JARu zůstává
záměrně stabilní, aby při nasazení nevznikla druhá verzovaná kopie vedle
aktivního pluginu.

## Rozpracovaný kandidát 1.3.0

Lokální pracovní strom po 1.2.5 obsahuje nový modul `auth` (NekaraAuth). Přidává
GUI registraci a přihlášení přes virtuální kovadlinu, ochranu nicku pro
offline-mode, PBKDF2-HMAC-SHA256, per-nick lockout a souborové repository
`plugins/NekaraRPG/auth/accounts.yml`. Úložiště i chyby selhávají uzavřeně.

Kandidát zatím není GitHub release a nesmí být automaticky nasazen do produkce.
Před odstraněním AuthMe je nutný živý průchod sekce NekaraAuth v `TESTING.md`,
plán migrace existujících AuthMe účtů a whitelist během prvotní rezervace nicků.

## Vydané moduly NekaraRPG

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

Nejdřív živě otestuj verzi 1.2.5 podle `NekaraRPG/LIVE_TESTING.md`, zejména:

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
