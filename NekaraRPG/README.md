# NekaraRPG

NekaraRPG je modulární plugin pro Purpur/Paper, který zajišťuje RPG a imerzivní
systémy Nekary. V jednom JARu aktuálně obsahuje NekaraAuth, NekaraFishing, volné
sezení, odpočinek u ohně, NekaraMining s první ValhallaMMO aktivitou Echo Vein a
NekaraMounts s jedním trvalým vanilla koněm na hráče.

Plugin je záměrně konzervativní: nenahrazuje vanilla loot tabulky, nevytváří
syntetické fishing eventy a nevyžaduje ValhallaMMO ani jiný plugin.

## Moduly

Moduly jsou výchozím stavem zapnuté a ovládají se v `config.yml`:

```yml
modules:
  auth:
    enabled: true
  fishing:
    enabled: true
  sitting:
    enabled: true
  campfire:
    enabled: true
  mining:
    enabled: true
  mounts:
    enabled: true
```

Kořenový `config.yml` obsahuje pouze jádro, updater a přepínače modulů. Podrobné
nastavení je rozdělené do `auth/config.yml`, `fishing/config.yml`,
`sitting/config.yml`, `campfire/config.yml`, `mining/config.yml` a
`mounts/config.yml` uvnitř datové
složky pluginu. Při prvním upgradu ze starého monolitického configu se vlastní
hodnoty automaticky přenesou do odpovídajících souborů.

Aktuální moduly:

| Modul | Stav | Popis |
| --- | --- | --- |
| `auth` | testovací | Registrace, přihlášení a ochrana nicku pro offline-mode server. |
| `fishing` | produkční | Nenásilná časovací rybářská minihra kompatibilní s vanilla a ValhallaMMO. |
| `sitting` | produkční | Sezení řízené příkazem a nastavitelná detekce externích sedadel. |
| `campfire` | produkční | Léčení, ochrana hladu, Rested bonus, skupinové škálování a roleplay v action baru u zapáleného ohně. |
| `mining` | testovací | Aktivity NekaraMining, aktuálně prostorová výzva Echo Vein s nativními ValhallaMMO odměnami. |
| `mounts` | testovací | Jeden trvalý vanilla kůň na hráče s bezpečným odvoláním, cooldownem po smrti a ochranou proti duplikaci. |

Campfire přijímá sedadla NekaraRPG i nakonfigurovaná externí vehicle sedadla.
Interní Sitting proto může být vypnutý, pokud sezení poskytuje jiný plugin.
Budoucí moduly lze přidávat bez rozdělení projektu do nesouvisejících JARů.
Vhodnými kandidáty jsou lockpicking, wounds, world events, rumors, territory,
reputation.

## Centrální menu

Hráč otevře hlavní nabídku příkazem `/nekararpg` nebo `/nekararpg menu`. Nabídka
zobrazuje jen moduly, které jsou právě zapnuté v `config.yml` a ke kterým má hráč
oprávnění. Umožňuje otevřít účet a NekaraMounts, sednout si nebo vstát a zobrazuje
stav rybaření, odpočinku a těžby. Původní příkazy zůstávají dostupné jako fallback
a pro administraci. Přístup k nabídce řídí oprávnění `nekararpg.menu.use`.

## NekaraMounts

Hráč nepotřebuje hledat ani ochočovat vanilla koně. `/nekararpg mount` otevře
prvotní GUI s výběrem barvy a následně bezpečnou kovadlinu pro jméno. Vznikne
virtuální záznam jednoho koně a hráč dostane svázanou píšťalku. Administrátor může
stejný průchod otevřít online hráči přes `/nekararpg mount grant <hráč>`.

Pravé kliknutí píšťalkou nebo `mount call` vytvoří odvolaného koně 7-12 bloků od
hráče a pošle ho na přesné místo písknutí. Tam se zastaví a bez jezdce se od něj
nevzdaluje. Aktivní kůň se pouze přesměruje, nikdy nevznikne druhá entita; stav v
nenačteném chunku se odmítne kopírovat. Přivolání má výchozí cooldown 30 sekund.
`dismiss` uloží celý stav před odstraněním entity, stejně jako bezpečný unload,
reload a vypnutí modulu.

Vlastní patnáctisekundové PvP okno se ukládá do `mounts/data.yml`, takže reconnect
blokaci neobejde. Smrt zachová vybavení uvnitř záznamu a výchozím stavem spustí
minutový cooldown. Odvolání neléčí ani nemaže oheň, zamrznutí či potion efekty.
Přejmenování, barva, sedlo, brnění a obnova píšťalky jsou v management GUI.
Píšťalka je svázaná s hráčem: nelze ji zahodit, uložit do cizího inventáře ani
sebrat jiným hráčem. `/nekararpg mount whistle <restore|remove>` odstraní všechny
hráčovy nalezené kopie nebo vydá právě jednu novou.
Jméno aktivní entity je vždy tučné. Smrt nic nedropuje a uložená výbava se vrátí
po death cooldownu. Povolené světy, prostor, pathfinding, autosave, recall a oba
cooldowny se nastavují v `mounts/config.yml`. První verze nemá přímou Lands API
integraci; omezení regionů lze vynutit kontextovým oprávněním nebo seznamem světů.

## NekaraAuth

NekaraAuth je technický modul pro offline-mode server. Každé připojení uzamkne
hráče do dokončení registrace nebo přihlášení. Před ověřením blokuje pohyb,
teleport, chat, běžné příkazy, inventář, interakce, souboj, stavění, těžení,
hlad, drop i pickup itemů. Registrovaný nick je porovnáván bez ohledu na
velikost písmen a výchozím stavem vyžaduje také jeho přesný zápis.

Hlavní ovládání je herní GUI `/nekaraauth`. Registrace a login používají
virtuální kovadlinu, takže heslo nejde běžným chatem; registrace vyžaduje druhé
zadání stejného hesla. Přihlášený hráč může ve stejné správě účtu změnit heslo po
zadání současného hesla, nového hesla a jeho potvrzení. Úspěšná změna zruší uložené
relog session; žádné heslo se nezapisuje do chatu, logu ani datového souboru.
`/login` a `/register` jsou pro běžné hráče vypnuté,
protože příkaz může vidět jiný serverový plugin pracující s command eventy.
Nouzový fallback musí administrátor výslovně zapnout v konfiguraci a povolit
samostatným oprávněním.

Po úspěšném přihlášení si NekaraAuth výchozím stavem ponechá desetiminutovou
session pouze v paměti. Návrat se stejným nickem a IP adresou během této doby
hráče automaticky ověří. `/logout`, kick, zrušení účtu, reload, restart, změna IP
nebo vypršení platnosti session znovu vyžadují heslo. Pokud proxy nepředává
Paperu skutečnou adresu hráče nebo všichni klienti sdílejí jednu proxy adresu,
session vypni v `auth/config.yml` pomocí `session.enabled: false`.

Účty se v první verzi ukládají do `plugins/NekaraRPG/auth/accounts.yml`.
Hesla v něm nejsou čitelná: používá se PBKDF2-HMAC-SHA256 s unikátní solí a
výchozími 600 000 iteracemi. Storage je oddělená rozhraním `AccountRepository`,
aby pozdější databáze propojila herní identitu s webem bez přepsání login flow.
Webový obchod ani databázový backend nejsou součástí 1.4.0.

Při upgradu může updater nový JAR bezpečně připravit ještě s nainstalovaným
AuthMe. Pokud je AuthMe při startu aktivní, NekaraAuth se záměrně nezapne. Pro
převzetí autentizace zastav server, odstraň AuthMe a proveď plný restart.

## Sezení

Příkaz `/nekararpg sit` posadí hráče na aktuální uzemněné pozici a
`/nekararpg stand` ho postaví. Sedadlo je neviditelná netrvalá entita, která se
odstraní při sesednutí, teleportu, smrti, odpojení, nakonfigurovaném poškození,
vypnutí modulu, čištění při reloadu a ukončení serveru.

NekaraRPG záměrně neregistruje hlavní příkaz `/sit`. CMI a jiné sitting pluginy
tak zůstávají vlastníky svých příkazů. Campfire výchozím stavem detekuje externí
sedadla `ARMOR_STAND`, takže `/cmi sit` funguje bez compile-time závislosti na
CMI. Další vehicle typy lze přidat pod `external-seat-entity-types` v
`sitting/config.yml`.

## Odpočinek u ohně

Hráč aktivně odpočívá, když sedí v nastaveném radiusu od zapáleného campfire
nebo soul campfire. Výchozí radius je `5.0` bloků a používá skutečnou
trojrozměrnou vzdálenost od hráče k ohni. Aktivní odpočinek:

- pomalu obnovuje zdraví,
- brání poklesu hladu,
- v nastaveném intervalu doplňuje malé množství hladu,
- škáluje léčení a hlad, pokud jeden oheň sdílí více hráčů,
- zobrazuje nastavitelné progress a roleplay zprávy v action baru,
- používá malé množství částic jako potvrzení aktivní mechaniky.

Po výchozích 20 skutečných sekundách hráč získá základní pětiminutový Rested.
Samotný Rested ztrátu hladu nemění. Smoker v nabitém táboře sníží průměrnou
ztrátu hladu na nastavený násobitel, výchozím stavem `0.5`. Časovač používá
skutečný čas, takže lag serveru dvacetisekundový odpočinek neprodlouží.

Po nabití se Rested obnovuje, dokud hráč zůstává u ohně. Odpočítávání začne až
po opuštění aktivního odpočinku.

### Vybavení tábora

Každý unikátní zapnutý typ táborového bloku do pěti bloků od ohně přidá jednu
minutu Rested. Duplicitní bloky stejného typu se nesčítají. Výchozí sada je
crafting table, bed, smoker, barrel, water cauldron, cartography table a
grindstone, tedy možná délka pět až dvanáct minut.

Crafting table přidává pro daný Rested Haste I. Haste používá standardní potion
ikonu a odpočet Minecraftu. NekaraRPG nepřepisuje silnější Haste z jiného zdroje.

Smoker pro výsledný Rested výchozím stavem zpomaluje ztrátu hladu na polovinu.
Přidání nebo odstranění smokeru během odpočinku aktualizuje obnovovaný bonus
stejně jako Haste z crafting table.

Bed u zapáleného ohně vytváří nastavitelný bezpečný radius 24 bloků. Přirozené
spawny nepřátel se při načteném táboře ruší, existující mobové ale mohou do
tábora vejít. Command, summon, quest, boss a jiné skriptované spawny zůstávají
výchozím stavem nedotčené. S MythicMobs se ruší pouze přirozené náhodné spawny
nastavené frakce `NekaraHostile`; `NekaraFauna` zůstává. MythicMobs je měkká
závislost a NekaraRPG funguje i bez něj.

Roleplay nabíjení zůstává v action baru. Nabitý Rested zobrazuje stručný text
`Odpočatý | m:ss` nezávisle na Haste. Časovač ustupuje rybářské minihře a
zprávám nabíjení. Lze ho vypnout hodnotou
`visuals.rested.indicator: NONE` v `campfire/config.yml`. Další částice jsou volitelné. Dokončení
20sekundového nabíjení potvrzuje jemný nastavitelný ametystový zvuk.

S ValhallaMMO přidává Rested výchozím stavem 10 % k běžným skill-action a
sdíleným XP všech skillů. Administrativní příkazy, resety, redemption a migrační
refundace se nenásobí.

## Echo Vein

Echo Vein je první volitelná aktivita NekaraMining a je dostupná na každém
Mining levelu. Těžba stone, deepslate, netherrack nebo end stone se skutečnými
ValhallaMMO Mining XP má výchozí 5% šanci odhalit blízký viditelný blok stejné
čtveřice. Ores, dirt, gravel, wood ani jiné bloky aktivitu nespouštějí a nemohou
být cílem. Cooldown neexistuje.

Hráč má šest sekund na nalezení a vytěžení cíle krumpáčem. Jemný pulz pokrývá
okolí jednoho bloku a hustší efekt označuje jeho viditelnou stěnu. Při prvním
poškození těžbou má blok jedinou 25% šanci odhalit váženou vanilla-kompatibilní
rudu pro danou Y úroveň. Stone a deepslate používají odpovídající Overworld
varianty, netherrack quartz nebo Nether gold pouze od Y 10 do 117 a end stone
zůstává beze změny.

Distribuce napodobuje vanilla rozsahy a váhy: coal nad Y 0, copper Y 0-96 s
vrcholem kolem 48, iron pod 72 nebo nad 80, gold pod 32 s vyšším Badlands
zlatem, lapis pod 64 s vrcholem kolem 0 a redstone/diamond pod 16 s rostoucí
vahou směrem ke dnu světa. Záměrně nereprodukuje seedový noise, biome density
mimo Badlands zlato ani vanilla omezení rud vystavených vzduchu.

Vytěžení označeného bloku přidá 25 % jeho finálních Mining XP s Valhalla důvodem
`PLUGIN`, takže se Rested, Mining ani globální násobitel neaplikuje znovu. Jeden
bonusový item se vybírá podle skutečného množství z finálních přirozených a
Valhalla-prepared dropů označeného bloku. Metadata zůstávají, množství je nejvýše
jeden a Fortune se nehází znovu.

Každý dokončený cíl má 50% šanci pokračovat na viditelný blok sousedící stěnou.
Řetězené cíle používají stejný časovač a odměny, ale dokončený blok zároveň
nehází nezávislou 5% šanci. Těžba jiných bloků aktivitu neruší. Přirozené pokusy
nepíšou do chatu: hlubší amethyst chime oznamuje žílu, jasnější příbuzný zvuk
potvrzuje rudu a finální úspěch má vlastní zvuk. Timeout je tichý.
`/nekararpg test vein` zůstává bezodměnovým vizuálním testem.

## Kompatibilní režim rybaření

Fishing modul používá `DEFERRED_CATCH`. Zachovává zamýšlený tok minihry i
původní serverem vytvořený úlovek:

1. Vanilla/Paper vyšle `PlayerFishEvent.State.BITE` a NekaraRPG vytvoří čekající relaci vlastněnou UUID.
2. Další pravé kliknutí prutem projde běžným Minecraft rybařením a vytvoří skutečný `PlayerFishEvent.State.CAUGHT_FISH`.
3. NekaraRPG event zruší až poté, co ho mohly pozorovat ostatní listenery, uloží přesný původní ItemStack a vanilla XP, odstraní dočasnou item entitu a spustí minihru v action baru.
4. Při úspěchu doručí uložený původní ItemStack a obnoví vanilla XP; nevytváří syntetický fishing event, loot tabulku ani vlastní item.
5. Při neúspěchu, timeoutu, odpojení, teleportu nebo neplatné relaci se dočasný úlovek zahodí bez odměny.

První kliknutí po záběru vytvoří odložený úlovek a spustí minihru; nepočítá se
jako úspěšný timing hit. Každý další úspěšný zásah přidá
`minigame.time-bonus-ticks` k časovači.

Chat zůstává během hry záměrně tichý: výchozím stavem se posílá jen zpráva o
záběru a finálním úniku. Zásahy a postup ukazuje action bar, bossbar, částice a
zvuky.

## ValhallaMMO

ValhallaMMO je měkká závislost. Pokud je nainstalované, NekaraRPG může:

- škálovat obtížnost rybářské minihry podle ValhallaMMO FishingSkill levelu,
- odložit profesní fishing XP a udělit je spolu s finálním úlovkem,
- zachovat připravené extra dropy, například double-loot,
- přidat Rested hráčům nastavitelná bonusová XP ke všem skillům,
- řídit Echo Vein skutečnými Mining XP a finálními Mining dropy.

NekaraRPG ValhallaMMO loot ani postup skillu nenahrazuje a nepřepočítává. Veřejná
hodnota XP eventu se násobí jen při aktivním Rested. Bonus lze upravit nebo
vypnout:

```yml
campfire:
  rested:
    valhalla-experience:
      enabled: true
      multiplier: 1.10
```

Škálování obtížnosti rybaření se vypíná:

```yml
valhalla:
  fishing-difficulty:
    enabled: false
```

## Požadavky

- Purpur 26.1.2 nebo kompatibilní implementace Paper API
- Java 25
- Pro plánované nasazení NekaraAuth server s `online-mode=false`; při
  `online-mode=true` modul funguje, ale zaloguje varování o dvojím ověření
- Žádné povinné pluginové závislosti; MythicMobs a ValhallaMMO jsou volitelné

Purpur API je použité jako `compileOnly`, takže výstup není fat JAR a neobsahuje
třídy Paper/Purpur.

## Sestavení

Běžné kontroly:

```text
gradlew.bat clean test build
```

Ověřený release:

```text
scripts\build-release.cmd
```

Výstupy releasu:

- `build/libs/NekaraRPG.jar`
- `dist/NekaraRPG.jar`
- `../../dist/NekaraRPG.jar` v kořeni repozitáře

Sémantická verze zůstává v `plugin.yml`, manifestu JARu, changelogu a Git tagu;
v názvu nasazovaného souboru záměrně není.

Release smlouva je v `DEVELOPMENT.md`, staging postup v `LIVE_TESTING.md`.

## Instalace

Zkopíruj JAR z `dist/` do serverového `plugins/`, spusť Purpur a uprav:

- `plugins/NekaraRPG/config.yml`
- `plugins/NekaraRPG/messages.yml`
- `plugins/NekaraRPG/auth/config.yml`
- `plugins/NekaraRPG/fishing/config.yml`
- `plugins/NekaraRPG/sitting/config.yml`
- `plugins/NekaraRPG/campfire/config.yml`
- `plugins/NekaraRPG/mining/config.yml`
- `plugins/NekaraRPG/auth/accounts.yml` (vznikne po prvním spuštění NekaraAuth)

Při upgradu není nutné mazat stávající `plugins/NekaraRPG`. Starý monolitický
config se při prvním načtení bezpečně migruje a chybějící nové klíče používají
zabalené runtime defaults.

Po změně konfigurace použij `/nekararpg reload`. Reload bezpečně ukončí aktivní
relace a neregistruje duplicitní listenery ani ticker tasky. JAR se reloadem
nenasazuje.

## Automatické aktualizace

Updater byl přidán ve verzi 1.2.0, která proto vyžadovala první ruční instalaci.
Poté NekaraRPG výchozím stavem kontroluje nejnovější stabilní release
`Jonaczech/nekara-plugins` po startu a každých šest hodin.

Při dostupné novější sémantické verzi přijme pouze přesný asset
`NekaraRPG.jar`. Ověří důvěryhodnou GitHub URL, deklarovanou velikost, SHA-256,
identitu JARu, vloženou verzi a přítomnost `plugin.yml`. Ověřený JAR přesune do
update složky Paperu a Paper ho nainstaluje při dalším úplném restartu.
NekaraRPG nikdy nenahrazuje ani nereloaduje svůj aktivní JAR. Před přípravou
aktualizace vytvoří hashově ověřenou zálohu v `plugins/NekaraRPG/backups`.

Automatické kontroly a stahování se nastavují pod `updater` v `config.yml`.
Administrátor může kdykoliv spustit `/nekararpg update check` a stav ověřit přes
`/nekararpg update status`. Online hráči s `nekararpg.update.notify` dostanou
upozornění při přípravě releasu a znovu při připojení před restartem.

## Příkazy a oprávnění

| Příkaz | Oprávnění |
| --- | --- |
| `/nekararpg help` | `nekararpg.command.help` |
| `/nekararpg reload` | `nekararpg.command.reload` |
| `/nekararpg status` | `nekararpg.command.status` |
| `/nekararpg update check` | `nekararpg.command.update` |
| `/nekararpg update status` | `nekararpg.command.update` |
| `/nekararpg sit` | `nekararpg.sitting.use` |
| `/nekararpg stand` | žádné; hráč se musí vždy moci postavit |
| `/nekararpg mount [menu\|status\|call\|dismiss\|whistle]` | `nekararpg.mount.use` |
| `/nekararpg mount grant <hráč>` | `nekararpg.mount.admin` |
| `/nekararpg test [fishing|vein]` | `nekararpg.command.test` |
| `/nekararpg cancel [player]` | `nekararpg.command.cancel` |
| `/nekaraauth` | žádné; otevře účetní GUI |
| `/login <heslo>` | výchozím stavem vypnuto; `nekararpg.auth.fallback-commands` a zapnutý fallback v konfiguraci |
| `/register <heslo> <heslo>` | výchozím stavem vypnuto; `nekararpg.auth.fallback-commands` a zapnutý fallback v konfiguraci |
| `/logout` | žádné |
| `/nekaraauth status` | `nekararpg.auth.admin` |
| `/nekaraauth unregister <hráč>` | `nekararpg.auth.admin` |

Aliasy: `/nrpg`, `/nekarafishing`, `/nfishing`.

Fishing vyžaduje `nekararpg.use`, výchozím stavem pro každého hráče.
`nekararpg.bypass` přeskakuje minihru a výchozím stavem je false. Campfire
vyžaduje `nekararpg.campfire.use`, Echo Vein `nekararpg.echo-vein.use`; obě jsou
výchozím stavem povolené. Upozornění updateru vyžadují
`nekararpg.update.notify`, výchozím stavem pro operátory.

Starší oprávnění `nekarafishing.*` jsou stále deklarovaná a přijímaná, aby šlo
existující nastavení postupně migrovat.

## Zvuky a zprávy

Zvuky podporují vanilla namespaced ID i vlastní resource-pack ID, například
`nekara:fishing.hit`. Vanilla ID se kontrolují proti API registru; vlastní ID se
kontrolují syntakticky a jejich dostupnost závisí na klientském resource packu.
Neplatná ID se zalogují a přeskočí.

Zprávy používají `messages.yml`, staré `&` barvy a MiniMessage tagy, pokud jsou
přítomné. Action bar se sestavuje interně bez závislosti na PlaceholderAPI.

## Omezení

- První registrace si nick nárokuje. Při prvotním nasazení ponech whitelist,
  dokud si administrátoři a vlastníci rezervovaných nicků nevytvoří účty.
- NekaraAuth automaticky neimportuje existující AuthMe účty a neobsahuje
  databázový ani webový backend; před odstraněním AuthMe je proto nutná řízená migrace.
- Fishing je záměrně časovací brána v action baru a nenahrazuje Minecraft rybaření.
- Sitting používá serverové passenger sedadlo a nepřidává klientský keybind.
  Výchozí Y posun je nastavitelný a starší předprodukční hodnoty se migrují na `0.20`.
- Externí sedadla se detekují podle vehicle typu. Bezpečný default je
  `ARMOR_STAND`; pluginy s jinou entitou ji musí přidat do konfigurace.
- Campfire a Rested mohou dočasně nahradit jiný méně prioritní text v action
  baru. Fishing minihra má vždy před časovačem Rested přednost.
- Automatické Echo Vein vyžaduje ValhallaMMO Mining. Testovací příkaz ověřuje
  viditelnost cíle bez odměn.
- Staré `minimum-mining-level` z 1.2.1 a starších se ignoruje.
- Pokud chybí `modules.mining.enabled`, použije se staré `modules.echo-vein.enabled`.
- Bonusový item Echo Vein pochází z finálních dropů cílového bloku. Digging
  treasure tabulka ani samostatný loot se nepoužívá.
- Rested má jen textový časovač; tábor s crafting table navíc používá vanilla
  ikonu Haste. Vlastní stavová ikona by vyžadovala resource pack nebo mod.
- Smoker zpomaluje Rested ztrátu hladu, crafting table přidává Haste a
  ValhallaMMO skill XP dostávají nakonfigurovaný Rested násobitel.
- Minihra začíná kliknutím prutem po záběru a odkládá původní `CAUGHT_FISH`
  item do finálního zásahu.
- Server nemůže ověřit klientský resource-pack zvuk, pouze jeho namespaced syntaxi.
- Úplná akceptace vyžaduje živý Purpur 26.1.2, protože pořadí Bukkit eventů a
  interakce pluginů nelze plně simulovat unit testy.
- Updater pouze připraví release a nikdy nerestartuje server. Úplný restart a
  kontrolu logů musí provést administrátor nebo hosting scheduler.

## Licence

Licence ve stylu MIT; viz `LICENSE`.
