# Projektová paměť Nekara Plugins

Tento soubor uchovává dlouhodobá rozhodnutí a jejich důvody. Aktualizuj ho,
pokud budoucí release některé z těchto rozhodnutí záměrně změní.

## Podoba produktu

- `NekaraRPG` je centrální plugin pro propojené hráčské RPG a imerzivní systémy.
- Funkce patří do NekaraRPG, pokud sdílí hráčský stav, životní cyklus,
  konfiguraci, příkazy nebo integrace. Skutečně nezávislý systém může vzniknout
  jako samostatný plugin v tomto repozitáři.
- Doménové moduly musí zůstat samostatně zapínatelné, i když se distribuují v
  jednom JARu. Sezení a ležení patří do Campfire, nejsou samostatným modulem.
- Nevytvářej znovu vyspělé systémy, které vlastní CMI, MythicMobs, Lands nebo
  jiný produkční plugin. Výjimkou je výslovně schválená nativní skill platforma
  NekaraRPG 2.0, která postupně a kontrolovaně nahradí ValhallaMMO.

## Kompatibilitní smlouvy

- Rybaření musí zachovat serverem vytvořený úlovek, metadata itemu, vanilla XP,
  profesní XP ValhallaMMO a volitelné extra dropy. Nevytvářej náhradní catch
  event a nepřepočítávej odměny jiného pluginu.
- CMI vlastní `/sit`. NekaraRPG poskytuje `/nekararpg sit` a bez pevné závislosti
  na CMI rozpoznává externí sedadla založená na vehicle entitách.
- MythicMobs je měkká závislost. Bezpečné tábory filtrují pouze náhodné
  přirozené spawny nepřátelské frakce `NekaraHostile`; fauna a skriptované spawny
  zůstávají beze změny.
- Bezpečné tábory nikdy nemažou existující nepřátele a nebrání jim do tábora vejít.
- Integrace ValhallaMMO a MythicMobs musí při chybějícím pluginu selhat bezpečně.

## Pravidla Campfire a Rested

- Hráč odpočívá při sezení nebo ležení u nejbližšího zapáleného campfire.
- Ležení používá pevnou spánkovou pózu bez falešné postele, změny spawnu nebo
  volání CMI. Noc smí přeskočit jen jediný online hráč v Overworldu po
  nastaveném čekání; příchod dalšího hráče čekání zruší, ale ležení ponechá.
- Čas používá skutečné sekundy, aby lag serveru neprodlužoval nabíjení.
- Základ Rested trvá pět minut po 20sekundovém nabíjení.
- Kvalita tábora počítá unikátní typy prvků, ne množství bloků. Každý zapnutý
  typ přispívá jednou, aktuálně jednou minutou.
- Crafting table řídí Haste I; efekt se nesčítá a nepřepisuje silnější externí efekt.
- Smoker řídí sníženou ztrátu hladu po opuštění tábora; samotný Rested hlad nezpomaluje.
- Rested přidává nastavitelné bonusové XP ke všem ValhallaMMO skillům pro běžné
  skill akce a sdílené XP. Administrativní a obnovovací XP se nemění.
- Po aktivaci nativních skillů musí Rested přejít na jediný Nekara Skills XP
  adaptér. Během migrace nesmí stejnou akci násobit současně v NekaraRPG i
  ValhallaMMO.
- Odložené rybářské XP musí získat násobitel Rested právě jednou.
- Bed řídí ochranu proti přirozenému spawnu nepřátel v širším radiusu tábora.
- Skupinové škálování ovlivňuje léčení a doplňování hladu s nastaveným stropem;
  nenásobí délku Rested ani bonusy vybavení.
- Rested používá text v action baru, aby zůstal bossbar zdraví MythicMobs
  jednoznačný. Rybářská minihra a nabíjení odpočinku mají vyšší prioritu.

## Pravidla NekaraMining a Echo Vein

- NekaraMining používá ID modulu `mining`. Echo Vein je jeho první volitelná
  ValhallaMMO aktivita a je dostupná na každé úrovni. Vyžaduje skutečný
  nezrušený Mining XP event s důvodem `SKILL_ACTION`.
- Automatické spuštění a cíle jsou omezené na stone, deepslate, netherrack a end
  stone v Paper tagu `MINEABLE_PICKAXE`. Těžba jiného bloku aktivitu neruší.
  Přirozené pokusy nepíšou do chatu a timeout je tichý.
- Původní Mining akce se vždy dokončí před začátkem výzvy; neúspěch její XP ani
  dropy neodebírá a nemění.
- Vytěžení označeného cíle přidá nastavitelnou část finálních Mining XP tohoto
  bloku s důvodem `PLUGIN`. Mining, Rested ani globální násobitele se proto
  neaplikují podruhé.
- Volitelný bonusový drop je přesně jeden item vybraný podle množství a
  naklonovaný z finálních přirozených a Valhalla-prepared dropů cílového bloku.
  Echo Vein nepoužívá Digging tabulku ani vlastní loot.
- Aktivita nemá cooldown. Dokončený cíl má nastavitelnou šanci pokračovat na
  viditelný blok sousedící stěnou.
- První poškození cíle může právě jednou odhalit váženou vanilla rudu. Kandidáti
  respektují vanilla rozsahy Y a relativní výškové váhy, včetně Badlands zlata
  a netherového rozsahu Y 10-117. Seedový noise ani potlačení rud vystavených
  vzduchu se nesimulují. End stone se nemění.
- Objevení žíly, úspěšné odhalení rudy a finální dokončení používají odlišné
  související zvuky. Rybaření má před aktivní Echo Vein přednost.
- Pokud chybí `modules.mining.enabled`, zachovej hodnotu starého
  `modules.echo-vein.enabled`. Nastavení aktivity zůstává pod `echo-vein`.

## Směr modulu Mounts

- NekaraMounts je implementovaná součást NekaraRPG 1.8.0. Konkrétní rozsah,
  bezpečnostní pravidla, uzavřená rozhodnutí a akceptace jsou v `ROADMAP.md`.
- Modul `mounts` používá vanilla koně, jednoho mounta na hráče, výchozí sedlo a
  volitelnou obyčejnou truhlu s virtuálními 54slotovými brašnami.
- Sedlo, brnění, jméno, zdraví, cooldown po smrti a vlastnictví musí být trvalé.
  Přivolání a odvolání nesmí sloužit jako únik nebo léčení v PvP.
- Kůň vzniká virtuálně přes GUI pro barvu a jméno, bez hledání a ochočování.
  Administrátorský grant otevře stejný průchod. NekaraRPG nebude vytvářet vlastní
  questovou mechaniku; případné udělení v příběhu vlastní BetonQuest.
- Svázaná píšťalka má perzistentní cooldown, spawn 7-12 bloků od hráče a navede
  koně k místu písknutí. Je soulbound, nedropuje a obnova udržuje jedinou nalezenou
  kopii. Smrt koně používá minutový cooldown bez ceny a bez dropu výbavy.
- PvP používá vlastní perzistentní combat okno. Stabilní identita vlastníka je
  normalizovaný nick chráněný NekaraAuth; poslední známé UUID je doplňkové.
- `MountRepository` je hranice storage. Aktivní backend je transakční SQLite s
  WAL; starý YAML slouží jen k jednorázovému importu. Původní soubor i samostatná
  záloha zůstávají zachované. Modul při chybě selže uzavřeně.

## Hranice BetonQuest a skill platformy

- BetonQuest vlastní questy, podmínky, dialogy a příběhové odměny. NekaraRPG může
  nabídnout úzký bezpečný vstup pro existující funkci, ale nevytváří paralelní
  quest engine ani vlastní questová GUI.
- NekaraRPG 2.0 bude vlastnit skilly, jejich postup, perk stromy, statistiky a
  bezpečné aktivní schopnosti. Do dokončení migrace zůstává ValhallaMMO pouze
  dočasným produkčním zdrojem postupu; nesmí se zapnout dvojí udělování XP ani
  odměn.
- Implementace je clean-room. Z ValhallaMMO, AuraSkills ani mcMMO se nepřebírá
  zdrojový kód, konfigurace, texty, hodnoty, názvy perků nebo rozložení stromů.
  Veřejné zdroje slouží pouze jako žánrová a integrační reference.
- Powerlevel je odvozený průměr patnácti trénovaných dovedností. Poskytuje
  společné perk pointy a obecné milníky; mount může být odemknutelný na Power 50,
  zatímco příběhové podmínky a udělení nadále vlastní BetonQuest.
- Změny Mounts statistik za economy a questové získávání nejsou aktuální rozsah.

## Pravidla NekaraAuth

- NekaraAuth je modul `auth` uvnitř `NekaraRPG.jar`, ale jeho bezpečnostní
  listenery se registrují před herními moduly.
- Cílí na offline-mode server. Registrovaný nick se hledá bez ohledu na velikost
  písmen, uložený přesný zápis se však výchozím stavem vynucuje kvůli offline UUID.
- Hesla se nikdy neukládají ani nelogují v plaintextu. Výchozí formát je
  PBKDF2-HMAC-SHA256 s unikátní solí, 600 000 iteracemi a work factorem v hashi.
- Nedostupné úložiště musí selhat uzavřeně a odmítnout přihlášení. Hashování je
  mimo Bukkit main thread a neomezený paralelní výpočet hesel není povolen.
- První backend je `auth/accounts.yml` za `AccountRepository`. Databáze a webové
  propojení tuto hranici zachovají; Minecraft heslo se nemá veřejně předávat webu.
- Krátkodobá relog session je jen v paměti, váže normalizovaný nick na IP adresu
  a ruší se při logoutu, kicku, zrušení účtu, reloadu i restartu. Na proxy bez
  správně předané skutečné IP se musí vypnout.
- `/login` a `/register` jsou pro běžné hráče vypnuté. Nouzový fallback vyžaduje
  současně zapnutou konfiguraci a oprávnění `nekararpg.auth.fallback-commands`.
- Změna hesla vyžaduje současné heslo, nové heslo a jeho potvrzení. Běží přes stejné
  omezené asynchronní hashování jako login, po úspěchu ruší relog session a nesmí
  pokračovat, pokud se autentizační stav hráče během výpočtu změnil.
- První registrace si nick nárokuje. Počáteční nasazení proto používá whitelist,
  dokud nejsou vytvořené účty administrátorů a rezervovaných nicků.
- AuthMe se nesmí odstranit před řízenou migrací existujících účtů a živou
  akceptací NekaraAuth na offline-mode staging serveru.

## Konfigurace a upgrady

- Kořenový `config.yml` obsahuje pouze jádro, updater a přepínače modulů.
- Centrální hráčské GUI `/nekararpg` zobrazuje pouze zapnuté moduly dostupné podle
  oprávnění. Modulová GUI a příkazy zůstávají vlastníky své doménové logiky.
  Podrobné hodnoty patří do `<module>/config.yml`; současné moduly používají
  složky `auth`, `fishing`, `campfire`, `mining` a `mounts`. Starý
  `sitting/config.yml` se jednou převezme pod `campfire.sitting` a zůstane jako
  záloha.
- Centrální GUI poskytuje osobní přehled a oprávněnou administrátorskou
  diagnostiku. Běžná krátká zpětná vazba patří do action baru a delší vysvětlení
  do lore GUI; technické detaily patří do konzole.
- Přechod ze starého monolitického configu musí nejprve úspěšně uložit vlastní
  hodnoty do modulových souborů a teprve potom odstranit staré sekce. Existující
  modulový config má při opakovaném startu přednost a nesmí se přepsat.
- Zabalené výchozí hodnoty musí pokrýt chybějící klíče ve stávajících
  `config.yml` a `messages.yml`; běžný upgrade nesmí vyžadovat smazání složky pluginu.
- Zachovej uživatelsky upravené hodnoty a zprávy. Migruj pouze známé
  předprodukční výchozí hodnoty, pokud to vyžaduje kompatibilita.
- Hráčské texty musí být konfigurovatelné a mít zabalený fallback, aby se nikdy
  nezobrazil surový klíč zprávy.
- Používej typované konfigurační recordy a validuj číselné limity i názvy enumů.

## Release pravidla

- Cílový server je Purpur/Paper 26.1.2 na Javě 25.
- Nasazovaný plugin se vždy jmenuje `NekaraRPG.jar`.
- Nikdy necommituj `build/`, `dist/`, přihlašovací údaje, serverové soubory,
  cache ani lokální certificate truststore.
- Release vyžaduje odpovídající sémantickou verzi a nadpis changelogu, čistý
  release build, úspěšné testy, ověření verze uvnitř pluginu, Git tag a GitHub release.
- Verze používají `MAJOR.MINOR.PATCH`: patch je menší změna nebo bugfix, minor
  přidává funkce a major (`2.0.0`) je pro významné systémy či nekompatibilní změny.
- GitHub release obsahuje pouze stabilně pojmenovaný plugin JAR, pokud není
  výslovně schválený další asset.
- Každý release musí současně aktualizovat kořenové `HANDOFF.md`,
  `PROJECT_MEMORY.md` a `ROADMAP.md` podle skutečně vydaného stavu.
- Plugin se na serveru nahrazuje při vypnutém serveru a v `plugins` musí být
  právě jeden NekaraRPG JAR. Bukkit `/reload` není způsob nasazení.
- NekaraRPG 1.2.0 a novější mohou připravit ověřený stabilní GitHub release do
  aktualizační složky Paperu. Instalace proběhne až při úplném restartu; plugin
  nikdy nenahrazuje svůj aktivní JAR ani nerestartuje server.
- Updater důvěřuje pouze `Jonaczech/nekara-plugins`, přesnému assetu
  `NekaraRPG.jar`, GitHub SHA-256 metadatům a odpovídající identitě a sémantické
  verzi JARu. Síťová práce musí zůstat asynchronní a při chybě nic nenasadit.

## Preference spolupráce

- Pracuj a komunikuj s uživatelem česky.
- Požadované změny implementuj a ověř až do konce; nekonči jen návrhem, pokud
  uživatel výslovně nechce pouze nápady nebo plán.
- Před přechodem na další modul nebo plugin udržuj dokumentaci a handoff aktuální.
