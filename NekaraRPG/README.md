# NekaraRPG

NekaraRPG je modulární plugin pro Purpur/Paper, který zajišťuje RPG a imerzivní
systémy Nekary. V jednom JARu aktuálně obsahuje NekaraAuth, NekaraFishing,
Táboření se sezením, ležením a odpočinkem u ohně, NekaraMining s Echo Vein a
NekaraMounts s jedním trvalým vanilla koněm na hráče.

Plugin je záměrně konzervativní: nenahrazuje vanilla loot tabulky, nevytváří
syntetické fishing eventy ani jiný herní plugin.

## Nekara Skills 2.2 — native progression

Aktuální GUI: aktivace `Nová hra+` je po dosažení levelu 100 přímo uprostřed rozšířené perkové stezky; dřívější údaj o spodní liště již neplatí.

### New Game+ a serverové XP události

Po dosažení levelu 100 je v každé přímé dovednosti ve spodní liště její stezky
`Nová hra+`. Po potvrzení resetuje pouze XP a zakoupené perky této dovednosti;
body utracené v jejích percích se vrátí. Každá dovednost může Novou hru+
aktivovat právě jednou. Trvale zvyšuje její perk-statistiky o 2 % a druhý běh
zpomalí na 90 % XP. Dokončená Nová hra+ se počítá jako dalších 100 úrovní pro
odvozený Power: při druhém vyexpění všech dovedností může Power dosáhnout 200.
Pravidla jsou upravitelná v `skills/config.yml` → `new-game-plus`.

Pro víkendové i testovací bonusy existuje jedna perzistentní událost:
`/nrpg skills admin event start <skill|all> <1-5> <30m|2h|1d>`, dále `event status`
a `event stop`. Stav je uložen v `skills/experience-event.yml`, takže přežije restart.

Pro rychlé testování lze použít dočasný admin boost bez zásahu do profilu:
`/nrpg skills admin xp-boost <hráč> <skill|all> <1-100>` a zrušení přes
`/nrpg skills admin xp-boost-clear <hráč> <skill|all>`. Boost se po restartu
automaticky smaže.

Větev 2.0 staví vlastní autoritativní RPG postup: 15 přímo trénovaných dovedností
a odvozený Powerlevel, vše se stropem 100. Součástí základu je validovaný perk
graf, skládání statistik, bojové efekty, bezpečný násobitel dropů, rozhraní pro
aktivní schopnosti a ochrana XP před opakováním či automatizací. Powerlevel je
průměr úrovní všech trénovaných dovedností; jednostranné vytěžení jediné profese
proto hlavní úroveň výrazně nezvedne.

Implementace je clean-room. NekaraRPG nepřebírá cizí kód, texty, názvy perků,
hodnoty ani rozložení stromů. Podrobná rozhodnutí jsou v
`docs/adr/0001-native-skills-platform.md` a pořadí práce v
`docs/SKILLS_2_0_ROADMAP.md`.

Release 2.1.0 přidává původní katalog 90 perků, detailní 54slotové stezky,
české názvy dovedností a potvrzený transakční nákup za společné Power body.
Každá dovednost má šest uzlů ve dvou větvích; GUI vysvětluje rank, cenu, úroveň
a předchůdce. Všech 15 dovedností má nativní validovaný zdroj XP a živou runtime
vertikálu. Sběr, výroba, obchod, rybaření i boj používají sdílenou cache profilu,
omezenou asynchronní frontu zápisů a eventové efekty bez per-tick skenování světa.
Po úspěšném zápisu ukazuje action bar právě připsanou XP odměnu a postup
v aktuální úrovni, například `+4 XP | ▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱▱ 28 %`.
Stejný segmentový ukazatel i přesný zbývající počet XP je v popisu dovednosti.
Stejnou segmentovou grafiku používá rybářská minihra: tmavá dráha `▱`, tyrkysové
cílové pole `▰` a zlatá kotva jako pohybující se ukazatel.
Modul `skills` je výchozím stavem zapnutý a je nativní autoritou postupu.

### Doprovodný resource pack stezky

NekaraRPG používá klientské textury z odděleného repozitáře
[`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack).
Je zdrojem textur perk-tree cest, navigačních šipek a rezervovaných modelů vlastních
zbraní; plugin obsahuje pouze jejich namespaced identifikátory a bez packu použije
bezpečné vanilla zástupné ikony.

Klientský balíček `resource-pack/NekaraRPG-GUI` přidává vlastní osmisměrné navigační
šipky a animované spojnice perků ve třech stavech: zamčené, dostupné a odemčené.
Sestaví se skriptem `resource-pack/NekaraRPG-GUI/build-resource-pack.ps1` do
`resource-pack/NekaraRPG-GUI.zip`. Plugin bez nahraného packu zůstane funkční a použije
vanilla zástupné ikony; pro finální vzhled je nutné ZIP hostovat a připojit ho v nastavení serveru.

## Moduly

Nativní moduly se ovládají v `config.yml`. Výchozí profil používá Nekara Skills a
neaktivuje Echo Vein, dokud ji administrátor výslovně nezapne:

```yml
modules:
  auth:
    enabled: true
  fishing:
    enabled: true
  campfire:
    enabled: true
  mining:
    enabled: false
  mounts:
    enabled: true
  skills:
    enabled: true
```

Kořenový `config.yml` obsahuje pouze jádro, updater a přepínače modulů. Podrobné
nastavení je rozdělené do `auth/config.yml`, `fishing/config.yml`,
`campfire/config.yml`, `mining/config.yml`, `mounts/config.yml` a
`skills/config.yml` uvnitř datové složky pluginu. Každá trénovaná dovednost má
navíc vlastní `skills/<skill>/config.yml`, `experience-sources.yml` a `messages.yml`;
`experience-sources.yml` určuje použitelné XP zdroje a jejich hodnoty bez změny kódu.
Skill s vlastními
nálezy může mít i `loot-tables.yml`. Při prvním upgradu ze starého monolitického
configu se vlastní hodnoty automaticky přenesou do odpovídajících souborů.

Aktuální moduly:

| Modul | Stav | Popis |
| --- | --- | --- |
| `auth` | testovací | Registrace, přihlášení a ochrana nicku pro offline-mode server. |
| `fishing` | produkční | Nenásilná časovací rybářská minihra s původním vanilla úlovkem. |
| `campfire` | produkční | Sezení, ležení, léčení, Rested bonus, skupinové škálování a roleplay u zapáleného ohně. |
| `mining` | volitelný | Echo Vein pro nativní těžbu; ve výchozím profilu je vypnutý. |
| `mounts` | testovací | Jeden trvalý vanilla kůň na hráče s píšťalkou, brašnami a ochranou proti duplikaci. |
| `skills` | nativní | Přehled 16 dovedností, 90 perků, transakční nákup a runtime vertikály všech 15 trénovaných skillů. |

Ležení nepoužívá ručně sestavená entity metadata. Na Purpur/Paper 26.1 vykresluje
nativní nekolizní mannequin se skinem a výbavou hráče; lze ho vypnout přes
`campfire/config.yml` → `lying.mannequin-visual-enabled`. Při chybě zůstane
odpočinek funkční na bezpečné serverové póze.

### Nativní sběrné vertikály

Při zapnutém `modules.skills.enabled` používají Těžba, Lesnictví a
Kopání vlastní tabulky pod `mining`, `woodcutting` a `digging` v
`skills/<skill>/config.yml`; vážené nálezy jsou v příslušném `loot-tables.yml`.
Rybaření má vlastní tabulku bonusových pokladů, která nikdy nenahrazuje skutečný
vanilla úlovek. Má 0,5 % základní šanci, která na levelu 100 dosáhne 2 %.
`skills/config.yml` obsahuje malé, zastropované vrozené bonusy
podle úrovně: až 2,5 % na dvojitý výtěžek ze sběru, až 1 % na nález ze
Kopání a až 1,5 % na rybářský poklad. Perky se k nim přičítají a zůstávají
hlavním zdrojem specializace.
Odměna vznikne až další
tick po skutečné změně vytěženého bloku, takže samotné syntetické vyvolání eventu
nestačí. Creative, Spectator, zakázaný svět a hráčem položený blok jsou odmítnuté.
Aktivita v jednom chunku se v časovém okně nejdřív tlumí a po hard limitu zastaví.

Původ bloků položených při zapnutém modulu se ukládá přímo v perzistentních datech
chunku a přežije restart. Historické bloky položené před prvním zapnutím trackeru
nelze zpětně spolehlivě určit; staging proto začíná v čisté oblasti a produkční
aktivace bude vyžadovat předchozí období sledování.

Perky dvojitého a trojitého výtěžku používají jediný vzájemně výlučný hod. Bonus
klonuje ItemStacky z reálného `BlockDropItemEvent`, zachová metadata a nespouští
znovu loot tabulku ani Fortune. Držení odpovídajícího nástroje
aktivuje perk rychlosti; horník s otevřenou pecí zkracuje pouze vanilla dobu receptu
a nepřepisuje rychlejší cizí úpravu.

`Žilobití` a `Pád velikána` se aktivují plížením při rozbití zdrojové rudy nebo
přírodního kmene. Každý další blok prochází skutečným hráčským breakem, limitem,
cooldownem, vanilla durability a kontrolami regionových pluginů. Cizí chunky se
nenačítají. `Řízený odstřel` omezeně posílí TNT zapálené vlastníkem perku a zastropuje
počet ovlivněných bloků. Lesnictví dále nabízí pět prken z vanilla receptu a nálezy
v přírodním listí; Kopání používá váženou tabulku nálezů rozšířenou perkem
`Paměť střepů`.

### Výkovy přímo ve vanilla craftingu

Kovové zbroje a zbraně vytvořené ve crafting table získají Tier I–V podle aktuální úrovně
Řemesla, ale vznikají jako `Nezpracované`; dřevěné, kamenné a kožené kusy zůstávají
bez výrobního procesu. Pravý klik s kovovým výkovem na Blast Furnace
spotřebuje jedno uhlí a nastaví `Nahřátý`; vodní cauldron jej změní na `Opracovaný` a
aktivuje armor bonus. Zbraně je pak nutné při plížení dokončit u grindstone: dvě sekundy
u brusu aktivují jejich Nekara damage. Tier se tím nezvyšuje a vanilla enchanty zůstávají
zcela oddělené.

Každý takový předmět má v lore barevný řádek `Stav výkovu` a grafický postup
`[◆◇◇◇]`. Součástí ukazatele je vždy konkrétní další krok, například Blast Furnace,
vodní cauldron nebo plížení u grindstone; hotový kus jasně hlásí aktivní bonus.

Perk `Zapomenuté nákresy` zvyšuje pouze výnos běžných vanilla stavebních bloků podle
Tieru Řemesla. Například recept s normálním výstupem čtyř kusů dává Tier I–V postupně
4, 5, 6, 7 a 8 kusů. Upravený počet je vidět přímo ve výsledkovém slotu crafting table
a funguje stejně při běžném i shift-craftingu. Kromě bloků se týká i základních stavebních
komponent: sticků, bowls, clay balls, běžných, netherových a resinových cihel. Neplatí pro jídlo, plodiny,
mob dropy ani skladovací bloky cenných surovin. Stonecutter pro kmeny, dřevo a prkna nabízí nativní dřevěné recepty v běžném
seznamu; hráči s perkem `Zapomenuté nákresy` v jejich výsledku uvidí stejnou škálu.

### Další nativní vertikály

`activities` v `skills/config.yml` nastavuje společnou deduplikaci; základní XP je
v `experience.amount` konfigurace každého skillu. XP vznikají
jen z nezrušené skutečné události: zralé sklizně, doručeného úlovku, dokončeného
obchodu/craftu/enchantu/varu nebo zásahu nepřátelské entity. PvP, pasivní zvířata,
Creative, Spectator a automatický var bez nedávné ruční práce hráče XP nedávají.

Perky používají cache přepočítanou pouze po změně profilu. Bojová pipeline pokrývá
damage, kritické zásahy, krvácení, omráčení, power attack, dodge, armor, reflection, parry, charged shot,
adrenalin, rage, grapple, uppercut, dropkick, coating a hexblade. Produkční pipeline
pokrývá kvalitu výbavy, úsporu zdrojů a XP, sílu enchantů a lektvarů, rychlost varu,
reputaci a slevy, zrychlený růst opečovávaných plodin, sklizeň pole, včelaření,
rychlost rybolovu a vybavení či salvage z úlovku. Hromadná sklizeň i nadále volá
skutečný `Player.breakBlock`, takže každý blok mohou zrušit regionové pluginy.

### Perkové recepty a dílenské úpravy

Perky mimo Obchodování mají konkrétní nativní účinek. `Poctivé řemeslo`, `Jemná
práce` a `Mistrovský kus` dávají přesnou šanci posunout nově vyrobenou výbavu o
jeden Tier výše. `Zapomenuté nákresy` odemykají řemeslnickou soupravu z 4 železných
nugetů, papíru a provázku. S `Dílenskými úpravami` hráč plíží a klikne na smithing
table s poškozenou výbavou v hlavní ruce a soupravou v levé ruce; souprava se
spotřebuje a obnoví 25 % odolnosti.

`Herbář Nekary` odemyká Tonikum vitality: water bottle, sweet berries a glow
berries vytvoří lektvar Regeneration I na 45 sekund. `Spojené esence` spojí dva
pitné lektvary a ametystový střep do jedné lahve, nejvýše se dvěma odlišnými
účinky. `Úsporné trámy` mění vanilla recept jednoho kmene či stonku na pět prken.
`Šípařova brašna` odemyká čtyři Šípy průzkumníka ze čtyř šípů, glow ink sac a
ametystového střepu; střela označí zasažený cíl na osm sekund. Všechny tyto recepty
se zobrazí jen hráči s odpovídajícím zakoupeným perkem a používají běžnou crafting mřížku.

### Auditovaná staging správa Nekara Skills

Operátor s `nekararpg.skills.admin` může na zapnutém staging modulu použít
`/nrpg skills admin`. Příkazy pracují jen s hráčem, kterého server už zná, a
nikdy nevyžadují ruční editaci živé databáze. `grant-xp` přidává XP nejvýše do
levelu 100; `grant-perk` ověří katalogové ID a maximální rank, obejde hráčské
level/prerequisite podmínky pro účely stagingu a započítá běžnou cenu bodů.
`reset <skill>` vynuluje jen XP dané dovednosti, `reset perks` vrátí všechny
uložené body a `reset all` vyčistí obojí.

SQLite schéma v2 migruje stávající v1 databázi transakčně. Každá skutečná admin
změna uloží společně profil, správce, operaci, detail, čas a revision před/po.
`inspect` ukazuje profil i posledních pět auditních změn; běžní hráči nemají
oprávnění ani našeptávání podřízených admin příkazů.

`metrics` ukazuje provozní stav omezené XP fronty od posledního zapnutí modulu:
požadavky, výsledky, chyby, připsaná XP, aktuální i nejvyšší hloubku fronty a
latenci. `export` asynchronně vytvoří pod `skills/exports` ZIP s konzistentním
SQLite snapshotem, CSV profily/XP/perky/auditem a manifestem s SHA-256. Cestu
neurčuje hráč, současně běží nejvýše jeden export a živá databáze se nemění.

Campfire přijímá sedadla NekaraRPG i nakonfigurovaná externí vehicle sedadla.
Při upgradu se vlastní hodnoty ze starého `sitting/config.yml` jednou převezmou
do sekce `sitting` v `campfire/config.yml`; starý soubor zůstane jako záloha.

## Centrální menu

Hráč otevře hlavní nabídku příkazem `/nekararpg` nebo `/nekararpg menu`. Nabídka
zobrazuje jen moduly, které jsou právě zapnuté v `config.yml` a ke kterým má hráč
oprávnění. Rybaření, Táboření a Těžbu sdružuje pod dlaždicí Činnosti; v Tábořišti
lze sednout, vstát, lehnout si i ukončit ležení. Původní příkazy zůstávají jako fallback
a pro administraci. Zapnutý nativní modul `skills` nabídne vlastní přehled a
perk stezky. Přístup k nabídce řídí oprávnění
`nekararpg.menu.use`.

Ikona osobního přehledu při zapnutých Dovednostech otevírá živou kartu postavy:
životy, poškození, zbroj, pohyb, aktivní bojové bonusy a souhrn bonusů pro
sběr, výrobu a obchod. Hodnoty se vždy znovu načtou podle právě nasazené výbavy;
odkaz Dovednosti vede zpět na úrovně, XP a perk stezky. Bez nativních Dovedností
zůstává bezpečný stručný přehled účtu, Rested, probíhající činnosti a koně. Hráči s administrátorským
oprávněním `nekararpg.command.status` mají také diagnostické GUI se stavem modulů,
integrací, updateru, paměti a úložiště Mounts. Toto oprávnění je výchozím stavem
pouze pro operátory a kontroluje se znovu i při samotném otevření obrazovky.

## NekaraMounts

Hráč nepotřebuje hledat ani ochočovat vanilla koně. `/nekararpg mount` otevře
prvotní GUI s výběrem barvy a následně bezpečnou kovadlinu pro jméno. Vznikne
virtuální záznam jednoho koně a hráč dostane svázanou píšťalku. Administrátor může
stejný průchod otevřít online hráči přes `/nekararpg mount grant <hráč>`.

Pravé kliknutí píšťalkou nebo `mount call` vytvoří odvolaného koně 7-12 bloků od
hráče a pošle ho na místo písknutí. Po doběhnutí se přirozeně prochází ve výchozím
okruhu pěti bloků. Další písknutí už aktivního koně přesměruje k nové pozici hráče
s krátkou třísekundovou ochranou proti spamu; nikdy nevznikne druhá entita. Stav v
nenačteném chunku se odmítne kopírovat. Nové vyvolání odvolaného koně má výchozí
cooldown 30 sekund.
`dismiss` uloží celý stav před odstraněním entity, stejně jako bezpečný unload,
reload a vypnutí modulu.

Vlastní patnáctisekundové PvP okno se ukládá do `mounts/data.db`, takže reconnect
blokaci neobejde. Smrt zachová vybavení uvnitř záznamu a výchozím stavem spustí
minutový cooldown. Odvolání neléčí ani nemaže oheň, zamrznutí či potion efekty.
Nový kůň dostane sedlo. Přejmenování, barva, sedlo, brnění, truhla a obnova
píšťalky jsou v management GUI. Nasazená obyčejná truhla zpřístupní pouze přes
toto GUI trvalé brašny o 54 slotech; plnou truhlu nelze z koně sundat. Každá změna
brašen se nejprve atomicky uloží a při chybě se v inventáři neprovede.
Sedlo, obyčejnou truhlu i koňské brnění lze nasadit jedním běžným kliknutím na
vhodný předmět ve spodním hráčském inventáři nebo klasicky přes kurzor a horní slot.
Píšťalka je svázaná s hráčem: nelze ji zahodit, uložit do cizího inventáře ani
sebrat jiným hráčem. `/nekararpg mount whistle <restore|remove>` odstraní všechny
hráčovy nalezené kopie nebo vydá právě jednu novou.
Jméno aktivní entity je vždy tučné. Smrt nic nedropuje a uložená výbava se vrátí
po death cooldownu. Povolené světy, prostor, pathfinding, autosave, recall a oba
cooldowny se nastavují v `mounts/config.yml`. První verze nemá přímou Lands API
integraci; omezení regionů lze vynutit kontextovým oprávněním nebo seznamem světů.

Mounts používá transakční SQLite s WAL. Při prvním startu databáze se existující
`mounts/data.yml` jednorázově importuje, ponechá na místě a zkopíruje do
`mounts/data.yml.pre-sqlite.bak`. Selhání migrace nebo zápisu modul uzamkne a
nepokračuje operací, která by mohla duplikovat koně, výbavu nebo obsah brašen.

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

## Sezení a ležení

Příkaz `/nekararpg sit` posadí hráče na aktuální uzemněné pozici a
`/nekararpg stand` ho postaví. Sedadlo je neviditelná netrvalá entita, která se
odstraní při sesednutí, teleportu, smrti, odpojení, nakonfigurovaném poškození,
vypnutí modulu, čištění při reloadu a ukončení serveru.

Příkaz `/nekararpg lay` nebo tlačítko na hlavní obrazovce či v Tábořišti uloží
hráče do spánkové pózy na zemi. Nejde o vanilla spánek: světový blok ani spawn se
nemění a plugin nevolá příkaz CMI. Purpur/Paper 26.1 vykreslí nativní mannequin
se skinem a výbavou hráče v podporované sleeping póze. Výchozí yaw korekce `-90°`
a forward offset `-0.9` tělo zarovnají se směrem hráče a vystředí na původní
pozici; `lying.mannequin.*` dovoluje jemné doladění. Skutečné vybavení původní
neviditelné entity se skrývá pouze klientsky, takže nevzniká druhý krumpáč a
inventář se nemění. Při vypnutí pojistky nebo runtime chybě zůstane bezpečný
serverový fallback. Během ležení je poziční pohyb
zablokovaný, ale hráč se může rozhlížet. Přikrčení, teleport, smrt, odpojení,
poškození podle konfigurace nebo `/nekararpg rise` jej znovu postaví.

NekaraRPG záměrně neregistruje hlavní příkaz `/sit`. CMI a jiné sitting pluginy
tak zůstávají vlastníky svých příkazů. Campfire výchozím stavem detekuje externí
sedadla `ARMOR_STAND`, takže `/cmi sit` funguje bez compile-time závislosti na
CMI. Další vehicle typy lze přidat pod `sitting.external-seat-entity-types` v
`campfire/config.yml`.

## Odpočinek u ohně

Hráč aktivně odpočívá, když sedí nebo leží v nastaveném radiusu od zapáleného
campfire nebo soul campfire. Výchozí radius je `5.0` bloků a používá skutečnou
trojrozměrnou vzdálenost od hráče k ohni. Aktivní odpočinek:

- pomalu obnovuje zdraví,
- brání poklesu hladu,
- v nastaveném intervalu doplňuje malé množství hladu,
- škáluje léčení a hlad, pokud jeden oheň sdílí více hráčů,
- zobrazuje nastavitelné progress a roleplay zprávy v action baru,
- používá malé množství částic jako potvrzení aktivní mechaniky.

Ležící hráč může po výchozích pěti sekundách přeskočit noc pouze tehdy, pokud je
jediným online hráčem na celém serveru a nachází se v Overworldu. Připojení
dalšího hráče přeskočení zablokuje, ale samotné ležení ani Rested nepřeruší.

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

Rested výchozím stavem přidává 10 % k běžným NekaraRPG Skills akcím.
Administrativní příkazy, resety, redemption a migrační refundace se nenásobí.

## Echo Vein

Echo Vein je první volitelná aktivita NekaraMining a je dostupná na každém
Mining levelu. Těžba stone, deepslate, netherrack nebo end stone se skutečnými
Nativní těžba hostitelského bloku má výchozí 5% šanci odhalit blízký viditelný blok stejné
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

Vytěžení označeného bloku přidá 25 % pozorovaného Mining XP do nativní dovednosti
Těžba; na tuto odměnu se aplikuje standardní NekaraRPG pipeline včetně Rested. Jeden
bonusový item se vybírá podle skutečného množství z finálních přirozených a
přirozených dropů označeného bloku. Metadata zůstávají, množství je nejvýše
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

## Nativní rybaření a Rested

Rybářská minihra se škáluje podle nativního levelu Rybaření a po úspěchu udělí
jednu nativní XP odměnu. Rested bonus lze upravit nebo vypnout:

```yml
campfire:
  rested:
    skills-experience:
      enabled: true
      multiplier: 1.10
```

Škálování obtížnosti rybaření se vypíná:

```yml
fishing:
  difficulty:
    enabled: false
```

## Požadavky

- Purpur 26.1.2 nebo kompatibilní implementace Paper API
- Java 25
- Pro plánované nasazení NekaraAuth server s `online-mode=false`; při
  `online-mode=true` modul funguje, ale zaloguje varování o dvojím ověření
- Žádné povinné pluginové závislosti; MythicMobs je volitelný

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
- `plugins/NekaraRPG/campfire/config.yml`
- `plugins/NekaraRPG/mining/config.yml`
- `plugins/NekaraRPG/mounts/config.yml`
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
| `/nekararpg prehled` | `nekararpg.skills.use`; živý přehled postavy |
| `/nekararpg sit` | `nekararpg.sitting.use` |
| `/nekararpg stand` | žádné; hráč se musí vždy moci postavit |
| `/nekararpg lay` | `nekararpg.campfire.use` |
| `/nekararpg rise` | žádné; hráč se musí vždy moci zvednout |
| `/nekararpg mount [menu\|status\|call\|dismiss\|whistle]` | `nekararpg.mount.use` |
| `/nekararpg mount grant <hráč>` | `nekararpg.mount.admin` |
| `/nekararpg skills admin inspect <hráč>` | `nekararpg.skills.admin` |
| `/nekararpg skills admin grant-xp <hráč> <skill> <množství>` | `nekararpg.skills.admin` |
| `/nekararpg skills admin grant-perk <hráč> <perk-id> [rank]` | `nekararpg.skills.admin` |
| `/nekararpg skills admin points <hráč> <add\|remove> <množství>` | `nekararpg.skills.admin` |
| `/nekararpg skills admin reset <hráč> <skill\|perks\|all>` | `nekararpg.skills.admin` |
| `/nekararpg skills admin metrics` | `nekararpg.skills.admin` |
| `/nekararpg skills admin export` | `nekararpg.skills.admin` |
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
- Automatické Echo Vein používá nativní těžbu. Testovací příkaz ověřuje
  viditelnost cíle bez odměn.
- Staré `minimum-mining-level` z 1.2.1 a starších se ignoruje.
- Pokud chybí `modules.mining.enabled`, použije se staré `modules.echo-vein.enabled`.
- Bonusový item Echo Vein pochází z finálních dropů cílového bloku. Digging
  treasure tabulka ani samostatný loot se nepoužívá.
- Rested má jen textový časovač; tábor s crafting table navíc používá vanilla
  ikonu Haste. Vlastní stavová ikona by vyžadovala resource pack nebo mod.
- Smoker zpomaluje Rested ztrátu hladu, crafting table přidává Haste a
  nativní NekaraRPG Skills XP dostávají nakonfigurovaný Rested násobitel.
- Minihra začíná kliknutím prutem po záběru a odkládá původní `CAUGHT_FISH`
  item do finálního zásahu.
- Server nemůže ověřit klientský resource-pack zvuk, pouze jeho namespaced syntaxi.
- Úplná akceptace vyžaduje živý Purpur 26.1.2, protože pořadí Bukkit eventů a
  interakce pluginů nelze plně simulovat unit testy.
- Updater pouze připraví release a nikdy nerestartuje server. Úplný restart a
  kontrolu logů musí provést administrátor nebo hosting scheduler.

## Licence

Licence ve stylu MIT; viz `LICENSE`.
