# Předání projektu Nekara Plugins

## Aktuální stav

### NekaraRPG – aktuální vývojový stav

- NekaraRPG je nyní plně nativní a nemá žádnou externí skill závislost. Rested,
  Rybaření i Echo Vein zapisují XP výhradně do Nekara Skills.
- Echo Vein je volitelný modul nad nativní těžbou; Rybaření zachovává serverem
  vytvořený vanilla úlovek a jeho obtížnost čte nativní level Rybaření.
- Interní skill ID byla jednorázově převedena na ASCII a migrují se při načtení.
- Pro perk-tree cesty, navigaci a vlastní weapon modely je nutný resource pack z
  `https://github.com/Jonaczech/nekara-resourcepack`. Plugin bez něj funguje s
  vanilla náhradními ikonami.

Nejnovější release je **NekaraRPG 2.3.1** (`v2.3.1`). Ověřený artefakt
`NekaraRPG.jar` má SHA-256
`573F53ABBA8A3FEEE7EB1B2482B596A8F52A472CF12346191E6DC4F4E3010C88`.

Repozitář je publikovaný jako
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
Výchozí větev je `main`.

Nejnovější stabilní a publikovaná verze je **NekaraRPG 2.3.0**:

- tag: `v2.3.0`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v2.3.0>
- nasazovaný soubor: `NekaraRPG.jar`
- SHA-256 vydaného i nasazeného JARu:
  `77D7FC8A2304D7125EF45CFF12ACE58C32BA52E59715C47F23966F3CDC86204C`
- automatické ověření: Gradle `release` včetně unit testů úspěšně dokončen

Release 2.2.0 dokončuje live-readiness nativních Nekara Skills: opravuje orientaci
ležícího mannequinu i duplicitní zobrazení výbavy, přidává bezpečný export profilů
a telemetrii XP fronty, zobrazení zdrojů XP a kompletní nativní získávání XP pro
všech 15 dovedností. Řemeslo nyní přidává Tier I–V přímo ve vanilla craftingu a
kovové vybavení prochází zřetelně označeným procesem Blast Furnace → vodní cauldron
→ grindstone. Stonecutter nabízí nativní dřevěné recepty a perk `Zapomenuté nákresy`
škáluje výsledky crafting table i shift-craftingu, včetně sticků a dalších stavebních
komponent. JAR byl bezpečně nasazen na zastavený server; předchozí verze je uložená
lokálně i vzdáleně v `plugins/NekaraRPG/backups`.

Release 2.1.0 rozšiřuje clean-room platformu Nekara Skills o schválené české názvy, původní
katalog 90 perků, detailní 54slotovou stezku každé dovednosti, navigaci,
potvrzovací dialog a atomický nákup za společné body hlavní úrovně. Všech 15
trénovaných skillů má validovaný eventový zdroj XP; sběrné, výrobní, obchodní,
rybářské, bojové a armor perky používají sdílenou cache a exploit pojistky.
SQLite schéma v2 přidává transakční migraci v1 a auditovanou staging správu
profilů přes `/nrpg skills admin`. XP zápisy procházejí omezenou frontou o kapacitě
8192 a dávkách nejvýše 256; hromadné schopnosti mají pevné rozpočty bloků a
nenačítají cizí chunky. Každý skill má vlastní `config.yml` a `messages.yml`,
Rubačina a Zeměrytectví navíc vlastní `loot-tables.yml`; migrace zachovává známé
hodnoty ze starého monolitického configu.

Release současně obsahuje opravy připravené původně pro 1.10.1: sezení i ležení
jsou znovu přímo v `/nrpg`, lehnout lze kdekoliv, Rested funguje jen u ohně a
pohyb už sleeping pózu bezprostředně neruší. Nebezpečná packetová metadata přes
ProtocolLib byla po chybě v živém logu odstraněna. Vizuál ležení nyní používá
nativní Paper/Purpur `Mannequin` se skinem a výbavou hráče; při nepodporované póze
nebo runtime chybě se aktivuje bezpečný serverový fallback.

Kandidát 2.2.0 zapíná nativní modul `skills` jako výchozí autoritu postupu.
ValhallaMMO se pro Nekara Skills nenasazuje; stará Echo Vein aktivita z modulu
`mining` je výchozím stavem vypnutá a bez ValhallaMMO neregistruje tick task ani
listenery. Před ostrým restartem vytvoř export profilů, ověř zálohu a projdi
všech 15 vertikál včetně MSPT. Po restartu nejprve vytěž přirozený stone a ověř
`Kámen | +2 XP do Hornictví`, celkový XP součet v GUI a čistý startovací log.

Metadata GitHub release, hash zpětně staženého JARu, verze uvnitř `plugin.yml`,
přítomnost SQLite ovladače, stabilní `latest` stav, jediný asset, cíl tagu a hash
zpětně staženého JARu byly ověřeny. Zbývá živě ověřit chování na Purpur serveru.

Čísla verzí patří do metadat pluginu, changelogu a Git tagů. Název JARu zůstává
záměrně stabilní, aby při nasazení nevznikla druhá verzovaná kopie vedle
aktivního pluginu.

## NekaraRPG 2.1.0

Centrální menu propojuje NekaraAuth, NekaraMounts, Činnosti a dovednosti.
Při vypnutém `modules.skills.enabled` vede tlačítko dál do ValhallaMMO `/skills`.
Po ručním zapnutí nativního modulu lze otevřít šest perků každé dovednosti a
dostupný perk po potvrzení atomicky koupit. Všech 15 trénovaných dovedností má
nativní zdroj XP a provádí zakoupené runtime efekty. Správa NekaraRPG je viditelná a
otevíratelná pouze s `nekararpg.command.status`, které má výchozí hodnotu `op`.

Schválené názvy jsou: Hlavní úroveň, Řemeslo, Runotepectví, Alchymie, Hornictví,
Rubačina, Zeměrytectví, Hospodářství, Sekání a bodání, Brutální boj, Umění dlaně,
Obchodování, Udičkářství, Umění střelby, Stínový oděv a Plátová ochrana.

Nekara Skills ukládá profily do `skills/data.db` za `SkillProfileRepository`.
SQLite schéma v2 používá WAL, cizí klíče a optimistic revision, migruje v1 v
jedné transakci a ukládá admin změnu společně s identitou správce; neznámou budoucí verzi
schématu odmítne před vytvořením starších tabulek. XP policy a fingerprint guard
jsou zapojené pro všechny tři sběrné zdroje. Hráčem položené bloky se od doby
zapnutí modulu značí v perzistentních datech chunku, časové chunk limity tlumí
farmení a stejný zdrojový otisk se nezapíše dvakrát. Nákup perku vždy znovu načte
profil, ověří úroveň, předchůdce, rank i Power body a uloží jej přes optimistic
revision. Současný dvojklik stejného hráče tlumí zámek probíhajícího zápisu;
autoritou zůstává kontrola uvnitř transakční služby.

Ležení používá pevnou spánkovou pózu bez postele, změny spawnu nebo volání CMI.
Počítá se do Rested stejně jako sezení. Noc se po nastaveném čekání posune pouze
v Overworldu a pouze tehdy, když je hráč jediný online na celém serveru. Starý
`sitting/config.yml` se jednou převede pod `sitting` v `campfire/config.yml` a
zůstane zachovaný jako záloha.

Vydaný modul NekaraMounts přidává virtuální
vytvoření jednoho vanilla koně přes GUI pro barvu a jméno, svázanou píšťalku,
management výbavy, virtuální 54slotové brašny a transakční SQLite backend za
`MountRepository`. Staré `mounts/data.yml` se importuje jednou a zůstane zachované
i jako `data.yml.pre-sqlite.bak`. Selhání zápisu modul uzamkne proti ztrátě nebo
duplikaci. Zbývající živé Purpur scénáře jsou v `TESTING.md` a `LIVE_TESTING.md`.

Release postup prošel se 116 testy. Vydaný asset má velikost `12399790` bajtů
a SHA-256 `C768215E7ED08F7F2ED99F0359597ECFB0B18552A9D5AEEE4EAAB53A4A889514`.
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

### Táboření včetně sezení a ležení

- Příkazy: `/nekararpg sit`, `/nrpg sit` a `/nekararpg stand`.
- Neregistruje hlavní `/sit`; vlastníkem tohoto příkazu zůstává CMI.
- Rozpoznává nakonfigurovaná externí sedadla; výchozí typ pro CMI je `ARMOR_STAND`.
- Aktuální serverový posun sedadla na ose Y je `0.20`.
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

Nejdřív spusť vydaný `NekaraRPG.jar` 2.1.0 na Purpur serveru a podle
`NekaraRPG/TESTING.md` a `NekaraRPG/LIVE_TESTING.md` ověř zejména:

- registraci, login, lockout, přesný zápis nicku a blokaci akcí NekaraAuth,
- bezpečný přechod s aktivním AuthMe a převzetí po jeho odstranění,
- že se vysoké rudy neobjevují mimo vanilla rozsahy,
- že jsou tři zvuky Echo Vein jasně rozlišitelné,
- že +25 % XP vzniká právě jednou z označeného bloku,
- že 50% řetězení a bonusový drop nejsou příliš štědré.

ValhallaMMO ponech zapnuté. Na izolovaném stagingu nastav
`modules.skills.enabled: true` a ověř přehled, všech 15 stezek, ikonky efektů,
zamčené stavy, navigaci, bezpečné klikání, XP zdroje a perky všech vertikál.
Auditovaný grant/reset je dostupný pod `/nrpg skills admin`. Ověř, že Creative,
Spectator, zrušená nebo syntetická událost, hráčem položený blok, duplicitní event
a přehřátý chunk odměnu nevytvoří. Zkontroluj migraci rozdělených konfigurací a
ležící mannequin vizuál bez packet encoder chyb. Bloky položené před prvním
zapnutím trackeru nelze zpětně spolehlivě rozlišit; první staging test proto dělej
v čisté oblasti.

Po záloze lze v plánovaném testovacím okně ValhallaMMO dočasně vypnout a provést
čistě nativní test všech 15 vertikál se sledováním MSPT. Produkční odstranění má
smysl až po exportu a mapování profilů, paritním provozu a ověřeném rollbacku.

Součástí akceptace je osobní přehled, administrátorská diagnostika a celý bezpečný
průchod změny hesla. U Mounts ověř migraci YAML do SQLite, zachování brašen přes
restart a smrt, putování, zotavení pathfindingu, dvojí volání, PvP, cizí manipulaci
a nasazení výbavy z hráčského inventáře. U Táboření ověř ležení s CMI, čištění
pózy a zákaz přeskočení noci při více online hráčích.

Možná pozdější rozšíření Campfire:

- unikátní herní bonusy pro smoker, barrel, cauldron, cartography table a grindstone,
- bohatší postup kvality tábora podle staveb kolem ohně.

Před další změnou spusť `git status`, stáhni `origin/main` a znovu ověř nejnovější
GitHub release, aby tento snapshot nebyl omylem považovaný za živý stav.
