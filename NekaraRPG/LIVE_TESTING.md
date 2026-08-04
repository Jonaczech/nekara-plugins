# Živé testování NekaraRPG

## Doporučené prostředí

Použij samostatný staging server Purpur 26.1.2 s Javou 25. Svět i port odděl od
produkce. Testuj ve dvou průchodech:

1. Pouze Purpur a NekaraRPG s aktivním nativním modulem `skills`; to je primární
   produkční průchod.
2. Volitelně stejný server s CMI a ValhallaMMO pouze pro ověření starých kompatibilních
   bridge. Nativní postup je nezávislý a ValhallaMMO se pro něj nenasazuje.

Sestavení a nasazení z adresáře `NekaraRPG`:

```powershell
scripts\build-release.cmd
scripts\deploy-test.cmd -ServerPath D:\path\to\test-server
```

Před výměnou server zastav a potom ho znovu spusť. Bukkit `/reload` nepoužívej k
nahrazení JARu. `/nekararpg reload` slouží pouze k načtení konfigurace a zpráv.
Deploy skript odmítne pokračovat, pokud vedle stabilního `NekaraRPG.jar` najde
další `NekaraRPG*.jar` nebo starý `NekaraFishing*.jar`; nahlášený soubor nejdřív
přesuň mimo `plugins`.

Před nasazením si ponech ověřenou kopii aktivního JARu i datové složky. Při přechodu
bez ValhallaMMO nastav v existujícím `plugins/NekaraRPG/config.yml` explicitně
`modules.skills.enabled: true` a `modules.mining.enabled: false`; existující konfigurace
se upgradem záměrně automaticky nepřepisuje.

## Nativní Nekara Skills 2.2

1. Na čistém serveru je `modules.skills.enabled: true` výchozí hodnota. U existující
   datové složky zkontroluj hodnotu ručně a vypni `modules.mining.enabled`, pokud
   ValhallaMMO není nainstalované.
2. Otevři `/nrpg` a Dovednosti. Zkontroluj 54slotový přehled, všech 15 trénovaných
   dovedností, Power, volné body, návrat a blokaci přesunů itemů. Každá stezka musí
   ukazovat i přesný celkový součet XP; informační kniha v přehledu všech dovedností
   vysvětluje action-bar potvrzení, barvy vazeb a ovládání stezky.
   Pozadí stromu je šedé sklo, nenaučené vazby bílé a vazba mezi dvěma odemčenými
   perky zelená. Osm ukotvených šipek v rozích a středech hran posouvá viewport
   virtuálního stromu ve čtyřech hlavních i čtyřech šikmých směrech, pouze pokud tam
   existuje další část; krajní šipka zůstane viditelná a klik nesmí zavřít GUI.
   Spodní pevný posuvník má zlaté šipky pro skok o tři dovednosti, sousední dovednosti
   po stranách a aktuální dovednost uprostřed. Samostatná zlatá šipka vlevo dole vrací
   hráče na přehled; kniha se uvnitř jednotlivé stezky nezobrazuje.
3. V čisté oblasti vytěž krumpáčem přirozený stone a několik rud. Hornictví musí
   získat právě jednu konfigurovanou XP odměnu a do 0,4 s ukázat například
   `+2 XP | ▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱▱ 2 %`; Power se pouze přepočítá z průměru. Rychle získané
   stejné odměny se smí sloučit, ale odmítnutý, duplicitní či uložením neúspěšný
   požadavek žádnou zprávu nevypíše.
   Creative, Spectator, položený blok a zrušený break odměnu nedají.
   U Hospodářství ověř zralé sweet/glow berries, přírodní květinu, malou houbu a osm
   přírodních trav. Berries, květina a houba dají právě jednu nakonfigurovanou odměnu;
   tráva až po osmém bloku. Položené květiny, houby a tráva nesmí dát XP.
4. Bez ValhallaMMO musí `/nrpg` otevřít nativní přehled dovedností a server nesmí
   logovat chybějící třídu ani vytvářet druhý postupový systém. Při volitelném
   kompatibilním průchodu stále ponech pouze Nekara Skills jako autoritu XP a perků.
5. Ověř `skills/data.db`, restart a reload. Při chybě storage se modul uzamkne a
   nesmí tiše vytvořit druhou databázi nebo přepsat profil.
6. Přes `/nrpg skills admin` ověř `inspect`, zastropovaný `grant-xp`, validovaný
   `grant-perk` a reset skillu/perků/celého profilu. Bez
   `nekararpg.skills.admin` musí být příkazy odmítnuté. Každá změna má zvýšit
   revision a objevit se v posledních auditních záznamech.
   Přes `points <hráč> add 10` ověř deset testovacích volných bodů a přes
   `points <hráč> remove 10` jejich odebrání; odebrání nesmí sahat na přirozené
   Power body ani vytvořit záporný bonus.
7. Na kopii v1 databáze ověř automatický přechod na schéma v2 bez ztráty XP,
   perků nebo revision. Souběžný admin reset a těžební XP nesmí vytvořit
   částečný profil či audit.
8. Zakoupený `Hlas kamene` nebo `Srdce hory` musí násobit pouze skutečné finální
   dropy jednoho přirozeného bloku. Ověř metadata, Fortune, Silk Touch a plný
   inventář bez druhého loot průchodu.
9. Neprováděj ruční úpravy živé SQLite databáze. Nový nativní postup nevyžaduje
   import z ValhallaMMO.
10. Uděl staging profilu perky všech 15 dovedností. Ověř XP postupně po skupinách:
    pět sběrných, čtyři výrobní/obchodní a šest bojových/armor. Po každé skupině
    zkontroluj konzoli, TPS/MSPT a velikost `skills/data.db`.
11. Žilobití a Pád velikána spusť plížením. Ověř blokový limit, cooldown, dávkování,
    durability, nenahrávání sousedního chunku a skutečné zrušení každého chráněného
    bloku přes Lands.
12. Řízený odstřel ověř vlastním TNT: žádný oheň, omezený počet bloků, žádné XP za
    výbuch a beze změny bloků odstraněných regionovou ochranou.
13. Po úspěchu s načteným ValhallaMMO vytvoř ověřenou zálohu JARů, konfigurace,
    Valhalla dat a `skills/data.db`. V plánovaném testovacím okně ValhallaMMO
    dočasně vypni a zopakuj všech 15 vertikál čistě nativně. Produkční odstranění
    proveď až po přijetí výsledků, mapovacím reportu profilů a úspěšném rollbacku.
14. Zátěžový průchod: skliď pole, poraz strom, vytěž žílu a bojuj ve více hráčích.
    Nesmí vznikat per-tick scan světa; XP fronta nesmí hlásit zaplnění a serverové
    MSPT se musí po skončení burstu vrátit na původní baseline.
15. Výrobní exploit průchod: shift-click crafting, dva hráči u jednoho vesničana,
    hopper brewing bez nového ručního vstupu a zrušený enchant/craft. Bonus ani XP
    nesmí vzniknout bez dokončené nativní události.
15a. Pro perk recepty zkus shift-crafting, neúplnou mřížku, stack více ingrediencí
    v jednom slotu, Creative a dvě postavy u stejné crafting table. Výsledek smí
    vzniknout pouze z přesného rozložení ingrediencí, odpovídajícího zakoupeného perku
    a jednoho dokončeného craftu; zvláštní šíp musí označit jen po výstřelu hráče,
    který vlastní perk `Šípařova brašna`.
16. Po upgradu ověř vznik všech 15 složek `skills/<skill>`. Vlastní hodnoty ze
    starého `skills/config.yml` musí být v nových souborech a woodcutting/digging
    nálezy v `loot-tables.yml`; databáze ani profily se migrací nemění.
17. U lehkých zbraní ověř krvácení na nepřátelské entitě: tři následné tiky,
    žádná rekurzivní kritika/XP a obnovení silnějším zásahem. V GUI musí mít
    krvácení redstone, omráčení mace a kritický zásah netheritový meč.
18. `/nrpg` → `Lehnout si` nesmí hráče odpojit ani zapsat chybu
    `clientbound/minecraft:set_entity_data`. V první i třetí osobě a z pohledu
    druhého hráče ověř nativní sleeping mannequin, shodný skin/výbavu, odstranění
    po vstání, zásahu, teleportu a odpojení a pokračující nabíjení Rested bonusu.
19. V třetí osobě ověř, že model leží ve směru předchozího pohledu a je vystředěný
    na původním místě. Krumpáč, offhand ani armor nesmí být dvakrát. Pokud je
    potřeba jemná korekce, uprav pouze `lying.mannequin.*` a reloaduj konfiguraci.
20. Před a po zátěžovém průchodu ulož `/nrpg skills admin metrics`, potom spusť
    `export`. Ověř nulovou frontu po doběhu, žádné queue reject/storage chyby,
    shodný hash ZIPu a možnost otevřít exportovaný SQLite snapshot odděleně od
    živé databáze.

## Profil pro rychlé testování

Pro krátký vývojový cyklus dočasně použij na staging serveru:

```yml
campfire:
  update-period-ticks: 10
  healing:
    amount: 2.0
    period-seconds: 1
  hunger:
    restore-amount: 1
    restore-period-seconds: 2
  rested:
    charge-seconds: 5
    duration-seconds: 20
  camping:
    duration-per-feature-seconds: 10
    spawn-protection:
      radius: 10.0
```

Po změně spusť `/nekararpg reload`. Před akceptací vrať produkční defaults:
20sekundové nabíjení, základ pět minut, jednu minutu za unikátní prvek, bezpečný
radius 24 bloků, jeden bod zdraví za pět sekund a jeden bod jídla za deset sekund.

## Akceptace centrálního menu a změny hesla

1. Přihlášený hráč otevře `/nekararpg`; vidí pouze zapnuté moduly, ke kterým má
   oprávnění. Konzole bez argumentu dál dostane textovou nápovědu.
2. Otevři NekaraMounts a NekaraAuth z centrálního menu a ověř návrat do jejich
   existujících bezpečných GUI bez duplikace mounta nebo píšťalky.
3. Klikni na prázdné výplně a informační dlaždice. Menu musí zůstat otevřené a
   chat nesmí zaplavit technickými vysvětlivkami. S ValhallaMMO ověř tlačítko
   dovedností proti přímému `/skills`.
4. V NekaraAuth změň heslo přes současné heslo, nové heslo a potvrzení. Po logoutu
   ověř odmítnutí starého a přijetí nového hesla.
5. Ověř chybný současný údaj, neshodné potvrzení, zavření každého kroku a odpojení
   během výpočtu. Účet musí zůstat použitelný a plaintext se nesmí objevit v logu.

## Akceptace sezení a ležení

1. Po prvním ulehnutí ověř v logu `Native mannequin lying visuals enabled`; nesmí se
   objevit chyba spawnu ani packet encoderu. Potom spusť `/nekararpg sit` na plných
   blocích, slabech, schodech a nerovném terénu.
   Mannequin musí být ve směru pohledu a držené předměty se nesmí zobrazit dvakrát.
2. Ověř dosednutí modelu na povrch s offsetem `0.20` bez průniku či levitování a odstranění sedadla přes `/nekararpg stand`.
3. Sedni znovu a použij běžnou klávesu sesednutí; neviditelné sedadlo musí zmizet.
4. Teleport, smrt, odpojení, poškození, reload a shutdown nesmí zanechat armor stand.
5. S CMI ověř nezměněný hlavní `/sit`. NekaraRPG registruje jen `/nekararpg sit` a `/nrpg sit`.
6. Použij `/cmi sit` nebo jeho alias u ohně a ověř spuštění Campfire bez příkazu NekaraRPG.
7. Ověř, že samostatný přepínač `modules.sitting.enabled` už neexistuje a vypnutí
   Campfire uklidí interní sedadlo i ležení.
8. Ulehni bez ohně a ověř trvalou spánkovou pózu bez Rested. Pohyb musí být
   zablokovaný, rozhlížení povolené a přikrčení hráče zvedne. Teleport, smrt,
   odpojení či nakonfigurované poškození musí pózu také uklidit.
9. Ověř stejná tlačítka sezení a ležení přímo na hlavní obrazovce `/nrpg` i v
   Tábořišti. Každé použití musí zůstat ve správné obrazovce GUI.
10. Se dvěma hráči noc nesmí přeskočit. Po odpojení druhého smí jediný ležící hráč
   po pěti sekundách přejít do rána; CMI příkaz se nesmí spustit a spawn se nezmění.

## Akceptace Campfire

1. Zraň hráče, sniž hlad a sedni do výchozího kulového radiusu pěti bloků od zapáleného ohně.
2. Ověř léčení v intervalu, neklesající hlad, pomalé doplňování a částice aktivního odpočinku.
3. Uhas nebo rozbij oheň a ověř konec odpočinku při další aktualizaci.
4. Opakuj se zapáleným soul campfire a ověř stejné chování.
5. Sedni mimo radius a ověř, že se efekty nespustí.
6. U holého ohně dokonči skutečné nabíjení. Ověř jednorázovou Rested zprávu, jemný chime a text `Odpočatý | m:ss`, ale žádný Haste ani bossbar. Nesmí se zobrazit surový klíč zprávy.
7. Odejdi od holého ohně a vyvolej pokles hladu; i s Rested musí klesat běžnou rychlostí.
8. Přidej smoker do pěti bloků, znovu nabij Rested a po odchodu ověř nastavený násobitel hladu.
9. Přidej crafting table, znovu nabij a ověř minutu navíc, Haste I a potion ikonu.
10. Přidávej ostatní prvky a ověř minutu za unikátní typ bez efektu duplicit.
11. Pro rychlou expiraci zkrať doby; časovač, snížení hladu a řízený Haste musí skončit.
12. Před odpočinkem dej hráči silnější Haste a ověř, že ho NekaraRPG nenahradí ani neodstraní.

## Akceptace bezpečného tábora

1. Polož bed do pěti bloků od zapáleného ohně a pohybuj se kolem výchozího radiusu 24 bloků.
2. Nech v noci probíhat přirozené spawny. Vanilla nepřátelé nesmí vzniknout uvnitř, ale mohou vejít zvenčí.
3. Nech chunky odnačíst, později se vrať a ověř obnovenou ochranu beze změny tábora.
4. Uhas oheň nebo přesuň bed mimo radius prvků a ověř návrat přirozených spawnů.
5. S MythicMobs ověř blokování přirozených `NekaraHostile` a pokračující `NekaraFauna`.
6. Vyvolej command, summon, quest nebo boss spawn uvnitř a ověř, že se nezruší.

## Akceptace skupiny a kompatibility

1. Posaď dva hráče u stejného ohně a ověř dva hráče a násobitel `1.15x` v action baru.
2. Porovnej léčení za stejný interval s jedním a dvěma hráči.
3. Přesuň jednoho k jinému ohni; obě skupiny se musí vrátit na `1.00x`.
4. `/nekararpg status` musí hlásit správné počty seated, resting a Rested.
5. S ValhallaMMO porovnej běžná a Rested XP více skillů; běžná skill-action a sdílená XP mají být přesně `1.10x`.
6. Dokonči rybaření s Rested. Původní loot zůstane a odložená XP dostanou 10 % právě jednou.
7. Uděl ValhallaMMO XP administrativním příkazem a ověř, že se nenásobí.
8. Vypínej moduly jednotlivě. Vypnutý Campfire nesmí přijmout interní ani externí
   sezení pro Rested a `/nrpg sit` nesmí vytvořit sedadlo.
9. Spusť rybářskou minihru při Rested a ověř, že její UI dočasně nahradí Rested časovač.

## Akceptace Echo Vein

1. Použij `/nekararpg test vein` v různých jeskyních. Cíl musí být viditelný, dosažitelný, jen pro testujícího hráče a omezený na stone, deepslate, netherrack nebo end stone.
2. Ověř jemný pulz v okolí jednoho bloku a výrazně hustší viditelnou stěnu. Ores, dirt, gravel ani wood nesmí být cílem či spouštěčem.
3. Nastav trigger na `1.0`, vytěž hostitelský blok s novým i zkušeným profilem a ověř start bez level gate až po původních odměnách.
4. Přirozený chat zůstává prázdný, hlubší chime objevení zazní jednou a objeví se časovač.
5. Vytěž jiné bloky; cíl i časovač zůstávají. Vytěž cíl a bez chainu ověř jeden zvuk úspěchu.
6. Zapni `plugin.debug`. Porovnej `markedBlockXp`, `bonusXp` a profil: bonus je jednou, 25 % finální hodnoty cíle a důvod `PLUGIN`.
7. Bonusový item je přesně jeden item z finálního přirozeného nebo Valhalla-prepared výstupu cíle včetně metadat, bez nového Fortune hodu. Testovací příkaz nedává nic.
8. Vynuť ore reveal `1.0` a testuj Y 70, Y 15, Y -32 a Y -64. Každý výsledek musí patřit do vanilla-kompatibilního pásma; vysoko nesmí diamond ani redstone.
9. Porovnej běžný biom a Badlands na Y 70. Testuj netherrack těsně uvnitř a vně Y 10-117. End stone se nemění.
10. Úspěšná přeměna přehraje jednou jasnější ametystový zvuk, zřetelně jiný než objevení žíly. Opakované poškození ho neopakuje.
11. Vynuť chain `1.0`, vytěž cíl vedle viditelného hostitele a ověř sousední pokračování s vlastním časovačem a odměnami. Poté vrať `0.50`.
12. Nech pokus vypršet a ověř tichý timeout bez chatu.
13. Testuj Rested, Silk Touch, plný inventář, odpojení, smrt, teleport a prioritu rybaření.
14. Ověř nový `modules.mining.enabled` i fallback starého `modules.echo-vein.enabled`.
15. Vrať defaults (`0.05`, bez cooldownu, `0.50` chain, `0.25` ore reveal) a ověř nezávislé po sobě jdoucí bloky.
16. Vypni ValhallaMMO nebo Mining skill. NekaraRPG musí bezpečně nastartovat, automatická Echo Vein zůstane nedostupná.

## Akceptace NekaraMounts

1. Vytvoř virtuálního koně přes GUI, odvolej ho a přivolej píšťalkou; porovnej celý
   stav před a po a ověř tučné jméno.
2. Opakuj po plném restartu a po unloadu chunku. Vždy spočítej entity a ověř jediný
   mount ID bez duplicitního sedla nebo brnění.
3. Po doběhnutí sleduj přirozené putování v pětiblokovém okruhu. Přesuň se a znovu
   pískni: aktivní kůň má změnit cíl, třísekundová ochrana má brzdit spam a druhá
   entita nesmí vzniknout. Zopakuj s aktivní entitou v nenačteném chunku.
4. Ověř cizí interakce a vstup do horse inventory druhým hráčem.
   Píšťalku zkus zahodit, uložit do truhly a sebrat druhým hráčem; nesmí opustit
   vlastníka. Ověř také `mount whistle remove` a `mount whistle restore`.
5. Způsob PvP zásah, reconnect a restart v průběhu combat okna. Summon i dismiss
   zůstávají blokované do uloženého času.
6. Nový kůň musí mít sedlo. Nasaď brnění a obyčejnou truhlu, naplň 54slotové
   brašny přes GUI a zabij koně. Gear ani obsah nesmí vypadnout a po minutě se
   vrátí právě jedna uložená sada. Po smrti hráče se píšťalka vrátí do inventáře.
7. Ověř spawn 7-12 bloků daleko, doběhnutí a čekání u místa písknutí. Opakuj v
   úzkém interiéru, na hraně, ve vodě a v povoleném i zakázaném světě.
8. Vypni modul a simuluj selhání zápisu pouze na kopii serveru. Při úspěchu se stav
   uloží a entita uklidí; při chybě se entita ponechá a další operace uzamknou.
9. Ověř persistence brašen přes odvolání, reload a restart, zákaz vyjmutí plné
   truhly, zákaz píšťalky a shift-clicku a odmítnutí přístupu cizímu hráči.
10. V menu klikni na prázdný armor slot, výplně a citlivé akce. Prázdný slot nesmí
    hlásit uložení, výplně nezavírají GUI a odvolání i odebrání píšťalky vyžaduje
    potvrzení.
    Potom ve spodním inventáři běžně klikni na sedlo, truhlu a koňské brnění;
    každý item se musí nasadit jedním kliknutím a nahrazená výbava vrátit na kurzor.
11. Na kopii serveru spusť upgrade s původním `mounts/data.yml`. Zkontroluj
    databázi i zálohu, stejné vlastníky, mount ID, výbavu, brašny a combat okna.
    Druhý restart nesmí nic znovu importovat ani duplikovat.
12. Zablokuj koni přímou trasu vhodnou překážkou. Po zhruba šesti sekundách musí
    zkusit bezpečný vedlejší přístup, ale nesmí se teleportovat ani vzniknout
    druhá entita.

## Akceptace updateru

Použij jednorázový staging release novější než instalovaná verze. Jeho jediný
asset musí být stabilní `NekaraRPG.jar` vytvořený `build-release.cmd`.

1. Spusť `/nekararpg update check`; příkaz odpoví ihned a download pokračuje asynchronně.
2. Konzole ohlásí připravenou verzi a operátor s `nekararpg.update.notify` dostane české upozornění na restart.
3. Stažený soubor musí být v update složce Paperu, ne vedle aktivního JARu.
4. Spusť `/nekararpg update status`, znovu připoj operátora a ověř stejnou čekající verzi.
5. Server běžně zastav a spusť. Staging soubor se spotřebuje, zůstane jediný aktivní `NekaraRPG.jar` a status hlásí novou verzi.
6. Zkontroluj startup logy a před produkcí zopakuj smoke testy Fishing, Campfire
   (včetně sezení a ležení), Echo Vein, ValhallaMMO a MythicMobs.

Chybové režimy updateru nikdy netestuj na produkci. Pro špatné digesty, identity,
velikosti a verze použij staging release nebo dočasnou repository fixture.
