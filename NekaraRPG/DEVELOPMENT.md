# Vývoj a vydávání NekaraRPG

## Release smlouva

Každá vydaná změna končí jako jedna verze jediného pluginu `NekaraRPG` v JARu.
Moduly se nesestavují jako samostatné pluginy. Release je úplný pouze tehdy, když:

1. `gradle.properties` obsahuje zamýšlenou hodnotu `plugin_version`.
2. `CHANGELOG.md` obsahuje odpovídající nadpis `## <version>`.
3. `scripts\build-release.cmd` úspěšně doběhne.
4. Všechny unit testy projdou před kopírováním release artefaktů.
5. Verze vložená do `plugin.yml` a implementačního manifestu JARu odpovídá
   `plugin_version`.
6. Jediný nasazovaný artefakt je `dist\NekaraRPG.jar`; verze je uložená v
   metadatech `plugin.yml`, ne v názvu souboru.
7. JAR projde relevantními kontrolami z `TESTING.md` na staging Purpur serveru.
8. Publikovaný GitHub release je stabilní, označený `v<version>`, obsahuje přesně
   jeden `NekaraRPG.jar` a zpřístupňuje jeho SHA-256 v release metadatech.

Pro opravy používej patch verzi, pro nový modul nebo významnou herní změnu minor
verzi. Auth, fishing, sitting, campfire a mining musí zůstat samostatně zapínatelné
pod `modules` v `config.yml`.

## Standardní postup

Z adresáře `NekaraRPG` spusť:

```powershell
scripts\build-release.cmd
```

Skript provede čistý Gradle task `release`. Ten před kopírováním artefaktu spustí
`build`, tedy kompilaci i všechny testy. Poté zkontroluje vloženou verzi pluginu
a vypíše SHA-256.

Pokud lokální antivirus nebo proxy nahrazuje HTTPS certifikáty, může Java při
stahování závislostí nahlásit `PKIX path building failed`. Importuj lokální
důvěryhodný root do dočasného Java truststore a předej ho bez commitnutí:

```powershell
scripts\build-release.cmd -JavaTrustStore C:\path\to\truststore.p12
```

Nikdy nevypínej TLS ověřování Gradlu a nikdy necommituj certifikát nebo
truststore konkrétního zařízení.

## Podoba modulu

Každý modul implementuje `NekaraModule`, vlastní své listenery a scheduler tasky
a v `disable()` musí uklidit všechny hráčské stavy a entity. Moduly registruj v
`NekaraRPGPlugin` v pořadí závislostí: `auth`, `fishing`, `sitting`, `campfire`,
`mining`. Auth se registruje první, aby nepřihlášení hráči nevstoupili do
herních listenerů ostatních modulů.

Konfigurace patří do typovaného recordu pod `configuration`, výchozí hodnoty do
`config.yml` a hráčské texty do `messages.yml`. Pro výpočty času, škálování nebo
stavů přidávej čisté unit testy, pokud nepotřebují samotný Bukkit.

## Bezpečnostní smlouva NekaraAuth

NekaraAuth musí při nedostupném nebo poškozeném úložišti selhat uzavřeně a login
odmítnout. Heslo se nikdy nezapisuje ani neloguje v plaintextu. Výchozí hash je
PBKDF2-HMAC-SHA256 s náhodnou solí a work factorem uloženým přímo v hash formátu.
Hashování běží mimo hlavní serverový thread v omezeném worker poolu.

Účet se hledá podle nicku normalizovaného přes `Locale.ROOT`, ale uložený přesný
zápis chrání offline UUID před změnou velikosti písmen. Lockout se váže na
normalizovaný nick a chybný pokus se započítá i při odpojení během ověřování.
Nepřihlášený hráč, včetně operátora, nesmí spustit administrativní auth příkaz.

`AccountRepository` je hranice storage. Současný YAML backend zapisuje přes
dočasný soubor a atomický replace; databázová implementace nesmí přesunout
síťové nebo diskové čekání na Bukkit main thread. Pro web použij společnou
identitu nebo jednorázové propojení, nikdy veřejné předávání Minecraft hesla.

## Smlouva NekaraMining a Echo Vein

Echo Vein je první aktivita volitelného modulu `mining` a při chybějícím
ValhallaMMO nebo Mining skillu musí selhat bezpečně. Samotný Bukkit block break
neprokazuje způsobilost: modul ho koreluje s nezrušeným ValhallaMMO Mining XP
eventem `SKILL_ACTION`.

XP označeného bloku se sledují až po ostatních násobitelích. Odložený úspěch
používá důvod `PLUGIN`, takže se Mining, globální ani Rested násobitele
neaplikují znovu. Bonusový loot se vybírá z naklonovaných finálních přirozených
a Valhalla-prepared dropů označeného bloku. Výběr je vážený skutečným množstvím
a omezený na jeden item; Fortune se nepřepočítává a vlastní loot tabulka nevzniká.

Echo Vein je dostupná na každé Mining úrovni. Automatické spuštění a cíle jsou
omezené na stone, deepslate, netherrack a end stone v Paper tagu
`MINEABLE_PICKAXE`. Aktivita nemá cooldown. Každý dokončený cíl může pokračovat
na viditelný blok sousedící stěnou a zároveň nesmí provést nezávislý základní hod.

První přirozený `BlockDamageEvent` provede hod na odhalení rudy právě jednou.
Kandidát musí projít `OreHeightDistribution`, která napodobuje vanilla rozsahy
Y a relativní váhy, ale ne seedový noise ani pravidla vystavení vzduchu. Zlato v
Badlands používá biome key cíle. Netherrack používá Y 10-117 a end stone nemá
žádného kandidáta. Přeměněný cíl aktualizuje materiál relace před kontrolou tickeru.

Interakce s jinými bloky aktivitu neruší. Přirozené pokusy nepíšou do chatu:
objevení žíly, úspěšné odhalení rudy a finální dokončení mají odlišný zvuk,
časovač je v action baru a timeout je tichý.

ID modulu je `mining`. Pokud klíč chybí, upgrade musí zachovat hodnotu starého
`modules.echo-vein.enabled`. Nastavení aktivity a oprávnění zůstávají pod
`echo-vein`.

Staré nastavení a timestampy cooldownu se ignorují. Dočasné rozpracované block
breaky a aktivní výzvy zůstávají v paměti a musí se vyčistit při vypnutí,
reloadu, odpojení nebo neplatném stavu.

## Smlouva updateru

Updater je služba jádra, protože vlastní životní cyklus celého JARu, ne jednoho
herního modulu. Kontroly a stahování z GitHubu běží asynchronně. Důvěryhodný je
jen pevně nastavený veřejný repozitář, endpoint nejnovějšího stabilního releasu,
stabilní název assetu a HTTPS cesta GitHub downloadu.

Stažený artefakt se nejprve zapíše do unikátního dočasného souboru s nastaveným
limitem velikosti. Musí odpovídat velikosti a SHA-256 z GitHubu, obsahovat
`plugin.yml` a v manifestu se identifikovat jako release verze NekaraRPG. Teprve
potom se přesune do aktualizační složky Paperu. Před přípravou aktualizace se
aktivní JAR zkopíruje do datové složky pluginu a ověří se jeho SHA-256, aby měl
administrátor ruční rollback. Služba nikdy nepřepisuje aktivní JAR, nespouští
Bukkit reload a nerestartuje server.

## Předání přes GitHub

Před publikací zkontroluj `git status` a celý diff. Zdrojový kód, konfiguraci,
testy, changelog a dokumentaci commituj společně. Necommituj `build/`, `dist/`,
serverové soubory, Gradle cache ani truststore. Pull request musí uvést verzi,
změněné moduly, automatické kontroly a stav živého testování.
