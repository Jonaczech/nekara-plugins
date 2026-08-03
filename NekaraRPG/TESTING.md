# Testovací příručka NekaraRPG

## Automatické kontroly

Spusť ověřený release postup:

```text
scripts\build-release.cmd
```

Unit testy pokrývají hashování hesel, neplatné hash formáty, auth lockout,
pohyb indikátoru a odraz na hranách, hranice cíle, zásahy,
chyby, timeout, přechody stavů, ochranu před dvojím dokončením, fallback
konfigurace, skupinové škálování Campfire a částečné snížení hladu Rested.
Samostatné čisté testy ověřují způsobilost a škálování ValhallaMMO Rested XP.
Echo Vein testuje hody šance, XP bonus, vážený výběr dropu, vanilla-kompatibilní
výškové hranice rud, Badlands zlato a Nether rozsah. Updater testuje sémantické
verze, parsování GitHub release, důvěryhodný asset, SHA-256, identitu JARu a
vloženou release verzi. NekaraMounts navíc testuje normalizaci offline identity,
hranice a formát cooldownu, atomický YAML round-trip a perzistenci combat okna.
NekaraAuth navíc ověřuje, že výměna hashe hesla zachová identitu a auditní údaje
účtu; resource test kontroluje zprávy a výchozí oprávnění centrálního menu.

## Ruční akceptace na Purpur 26.1.2

Nejdřív použij čistý testovací server s Javou 25, cílovým serverem a pouze
NekaraRPG. Testovacímu hráči dej potřebná oprávnění a umísti ho do povoleného světa.

### 1. Start a konfigurace modulů

1. Nainstaluj jediný release artefakt `NekaraRPG.jar`.
2. Spusť server a ověř vznik kořenového `plugins/NekaraRPG/config.yml` a souborů
   `auth/config.yml`, `fishing/config.yml`, `sitting/config.yml`,
   `campfire/config.yml`, `mining/config.yml` a `mounts/config.yml`.
3. Ověř výchozí hodnotu true u `modules.fishing.enabled`, `modules.sitting.enabled`, `modules.campfire.enabled`, `modules.mining.enabled` a `modules.mounts.enabled`.
4. Ověř, že kořenový config neobsahuje detailní sekce modulů. Spusť
   `/nekararpg status` a ověř zobrazení všech pěti modulů.
5. Nastav `modules.fishing.enabled: false`, spusť `/nekararpg reload` a ověř, že rybářská minihra nezačne.
6. Modul znovu zapni a proveď další reload.
7. Upgraduj kopii starého monolitického configu s několika změněnými hodnotami.
   První start je musí přenést do správných modulových souborů a odstranit staré
   detailní sekce až po úspěšném zápisu. Druhý start nesmí vlastní hodnoty přepsat.

### 1a. Centrální menu a účet

1. Jako běžný přihlášený hráč spusť `/nekararpg` a ověř otevření centrálního GUI.
2. Vypínej jednotlivé moduly v `config.yml` a používej `/nekararpg reload`; vypnutá
   dlaždice se po novém otevření nesmí zobrazit.
3. Odeber hráči oprávnění k Sitting nebo Mounts. Příslušná dlaždice se nesmí
   zobrazit ani být použitelná přes staré otevřené menu.
4. Kliknutím na účet otevři NekaraAuth, klikni na změnu hesla a postupně zadej
   současné heslo, nové heslo a stejné nové heslo znovu.
5. Odhlas se. Staré heslo musí být odmítnuto a nové musí přihlášení povolit.
6. Opakuj s chybným současným heslem a s neshodným potvrzením. Hash účtu se nesmí
   změnit a chybné současné heslo se započítá do stejného lockoutu jako login.
7. Během asynchronního ověření proveď logout nebo odpojení. Rozpracované GUI se
   nesmí znovu otevřít a zápis změny se nesmí zahájit v neplatném autentizačním stavu.
8. Ověř, že konzole, chat a `auth/accounts.yml` nikde neobsahují plaintext hesla.

### 2. Běžné úspěšné rybaření

1. Nahoď prut.
2. Počkej na skutečný záběr.
3. Jednou klikni pravým tlačítkem, čímž vznikne skutečný catch event a začne minihra; ověř, že item zatím nebyl doručen.
4. Dokonči nastavený počet zásahů a ověř, že každý úspěch vrací nastavený časový bonus.
5. Po finálním zásahu ověř doručení přesně jednoho původního vanilla úlovku.
6. Ověř zachování vanilla XP a samostatný zvuk úlovku oproti zvuku dokončení minihry.
7. Ověř, že action bar nemá popisek `FISH` a BossBar nad ním se po každém úspěchu plní.
8. Opakuj dost relací, aby se objevily různé počty požadovaných zásahů od 3 do 5.
9. Po každém úspěchu ověř přitažení skutečného splávku bez průchodu pevnou stěnou;
   `hook-pull-distance: 0` pod `minigame` ve `fishing/config.yml` musí pohyb vypnout.

### 3. Škálování rybaření podle ValhallaMMO

1. Ověř instalaci ValhallaMMO a `valhalla.fishing-difficulty.enabled: true`.
2. Zapni debug, spusť minihru a ověř v konzoli FishingSkill level a účinné hodnoty zásahů/chyb.
3. Porovnej levely 1-30, 31-60 a 61+. Mají dostat nakonfigurované hodnoty 3-5/1 chyba, 3-4/2 chyby a 2-3/3 chyby.
4. Hráč na skutečném ValhallaMMO max levelu musí dostat přesně 2 zatažení a 3 chyby.
5. Ověř, že původní vanilla/ValhallaMMO loot a fishing XP zůstaly stejné.

### 3a. ValhallaMMO Rested XP bonus

1. Ověř ValhallaMMO a `rested.valhalla-experience.enabled: true` v
   `campfire/config.yml`.
2. Získej stejné skill-action XP bez Rested a s Rested; Rested hodnota má být výchozím stavem přesně `1.10x`.
3. Opakuj s více skilly včetně Fishing a jednoho combat nebo gathering skillu. Bonus nesmí filtrovat podle typu.
4. Získej sdílená party XP a ověř stejný bonus.
5. Uděl XP administrativním příkazem a ověř, že se nenásobí.
6. Dokonči rybářskou minihru s Rested a ověř jeden 10% bonus odložených XP, ne dva.
7. Vypni Campfire nebo Rested XP nastavení a ověř návrat běžných ValhallaMMO XP.

### 3b. Echo Vein

1. Ověř zapnutý ValhallaMMO Mining. Testuj nový i zkušený profil; level gate neexistuje.
2. V jeskyni spusť `/nekararpg test vein`. Ověř jemný pulz v okolí, hustší šestisekundové označení viditelné stěny a informaci, že test nedá odměnu.
3. Udeř do pulzujícího bloku krumpáčem a ověř úspěch bez odměny. Opakuj úderem do jiného bloku a timeoutem; oba mají čistě selhat.
4. Opakuj kolem stone, deepslate, netherrack, end stone, ores, dirt, gravel a wood. Cílem smí být jen čtyři hostitelské typy.
5. Dočasně nastav `echo-vein.trigger-chance: 1.0` v `mining/config.yml`, reloaduj
   a vytěž hostitelský blok s Mining XP. Těžba rudy nesmí Echo Vein spustit.
6. Ověř doručení původního bloku a všech běžných Valhalla dropů před startem ozvěny. Chat zůstává prázdný a zvuk objevení zazní jednou.
7. Vytěž nebo udeř do několika jiných bloků. Cíl i časovač musí zůstat; potom označený blok skutečně vytěž.
8. S debugem ověř `markedBlockXp`, `bonusXp`, `xpGranted` a změnu profilu. Bonus je právě jednou a přesně 25 % finálních Mining XP označeného bloku.
9. Ověř nejvýše jeden bonusový item z finálních dropů cíle, zachování metadat a žádný nový Fortune hod.
10. Nastav `ore-reveal.chance: 1.0`. Na Y 70 smí stone odhalit jen coal, copper nebo iron; diamond, redstone, lapis a běžné gold jsou nemožné.
11. Opakuj pod Y 16 a kolem Y -64. Diamond/redstone se stanou možnými a hlouběji mají vyšší váhu. Copper je možný na Y 48, ne na Y 97.
12. Testuj Y 70 v běžném biomu a Badlands. Vysoké gold patří jen Badlands. Netherrack testuj na Y 9, 10, 117 a 118; Nether ruda je možná jen Y 10-117. End stone se nemění.
13. Ověř hlubší chime objevení žíly a jediné přehrání jasnějšího zvuku při přeměně na rudu. Opakované poškození nesmí znovu házet ani přehrávat zvuk.
14. Nastav `chain-chance: 1.0`, dokonči cíl vedle viditelného hostitelského bloku a ověř pokračování na souseda stěnou. Dokončený blok nesmí současně spustit samostatnou základní ozvěnu.
15. Nech přirozenou ozvěnu vypršet a ověř tichý timeout bez zprávy.
16. Opakuj s Rested. XP cíle obsahují Rested bonus, ale 25% Echo odměna se už znovu nenásobí.
17. Vrať defaults (`0.05`, bez cooldownu, `0.50` chain, `0.25` ore reveal) a ověř nezávislé po sobě jdoucí bloky.
18. Položené bloky nebo bloky bez Valhalla Mining XP nesmí aktivitu spustit.
19. Během ozvěny začni rybařit a ověř, že Echo Vein ustoupí bez narušení rybaření.
20. Odstraň `modules.mining.enabled`, nastav staré `modules.echo-vein.enabled: false`, reloaduj a ověř vypnutý NekaraMining. Poté přidej nový klíč a ověř jeho prioritu.

### 3c. NekaraMounts

1. Spusť `/nekararpg mount`, vyber barvu a platné jméno. Musí vzniknout virtuální
   záznam a svázaná píšťalka; druhé vytvoření musí být odmítnuto.
2. Pravým kliknutím píšťalkou přivolej koně. Musí se objevit 7-12 bloků daleko,
   doběhnout na místo písknutí a tam čekat. Jméno nad entitou musí být tučné.
3. Použij píšťalku dvakrát rychle. Druhé použití musí oznámit 30sekundový cooldown
   a na serveru smí existovat právě jedna entita se stejným mount ID.
4. Zkus píšťalku zahodit, shift-clicknout do truhly, vložit do Ender Chest a sebrat
   druhým hráčem. Vše musí být odmítnuto. `mount whistle remove` ji odebere a
   `mount whistle restore` vydá právě jednu; při plném inventáři nic nesmí spadnout.
5. Odvolej koně, restartuj server a znovu ho přivolej. Porovnej jméno, zdraví,
   maximální zdraví, rychlost, skok, barvu, styl, sedlo a brnění.
6. Nech aktivnímu koni unloadnout chunk a vrať se. Starý záznam entity nesmí vydat
   vybavení ani vytvořit kopii; odvolaný mount se musí dát bezpečně přivolat.
7. Přes GUI změň jméno, barvu, sedlo a brnění. Nech cizího hráče zkusit nasednout,
   otevřít inventář, přejmenovat a měnit výbavu. Vše musí být bezpečně odmítnuto.
8. Způsob PvP zásah mezi dvěma hráči. `create`, `grant`, `call` i `dismiss` musí být
   15 sekund blokované. Odpoj a připoj hráče; zbývající blokace musí pokračovat.
9. Zabij mounta a ověř minutový cooldown i návrat stejné výbavy. Opakuj se smrtí
   hráče: píšťalka nesmí dropnout a po respawnu se musí vrátit do inventáře.
10. Vypni `modules.mounts.enabled` a reloaduj. Aktivní entita zmizí až po úspěšném
   uložení, `mounts/data.yml` zůstane a ostatní modulové configy se nezmění.
11. Znepřístupni kopii storage pouze na stagingu. Operace musí selhat uzavřeně,
    aktivní entita se nesmí smazat a konzole musí ohlásit uzamčení modulu.

### 4. Neúspěšná minihra

1. Nahoď a počkej na záběr.
2. Chybuj do překročení limitu nebo počkej na timeout.
3. Ověř zpětnou vazbu o úniku/timeoutu.
4. Ověř, že nevznikl item ani náhradní loot a počet relací se vrátil na nulu.
5. Ověř krátký efekt částic kolem splávku a následné vyčištění splávku i relace.

### 5. Odpojení

1. Spusť minihru.
2. Odpoj hráče.
3. Ověř odstranění relace bez výjimky v konzoli.

### 6. Teleport

1. Spusť minihru.
2. Teleportuj hráče na jiné místo nebo do jiného světa.
3. Ověř zrušení relace, odstranění háčku a návrat počtu relací na nulu.

### 7. Reload

1. Spusť minihru.
2. Spusť `/nekararpg reload`.
3. Ověř vyčištění relace a nové načtení konfigurace/zpráv.
4. Spusť další minihru a ověř jediný proud action baru bez duplicitních zvuků.

### 8. Dva hráči

1. Nech dva hráče nahodit přibližně současně.
2. Ověř nezávislé action bary, počítadla, cíle, kliknutí a výsledky.

### 9. Vlastní zvuk

1. Nastav platné resource-pack ID, například `nekara:fishing.hit`.
2. Načti resource pack se zvukem a ověř přehrání.
3. Nastav neplatné ID, reloaduj a ověř varování v logu bez pádu pluginu.

### 10. Jiný plugin mění loot

1. Nainstaluj testovací plugin měnící ItemStack nebo XP existujícího `CAUGHT_FISH` eventu.
2. Dokonči rybaření NekaraRPG.
3. Ověř, že NekaraRPG nenahradí `Item`, nezmění metadata, nepřidá druhý item ani nezmění XP.
4. Zrušený `CAUGHT_FISH` nesmí vytvořit zprávu ani zvuk úspěšného úlovku.

## Další okrajové případy

Ověř také duplicitní interakce off-hand, změnu drženého itemu, zahození prutu,
otevření inventáře, smrt, spectator, creative, rozbitý prut, chybějící háček,
chycenou entitu, již zrušený fishing event a vypnutí serveru.

### 11. Sezení

1. Spusť `/nekararpg sit` a ověř sedící pózu na aktuální uzemněné pozici.
2. Model hráče se musí dotýkat podkladu bez průniku nebo viditelného levitování.
3. Spusť `/nekararpg stand` a opakuj běžnou klávesou sesednutí.
4. Teleport, smrt, odpojení, poškození, reload a shutdown musí sedadlo odstranit.
5. Sezení musí čistě selhat při létání, plavání, glidingu, spánku nebo jízdě na jiné entitě.
6. S CMI ověř nezměněný hlavní `/sit`; NekaraRPG používá jen svůj subcommand.
7. Použij `/cmi sit` a ověř externě sedícího hráče v `/nekararpg status`.

### 12. Odpočinek u ohně

1. Sniž zdraví a hlad, sedni do radiusu zapáleného ohně a ověř pomalé léčení.
2. Hlad během odpočinku neklesá, v intervalu roste a zobrazují se částice.
3. Uhas nebo rozbij oheň; aktivní odpočinek skončí do jednoho update intervalu.
4. Opakuj se soul campfire a mimo nastavený radius.
5. U holého ohně čekej 20 skutečných sekund. Musí jednou zaznít jemný chime, action bar ukázat `Odpočatý | 5:00`, nesmí vzniknout bossbar ani Haste. Starý `messages.yml` nesmí zobrazit klíč `campfire-rested-timer`.
6. Odejdi od holého ohně a ověř běžný pokles hladu při aktivním Rested.
7. Přidej smoker, znovu nabij Rested, odejdi a ověř poloviční průměrnou ztrátu hladu.
8. Přidej crafting table a ověř minutu navíc a Haste I s potion ikonou.
9. Přidej ostatní prvky; každý unikátní typ přidá minutu, duplicity se nesčítají.
10. Přidej bed a ověř blokování přirozených nepřátel do 24 bloků i po návratu.
11. Existující mobové mohou vejít, Mythic `NekaraFauna` se spawnuje a command/summon/quest/boss spawny zůstávají.
12. Zkrať dobu a ověř zmizení časovače po expiraci, reloadu a shutdownu; řízený Haste musí zmizet také.
13. Před odpočinkem použij silnější externí Haste a ověř jeho zachování.

### 13. Skupinový odpočinek

1. Posaď dva hráče u jednoho ohně a ověř dva hráče a násobitel `1.15x` v action baru.
2. Oba hráči zůstávají nezávislí a léčí se rychleji než samotný hráč.
3. Přesuň jednoho k jinému ohni; každý oheň se stane skupinou `1.00x`.
4. `/nekararpg status` musí hlásit očekávané počty sedících, odpočívajících a Rested.

### 14. Závislosti modulů a reload

1. Vypni `modules.campfire.enabled`, reloaduj a ověř sezení bez efektů ohně.
2. Zapni Campfire, vypni Sitting a ověř Campfire přes nakonfigurované CMI sedadlo.
3. Zapni oba a ověř, že nevznikly duplicitní listenery, action bary ani scheduler efekty.

### 15. GitHub updater

1. Verzi 1.2.0 bylo nutné nainstalovat ručně; starší se neumí samy aktualizovat.
2. `/nekararpg update status` musí bez blokování ticku hlásit idle nebo dokončený stav.
3. `/nekararpg update check` při stejném nebo starším releasu označí instalaci za aktuální.
4. Publikuj novější staging release s přesným `NekaraRPG.jar`. Plugin musí nejdřív vytvořit hashově shodný rollback v `plugins/NekaraRPG/backups` a až po ověření digestu a identity `plugins/update/NekaraRPG.jar`.
5. Online operátor dostane zprávu o připravené aktualizaci a znovu po připojení před restartem.
6. Spusť kontrolu dvakrát; běží jediná síťová operace a již ověřený JAR se znovu použije.
7. Proveď úplný restart. Paper nahradí aktivní JAR, odstraní staging kopii, načte novou verzi a zachová `config.yml`.
8. Opakuj s neplatným digestem, názvem, manifest verzí, příliš velkým JARem, nedostupným API a timeoutem. Aktivní JAR musí vždy zůstat nedotčený.
9. Vypni `updater.automatic-checks` a ověř ruční kontroly. Vypni `updater.enabled` a ověř zastavení obou režimů.

### 16. NekaraAuth na offline-mode serveru

1. Spusť čistý server s `online-mode=false`, modulem `auth` a bez AuthMe.
2. Připoj nový nick. Musí se otevřít GUI registrace; pohyb, teleport, chat,
   inventář, příkazy kromě auth, boj, stavění, těžení, drop a pickup musí být blokované.
3. V kovadlině zadej krátké heslo, dvě různá hesla a nakonec dvě shodná platná
   hesla. Účet vznikne jen v posledním případě a hráč se odemkne.
4. Zkontroluj `auth/accounts.yml`: nesmí obsahovat plaintext heslo, hash musí mít
   formát `$PBKDF2-SHA256$...`, unikátní salt a work factor 600000.
5. Odpoj se a připoj znovu. Špatné heslo musí snižovat počet pokusů, pátý pokus
   hráče vyhodí a stejný nick zůstane uzamčený i po okamžitém reconnectu.
6. Přihlas se správně, spusť `/logout` a ověř nové uzamčení. GUI nesmí přidávat
   papír ani potvrzovací itemy do hráčova inventáře při zavření.
7. Zkus registrovaný nick s jinou velikostí písmen. Pre-login ho musí odmítnout
   a zobrazit přesný chráněný tvar.
8. Jako nepřihlášený operátor zkus `/nekaraauth unregister`; příkaz musí být
   zablokovaný. Po přihlášení nebo z konzole ověř `status` a řízené `unregister`.
9. Udělej `accounts.yml` nezapisovatelný nebo neplatný a restartuj. NekaraAuth
   nesmí hráče pustit bez ověření a v logu musí být jasná storage chyba.
10. Spusť `/nekararpg reload` během přihlášené relace a ověř zachování loginu,
    jedinou sadu listenerů a aplikaci nových limitů na další pokusy.
11. Nainstaluj aktivní AuthMe a restartuj. NekaraAuth musí zůstat vypnutý a
    zalogovat důvod. Po zastavení serveru, odstranění AuthMe a dalším restartu
    musí NekaraAuth převzít registraci a přihlášení.
12. Nech nepřihlášeného hráče stát v přihlašovací obrazovce. Action bar musí
    odpočítávat zbývající čas a po výchozích 120 sekundách hráče automaticky odpojit.
13. Přihlas se, normálně se odpoj a do deseti minut se vrať ze stejné IP adresy.
    Hráč se musí automaticky ověřit bez hesla. Jiná IP, vypršení deseti minut,
    `/logout`, kick, reload nebo restart serveru musí znovu vyžadovat přihlášení.
14. V registračním i přihlašovacím GUI ověř, že hlavní položka zobrazuje hlavu
    a skin právě připojeného nicku.
15. S výchozí konfigurací ověř, že běžný hráč nemůže použít ani tab-completem
    získat `/login` a `/register`. Nouzový fallback musí fungovat teprve při
    `commands.fallback-enabled: true` v `auth/config.yml` a oprávnění
    `nekararpg.auth.fallback-commands`.

Zrychlený profil časovačů, CMI průchod, ValhallaMMO kompatibilitu a staging
nasazení popisuje `LIVE_TESTING.md`.
