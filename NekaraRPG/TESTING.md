# Testovací příručka NekaraRPG

## Automatické kontroly

Pro rychlé ověření zdrojů spusť:

```text
gradlew.bat clean test build
```

Před publikací spusť ověřený release postup:

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
hranice a formát cooldownu, starý YAML round-trip, SQLite transakce, import a
perzistenci combat okna.
Nekara Skills testuje katalog 16 dovedností a 90 prezentovaných perků,
celočíselnou XP křivku do úrovně 100, odvozený Powerlevel, validaci perk DAG,
transakční nákup a nepřečerpání bodů, skládání statistik, nerekurzivní
bojové efekty, vzájemně výlučné dropy, bezpečnost aktivních schopností, XP policy,
časovou deduplikaci, časové chunk počítadlo, škálování perk statů, kódování
perzistentního původu bloku, migraci SQLite v1 → v2 a atomický admin audit se
zamítnutím zastaralé revision.
NekaraAuth navíc ověřuje, že výměna hashe hesla zachová identitu a auditní údaje
účtu. Campfire testuje pravidla přeskočení noci osamělým hráčem a migraci starého
Sitting configu; resource test kontroluje zprávy a výchozí oprávnění menu.

## Ruční akceptace na Purpur 26.1.2

Nejdřív použij čistý testovací server s Javou 25, cílovým serverem a pouze
NekaraRPG. Testovacímu hráči dej potřebná oprávnění a umísti ho do povoleného světa.

### 1. Start a konfigurace modulů

1. Nainstaluj jediný release artefakt `NekaraRPG.jar`.
2. Spusť server a ověř vznik kořenového `plugins/NekaraRPG/config.yml` a souborů
   `auth/config.yml`, `fishing/config.yml`, `campfire/config.yml`,
   `mining/config.yml`, `mounts/config.yml` a `skills/config.yml`.
3. Ověř výchozí hodnotu true u `modules.fishing.enabled`,
   `modules.campfire.enabled`, `modules.mining.enabled` a `modules.mounts.enabled`.
4. Ověř, že kořenový config neobsahuje detailní sekce modulů. Spusť
   `/nekararpg status` a ověř, že `sitting` už není samostatný modul.
5. Nastav `modules.fishing.enabled: false`, spusť `/nekararpg reload` a ověř, že rybářská minihra nezačne.
6. Modul znovu zapni a proveď další reload.
7. Upgraduj kopii starého monolitického configu s několika změněnými hodnotami.
   První start je musí přenést do správných modulových souborů a odstranit staré
   detailní sekce až po úspěšném zápisu. Druhý start nesmí vlastní hodnoty přepsat.

### 1aa. Vývojový náhled Nekara Skills 2.0

1. Na odděleném staging serveru nastav `modules.skills.enabled: true` a spusť
   `/nekararpg reload`. V produkci ponech modul vypnutý.
2. Otevři `/nrpg`. Tlačítko Dovednosti musí otevřít vlastní 54slotový přehled,
   nikoliv `/skills` z ValhallaMMO.
3. Ověř hlavní úroveň 0, všech 15 přímých dovedností na úrovni 0 a schválené
   české názvy včetně `Obchodování`. Kliknutí na výplň ani spodní inventář nesmí
   přesunout item nebo zavřít GUI.
4. Klikni na každou dovednost. Stezka musí obsahovat šest uzlů, přepínání
   předchozí/další dovednosti a návrat na přehled. Zamčený uzel musí vysvětlit
   chybějící úroveň, body nebo předchůdce.
5. Na kopii profilu s hlavní úrovní a volnými body otevři dostupný perk, zruš
   potvrzení a ověř beze změny databáze. Potom nákup potvrď: přibude právě jeden
   rank a odečte se přesná cena. Rychlý dvojklik nesmí přečerpat body.
6. Ověř vznik `plugins/NekaraRPG/skills/data.db`. Reload a restart nesmí databázi
   poškodit ani změnit revision existujícího testovacího profilu.
7. Poškoď na kopii databáze schema version. Modul se musí uzamknout, nesmí vytvořit
   náhradní profil a hráči ukáže pouze krátké RPG sdělení bez SQL detailů.
8. Vypni modul během asynchronního otevírání GUI. Starý požadavek nesmí po reloadu
   znovu otevřít obrazovku jiné generace modulu.
9. Vypni `modules.skills.enabled`. Pokud je ValhallaMMO načtené, centrální tlačítko
   se musí bezpečně vrátit k jeho `/skills`.
10. Jako hráč bez `nekararpg.skills.admin` ověř odmítnutí i skryté admin
    našeptávání. Jako op spusť `inspect`, `grant-xp`, `grant-perk` a všechny tři
    varianty `reset`; konzole musí fungovat stejně.
11. Zkontroluj, že každá skutečná změna zvýší revision právě o jednu a `inspect`
    ukáže správce, operaci, čas, detail a revision před/po. Opakovaná no-op změna
    revision ani audit nepřidá.
12. Na kopii databáze v1 proveď start do 2.1.0. Profil, XP a perky musí zůstat
    stejné a metadata přejít na schéma v2. Neznámá budoucí verze se dál odmítne.
13. Spusť současně těžební XP a admin reset stejného profilu. Výsledek musí být
    jedna konzistentní posloupnost revisions bez částečného profilu nebo auditu.
14. V čisté staging oblasti vytěž krumpáčem každý blok uvedený pod
    `skills.mining.experience.blocks`. Ověř odpovídající růst Hornictví v
    `skills/data.db`, jediný revision krok za odměnu a žádné přímé XP pro Power.
15. Polož a znovu vytěž stejný typ bloku. XP ani výtěžkový bonus nesmí vzniknout;
    opakuj po restartu, výbuchu a přesunu pístem. Bloky položené před prvním
    zapnutím trackeru testuj odděleně jako známou přechodovou hranici.
16. Ověř odmítnutí v Creative, Spectator, zakázaném světě, se zrušeným breakem a
    po ručně vyvolaném eventu bez skutečné změny bloku. Dvojí dispatch stejného
    zdroje nesmí vytvořit druhý zápis.
17. Překroč soft a hard limit v jednom chunku. Mezi limity musí XP klesat až k
    nastavenému floor multiplikátoru a za hard limitem nevzniknout; sousední chunk
    zůstává nezávislý a časové okno staré záznamy uvolní.
18. Na profilu s `mining.yield` a `mining.triple` ověř vzájemně výlučný dvojitý
    nebo trojitý výtěžek. Bonus musí zachovat metadata skutečných finálních itemů,
    Silk Touch a Fortune nesmí házet znovu a ValhallaMMO extra odměna se nesmí
    násobit druhým průchodem.
19. Ověř Mining rychlost s krumpáčem, `Žhavou směnu` při otevřené peci a vlastní TNT
    s `mining.blast`. Silnější cizí zrychlení pece se nesmí přepsat, odstřel nesmí
    zapálit bloky a seznam zasažených bloků musí zůstat pod nastaveným limitem.
20. S `mining.vein` se při plížení dotkni přirozené propojené žíly. Ověř cooldown,
    blokový limit, dávkování po ticku, spotřebu krumpáče, zastavení po teleportu a
    zachování bloků zrušených Lands nebo jinou ochranou.
21. Stejnou matici XP, položených bloků, chunk limitů, restartu, Creative/Spectator
    a duplicitního eventu zopakuj se sekerou pro Rubačinu a lopatou pro
    Zeměrytectví.
22. Ověř výtěžkové perky obou dovedností. `woodcutting.recipes` smí změnit pouze
    vanilla recept jednoho kmene na pět prken; `woodcutting.leaves` smí přidat
    nejvýše jeden vážený nález pouze z přírodního listí.
23. S `woodcutting.feller` poraz při plížení přírodní strom. Stavba z položených
    kmenů, strom bez koruny, cizí chunk a blok zrušený ochranou musí zůstat beze
    změny. Ověř cooldown, limit, dávkování a vanilla durability sekery.
24. U Zeměrytectví ověř vážené nálezy z přirozené zeminy. `digging.archaeology`
    rozšíří tabulku, ale suspicious sand/gravel se samotným rozbitím neodměňuje.
25. Sklízej jen plně zralé plodiny. Nezralá plodina, Creative/Spectator a zrušený
    break nesmí dát Farming XP. `instant` znovu zasadí jen po úspěšném
    `Player.breakBlock`; `field` zasáhne nejvýše 3×3 načtených bloků.
26. Dokonči běžný i deferred NekaraFishing úlovek. Každý dá Fishing XP právě
    jednou; neúspěšná minihra, reel bez itemu a zrušený catch nedají nic.
27. Proveď trade, craft výbavy, smithing a enchant. Ověř jednu nakonfigurovanou XP
    odměnu, slevový refund až po obchodu, úsporu až po craftu a žádnou odměnu za
    zrušenou událost nebo přejmenovaný nesouvisející item.
28. Ručně vlož ingredienci do brewing standu a dokonči var. Potom opakuj pouze
    hopperem bez otevření stojanu; druhý var nesmí dostat vlastníka ani Alchemy XP.
29. Každou bojovou dovednost testuj proti nepřátelské entitě. PvP alt, armor stand
    a pasivní zvíře nesmí udělit combat XP. Armor XP vzniká jen v plné lehké nebo
    těžké sadě po skutečném zásahu nepřítelem.
30. Ověř parry, dodge, reflection, charged shot, coating, uppercut/dropkick/grapple
    a adrenalin/rage. Reflection nesmí rekurzivně spouštět další reflection ani XP.
31. Proveď burst alespoň 1000 způsobilých akcí. Konzole nesmí hlásit zaplnění XP
    fronty, hlavní tick se nesmí blokovat SQLite zápisem a revision profilu musí
    zůstat monotónní.
32. Po prvním lehnutí ověř log o nativním mannequin vizuálu, vlastní third-person
    pohled i pohled druhého hráče. Vstání, teleport, poškození a odpojení musí
    odstranit mannequin, obnovit viditelnost hráče a nezměnit skutečný svět.

### 1a. Centrální menu a účet

1. Jako běžný přihlášený hráč spusť `/nekararpg` a ověř otevření centrálního GUI.
2. Vypínej jednotlivé moduly v `config.yml` a používej `/nekararpg reload`; vypnutá
   dlaždice se po novém otevření nesmí zobrazit.
3. Odeber hráči oprávnění k Táboření nebo Mounts. Příslušná dlaždice se nesmí
   zobrazit ani být použitelná přes staré otevřené menu.
4. Kliknutím na účet otevři NekaraAuth, klikni na změnu hesla a postupně zadej
   současné heslo, nové heslo a stejné nové heslo znovu.
5. Odhlas se. Staré heslo musí být odmítnuto a nové musí přihlášení povolit.
6. Opakuj s chybným současným heslem a s neshodným potvrzením. Hash účtu se nesmí
   změnit a chybné současné heslo se započítá do stejného lockoutu jako login.
7. Během asynchronního ověření proveď logout nebo odpojení. Rozpracované GUI se
   nesmí znovu otevřít a zápis změny se nesmí zahájit v neplatném autentizačním stavu.
8. Ověř, že konzole, chat a `auth/accounts.yml` nikde neobsahují plaintext hesla.
9. Jako běžný hráč ověř, že dlaždice Správa NekaraRPG není vidět. Jako op ji
   otevři, pak odeber `nekararpg.command.status` a ověř, že staré GUI správu
   znovu neotevře.
10. Otevři Činnosti a ověř Rybaření, Táboření a Těžbu podle aktivních modulů.
    Z Táboření otevři Tábořiště a vyzkoušej tlačítka sednout/vstát a lehnout/vstát.
11. Stejná tlačítka pro sezení a ležení musí být přímo na hlavní obrazovce
    `/nrpg` a po použití hráče vrátit na stejnou obrazovku.

### 1b. Sezení, ležení a noční klid

1. Ulehni bez zapáleného ohně v radiusu. Spánková póza musí zůstat aktivní, ale
   nesmí začít Rested session, léčení, ochrana hladu ani přeskočení noci.
2. Zapal campfire nebo soul campfire, ulehni a ověř spánkovou pózu bez postele.
   Rested se musí nabíjet stejně jako při sezení.
3. Zkus se pohnout: pozice musí zůstat stejná a ležení pokračovat. Otočení hlavy
   je povolené. Přikrčení, teleport, poškození, smrt a odpojení musí ležení
   bezpečně uklidit bez trvalé pózy po návratu.
4. Se dvěma online hráči zůstaň ležet přes noc déle než nastavený čas. Noc se
   nesmí změnit. Druhý hráč se odpojí; pokud je stále noc a oheň hoří, jediný
   zbývající hráč může po nastaveném čekání přejít do rána.
5. Během čekání připoj druhého hráče. Přeskočení se zablokuje, ale ležení a
   nabíjení Rested pokračuje. Ověř, že CMI nehlásí spuštěný sleep příkaz a spawn
   hráče se nezměnil.
6. Reloaduj s vypnutým `campfire.lying.enabled` a potom s vypnutým
   `skip-night-when-alone`; v prvním případě nejde ulehnout, ve druhém ležení
   funguje bez posunu času.
7. Upgraduj kopii datové složky se starým `sitting/config.yml` a bez sekce
   `campfire.sitting`. Ověř převzetí vlastních hodnot do `campfire/config.yml`,
   ponechání starého souboru jako zálohy a žádné další přepisování po restartu.

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
   doběhnout na místo písknutí a poté přirozeně putovat v okruhu pěti bloků.
   Jméno nad entitou musí být tučné a nový kůň musí mít sedlo.
3. Změň polohu a znovu pískni. Aktivní kůň musí vyrazit k novému místu; rychlé
   opakování zastaví třísekundová ochrana proti spamu. Po odvolání platí pro nové
   vytvoření 30 sekund a na serveru je vždy právě jedna entita stejného mount ID.
4. Zkus píšťalku zahodit, shift-clicknout do truhly, vložit do Ender Chest a sebrat
   druhým hráčem. Vše musí být odmítnuto. `mount whistle remove` ji odebere a
   `mount whistle restore` vydá právě jednu; při plném inventáři nic nesmí spadnout.
5. Odvolej koně, restartuj server a znovu ho přivolej. Porovnej jméno, zdraví,
   maximální zdraví, rychlost, skok, barvu, styl, sedlo a brnění.
6. Nech aktivnímu koni unloadnout chunk a vrať se. Starý záznam entity nesmí vydat
   vybavení ani vytvořit kopii; odvolaný mount se musí dát bezpečně přivolat.
7. Přes GUI změň jméno, barvu, sedlo a brnění. Kliknutí na prázdný slot výbavy
   nesmí nic uložit ani ohlásit. Ověř návratová tlačítka, potvrzení odvolání a
   odebrání píšťalky a to, že kliknutí na výplň GUI menu nezavře.
   Ve spodním inventáři postupně běžně klikni na sedlo, obyčejnou truhlu a všechny
   podporované druhy koňského brnění. Každý vhodný kus se musí rovnou nasadit a
   nahrazený kus vrátit na kurzor. Ověř také klasický přesun přes kurzor a horní slot.
8. Nasaď obyčejnou truhlu, otevři 54slotové brašny přes GUI koně a naplň několik
   krajních slotů různými stacky a itemy s metadaty. Odvolej, zabij koně, reloaduj
   a restartuj server; obsah musí zůstat beze změny. Plnou truhlu nelze sundat,
   píšťalku vložit ani shift-clickem obejít kontrolovaný přesun.
9. Nech cizího hráče zkusit nasednout, otevřít inventář, přejmenovat, měnit výbavu
   a brašny. Vše musí být bezpečně odmítnuto.
10. Způsob PvP zásah mezi dvěma hráči. `create`, `grant`, `call` i `dismiss` musí být
   15 sekund blokované. Odpoj a připoj hráče; zbývající blokace musí pokračovat.
11. Zabij mounta a ověř minutový cooldown i návrat stejné výbavy. Opakuj se smrtí
   hráče: píšťalka nesmí dropnout a po respawnu se musí vrátit do inventáře.
12. Vypni `modules.mounts.enabled` a reloaduj. Aktivní entita zmizí až po úspěšném
   uložení, `mounts/data.db` zůstane a ostatní modulové configy se nezmění.
13. Znepřístupni kopii storage pouze na stagingu. Operace musí selhat uzavřeně,
    aktivní entita se nesmí smazat a konzole musí ohlásit uzamčení modulu.
14. Otevři centrální `/nrpg`, zkus prázdná místa a informační dlaždice a ověř, že
    menu zůstane otevřené bez informačního spamu v chatu. S ValhallaMMO musí
    tlačítko dovedností otevřít stejné GUI jako `/skills`.
15. Před prvním startem nové verze ponech pouze platné `mounts/data.yml`. Ověř
    vznik `mounts/data.db` a `data.yml.pre-sqlite.bak`, shodu koní i combat oken a
    to, že další restart import neopakuje. Po migraci otestuj brašny přes restart.
16. V osobním přehledu porovnej účet, Rested, rybaření, Echo Vein a stav koně se
    skutečností. Jako operátor otevři diagnostiku a ověř moduly, integrace, počty
    koní, SQLite, updater a návrat do hlavního menu.

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
2. Zapni Campfire a ověř Rested přes interní i nakonfigurované CMI sedadlo.
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
