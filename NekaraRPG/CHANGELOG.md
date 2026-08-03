# Přehled změn

## 2.1.0

- Všech 15 trénovaných dovedností má nativní runtime zdroj XP. Nové eventové
  listenery pokrývají Farming, Fishing, Trading, Smithing, Enchanting, Alchemy,
  Martial Arts, Light/Heavy Weapons, Archery a Light/Heavy Armor; Creative,
  Spectator, PvP/alt cíle, pasivní combat farmy a nepřiřazený automatický var jsou
  odmítnuté.
- Zakoupené perky jsou zapojené do sběru, výroby, obchodu, alchymie, rybaření,
  boje a armor setů. Aktivní techniky mají cooldown a bounded zásah; sekundární
  reflection damage je označený proti rekurzi a nepřidává XP.
- Zápisy XP používají omezenou frontu 8192 požadavků, jednoho asynchronního
  konzumenta a dávky po 256 místo samostatné scheduler úlohy pro každou akci.
  Přepočítané staty/mechaniky se cachují do změny revision profilu a žádná nová
  vertikála neskenuje svět po ticku.
- Po živém pádu packet encoderu byla odstraněna nebezpečná ProtocolLib metadata
  větev ležení. Vizuál nyní používá nativní Purpur/Paper mannequin se skinem a
  výbavou hráče v podporované sleeping póze; při nepodporované póze nebo chybě
  zůstane bezpečný serverový fallback. Kontrola sedadel byla snížena z každého
  ticku na dvakrát za sekundu.
- Každá z 15 trénovaných dovedností má vlastní složku s `config.yml` a
  `messages.yml`; Rubačina a Zeměrytectví mají navíc samostatné loot tabulky.
  Starý monolitický skill config se při prvním startu migruje bez přepsání již
  existujících modulárních souborů.
- Perk GUI používá rozpoznatelné ikony podle dominantního atributu nebo mechaniky.
  Kritický zásah, omráčení, krvácení, úhyb, odraz, výtěžek i ostatní definované
  statistiky tak nejsou schované pod obecnou barvou stavu.
- Krvácení lehkých zbraní je zapojené do ostrého combat listeneru. Aktivní efekty
  obsluhuje jedna fronta s limitem 2048 cílů a třemi sekundovými tiky; sekundární
  damage je označený proti rekurzi a nemůže znovu spustit perky ani XP.

- Schváleny české názvy všech šestnácti dovedností včetně `Obchodování` a
  `Hlavní úroveň`; názvy jsou nyní na jednom místě a používá je celé GUI.
- Přidán původní clean-room katalog 90 perků: každá z patnácti trénovaných
  dovedností má šest uzlů ve dvou větvích a vrcholový perk na úrovni 100.
  Typované efekty pokrývají veřejně popsané rodiny statistik a schopností, ale
  nepřebírají cizí názvy, texty, hodnoty ani rozložení.
- Kliknutí na dovednost otevře vlastní 54slotovou stezku. GUI rozlišuje naučené,
  dostupné a zamčené perky, ukazuje cenu, hodnost, potřebnou úroveň a předchůdce,
  dovoluje přepínat sousední stezky a při kliknutí na výplň zůstává otevřené.
- Nákup perku používá potvrzovací obrazovku a server znovu ověří rank, úroveň,
  předchůdce i společné Power body. SQLite compare-and-save zabraňuje přečerpání
  při souběhu a hráčský zámek tlumí dvojklik během probíhajícího zápisu.
- Přidána první nativní XP vertikála pro Hornictví. XP vznikají jen z fyzicky
  dokončeného `BlockBreakEvent` s krumpáčem, používají konfigurovatelnou tabulku
  bloků, časové limity aktivity v chunku, deduplikaci a atomický zápis profilu.
- Hráčem položené bloky se při zapnutém modulu značí v perzistentních datech
  chunku a Mining XP ani výtěžkový bonus za ně nevzniknou. Značky se čistí při
  vytěžení, výbuchu a přesunu pístem.
- `Hlas kamene` a `Srdce hory` jsou první živé perk efekty. Dvojitý a trojitý
  výtěžek se vzájemně vylučují a klonují pouze skutečné finální itemy z
  `BlockDropItemEvent`; loot tabulka, Fortune ani ValhallaMMO odměny se nehází
  znovu.
- Přidána auditovaná staging správa přes `/nrpg skills admin`: bezpečně umí
  profil prohlédnout, přidat zastropovaná XP, udělit validovaný rank perku a
  resetovat skill, perky nebo celý profil. SQLite schéma v2 migruje v1 data a
  zapisuje změnu profilu i identitu správce do jedné transakce.
- Dokončena nativní sběrná pipeline pro Hornictví, Rubačinu a Zeměrytectví. Všechny
  tři dovednosti získávají XP jen z fyzicky zahájeného a dokončeného hráčského
  rozbití správným nástrojem, sdílejí ochranu původu bloků, chunk limity, otisky
  akcí a atomický zápis profilu.
- Hornické perky nyní řídí násobné finální dropy, rychlost těžení, rychlost pece,
  `Žilobití` při plížení a omezený `Řízený odstřel` vlastním TNT. Rubačina přidává
  bezpečný `Pád velikána`, pět prken z vanilla receptu, vzácné nálezy v listí a
  výtěžkové perky. Zeměrytectví přidává XP, rychlost, násobné dropy a vážené
  nálezy včetně rozšíření perkem `Paměť střepů`.
- Hromadné schopnosti nikdy nenačítají cizí chunky, mají blokový rozpočet, dávku
  na tick a cooldown. Každý sekundární blok rozbíjejí přes `Player.breakBlock`,
  takže regionové pluginy mohou jednotlivé bloky zrušit a vanilla durability i
  loot proběhnou pouze jednou. Modul zůstává do živé akceptace výchozím stavem
  vypnutý a ValhallaMMO autoritou produkčního serveru.

## 2.0.0

- Založeno nativní jádro Nekara Skills se 16 dovednostmi: patnáct dovedností
  získává vlastní XP a šestnáctý Powerlevel se počítá jako zaokrouhlený průměr
  jejich úrovní. Každá dovednost má pevný strop 100.
- Přidána deterministická celočíselná XP křivka, neměnné profily a repository
  hranice s verzí záznamu. První transakční SQLite adaptér používá WAL, cizí klíče
  a optimistic revision; zastaralý souběžný zápis se celý vrátí zpět.
- Přidán validovaný acyklický perk graf, sdílený systém statistik a katalog
  aktivních mechanik. Power používá samostatné milníky, takže odměna mounta na
  úrovni 50 nemusí spojovat skill jádro přímo s implementací NekaraMounts.
- Bojové výpočty nyní mají čisté doménové jádro pro kritické zásahy, krvácení a
  omráčení. Sekundární poškození nemůže znovu spouštět další efekty.
- Dvojité a trojité dropy jsou vzájemně výlučný výsledek jedné odměnové cesty.
  Aktivní schopnosti mají povinnou kontrolu odemčení, cooldownu, ochrany regionu,
  nástroje a maximálního počtu zpracovaných bloků.
- XP ochrana odmítá zrušené, kreativní, syntetické a automatizované události,
  filtruje nepovolené hráčem položené zdroje, postupně tlumí opakované odměny v
  jednom chunku a deduplikuje stejný zdrojový otisk v omezené časové paměti.
- Atomická XP služba uplatní policy i deduplikaci před zápisem, zastropuje uložené
  XP přesně na úrovni 100 a při souběžném zápisu bezpečně načte novou revision a
  pokus zopakuje bez dvojí odměny.
- První 2.0 release zpřístupňuje základ k řízenému testování. Paper XP listenery,
  obsah všech perk stromů a živá migrace z ValhallaMMO budou následovat v dalších
  feature verzích. Vypnutelný modul `skills` nabízí bezpečný read-only 54slotový
  přehled a výchozím stavem zůstává vypnutý, takže stávající ValhallaMMO postup
  nepřebírá ani nezdvojuje.

## 1.10.1

- Tlačítka pro sezení a ležení jsou znovu dostupná přímo na hlavní obrazovce
  `/nrpg`; podnabídka Činnosti a Tábořiště zůstává zachovaná.
- Hráč si může lehnout kdekoliv. Rested, táborové účinky a přeskočení noci se
  nadále spustí pouze v radiusu zapáleného campfire nebo soul campfire.
- Opravena drobná poziční korekce sleeping pózy, která hráče bezprostředně
  postavila, ale ponechala běžet Campfire session. Pohyb je během ležení
  zablokovaný, otočení zůstává možné a přikrčení hráče bezpečně zvedne.

## 1.10.0

- Sezení je nově součástí modulu Campfire/Táboření; samostatný modul a jeho
  přepínač zmizely. Staré hodnoty ze `sitting/config.yml` se při prvním startu
  bezpečně převezmou pod `sitting` v `campfire/config.yml`.
- Hlavní GUI seskupuje Rybaření, Táboření a Těžbu do nové obrazovky Činnosti.
  Tábořiště má vlastní nabídku pro sezení, vstávání, ležení a ukončení ležení.
- U zapáleného campfire nebo soul campfire lze ulehnout bez postele. Ležení se
  počítá do Rested stejně jako sezení a ukončí se pohybem, teleportem, smrtí,
  odpojením nebo volitelně poškozením.
- Pokud ležící hráč zůstane jediným online hráčem na celém serveru, po krátkém
  čekání může v Overworldu přeskočit noc. Plugin nevolá CMI, nemění spawn hráče
  a při příchodu dalšího hráče ponechá pouze ležení bez zásahu do času.
- Správa NekaraRPG zůstává skrytá a znovu chráněná oprávněním
  `nekararpg.command.status`, které má výchozí hodnotu `op`.

## 1.8.1

- Opraveno management GUI koně, které rušilo také kliknutí ve spodním hráčském
  inventáři, takže nebylo možné vzít truhlu, sedlo ani koňské brnění na kurzor.
- Vhodnou obyčejnou truhlu, sedlo nebo koňské brnění lze nyní nasadit jedním
  běžným kliknutím přímo v hráčském inventáři. Nahrazený kus se vrátí na kurzor.
- Běžná práce s kurzorem zůstává dostupná, zatímco shift-click, hotbar přesuny a
  jiné zkratky zůstávají blokované proti obejití atomického uložení výbavy.

## 1.8.0

- NekaraMounts používá transakční SQLite úložiště. První start bezpečně importuje
  staré `mounts/data.yml`, původní soubor ponechá a vytvoří také
  `data.yml.pre-sqlite.bak`.
- Databázové zápisy brašen již nepřepisují celý YAML soubor. SQLite používá WAL,
  transakce, krátký busy timeout a při chybě dál uzamkne Mounts proti
  duplicitám nebo ztrátě obsahu.
- Přidány sdílené GUI položky, výplně a návratová tlačítka používaná centrálním
  menu, NekaraAuth a NekaraMounts.
- Centrální `/nrpg` nabízí osobní přehled účtu, odpočinku, právě probíhající
  činnosti a stavu koně. Oprávnění administrátoři mají samostatnou diagnostiku
  modulů, Mounts úložiště, integrací, paměti a updateru.
- Správa koně zobrazuje zdraví, stav, brašny a připravenost píšťalky přímo v GUI.
  Pokud cesta k hráči nepostupuje, kůň po šesti sekundách zvolí nový bezpečný
  přístupový bod bez teleportování nebo vytvoření další entity.
- Běžná odmítnutí interakcí s koněm a píšťalkou se přesunula z chatu do action
  baru. Delší vysvětlení zůstávají v popisech GUI.

## 1.7.0

- Aktivní kůň po doběhnutí přirozeně putuje v nastavitelném okruhu kolem místa
  písknutí. Opakované písknutí jej přesměruje k nové poloze hráče bez vytvoření
  další entity a používá samostatnou krátkou ochranu proti spamu.
- Nový kůň dostává sedlo. Do správy výbavy přibyl slot pro obyčejnou truhlu, která
  zpřístupní trvalé virtuální brašny o 54 slotech pouze přes GUI koně.
- Obsah brašen používá novou verzi datového schématu, ukládá se atomicky před
  každou změnou inventáře a přežije odvolání, smrt, reload i restart. Plnou truhlu
  nelze sundat a píšťalku do brašen nelze vložit.
- Opraveno nereagující písknutí při zrušení interakce jiným pluginem a falešná
  zpráva o uložení výbavy po kliknutí na prázdný slot.
- Menu mají návratová tlačítka a potvrzení citlivých akcí. Kliknutí na prázdné
  místo hráče z GUI nevyhodí a běžné vysvětlivky se zobrazují v popisu položek
  místo chatu.
- Centrální `/nekararpg` menu nabízí při načteném ValhallaMMO tlačítko pro
  otevření `/skills`. Hráčské texty Mounts a centrálního menu jsou kratší a více
  laděné do světa Nekary.

## 1.6.0

- Přidáno centrální hráčské GUI `/nekararpg` (také `/nekararpg menu`), které
  zobrazuje pouze právě aktivní moduly dostupné podle oprávnění hráče.
- Menu propojuje NekaraAuth, Fishing, Sitting, Campfire, Mining a NekaraMounts;
  příkazy zůstávají jako administrativní a nouzový fallback.
- NekaraAuth účet nově umožňuje bezpečnou změnu hesla přes tři kroky ve virtuální
  kovadlině: současné heslo, nové heslo a potvrzení nového hesla.
- Ověřování a hashování zůstává mimo hlavní serverové vlákno. Plaintext se
  neukládá ani neloguje a úspěšná změna ruší dřívější relog session.
- Přidána oprávnění, české zprávy, automatické kontroly YAML a jednotkový test
  zachování identity a auditních údajů účtu při výměně hashe.

## 1.5.1

- Navazující stabilní patch release pro instance, které už před publikací na
  GitHubu používaly interní sestavení `1.5.0`; vestavěný updater tak může bezpečně
  rozpoznat vyšší verzi a připravit ověřený JAR.
- Obsahuje migraci chybějícího jména mounta, minutový návrat po smrti a úplnou
  soulbound ochranu píšťalky včetně hopperů a dispenserů.

## 1.5.0

- Opraven pád `/nekararpg mount` při načtení staršího záznamu bez `custom-name`.
  Úložiště takový záznam atomicky doplní jménem `Bezejmenný` a doménový model už
  neplatné prázdné jméno nepřijme.
- Přidán samostatně zapínatelný modul `mounts` (NekaraMounts) pro jednoho trvalého
  vanilla koně na hráče.
- Přidána virtuální evidence koně bez ochočování: GUI pro prvotní výběr barvy a
  jména, správa jména, barvy a výbavy a administrátorské přidělení přes
  `/nekararpg mount grant <hráč>`.
- Svázaná píšťalka přivolá koně 7-12 bloků od hráče. Kůň doběhne na místo
  písknutí, čeká v jeho okolí a další použití má výchozí cooldown 30 sekund.
- Píšťalku nelze zahodit, uložit do cizího inventáře ani sebrat cizím hráčem.
  Management GUI a `mount whistle <restore|remove>` bezpečně udržují jedinou kopii;
  po smrti hráče se píšťalka vrátí. Plný inventář nikdy nevyhodí píšťalku na zem.
- Jméno, zdraví, maximální zdraví, rychlost, skok, barva, styl, sedlo, brnění,
  oheň, zamrznutí, vzduch a potion efekty se ukládají do atomicky zapisovaného
  `mounts/data.yml` za rozhraním `MountRepository`.
- Aktivní entity používají trvalý mount ID a owner ID; stale nebo duplicitní entity
  se při načtení chunku odstraní bez dropu uloženého vybavení.
- PvP okno se ukládá na disk a blokuje vytvoření, přidělení, přivolání i odvolání.
  Smrt ukládá minutový cooldown, který přežije reconnect, reload i restart.
- Cizí hráč nemůže mounta otevřít, osedlat ani na něj nasednout. Při chybě storage
  se modul uzamkne a existující entitu nemaže.
- Přidána typovaná konfigurace `mounts/config.yml`, české zprávy, persistence test,
  testy cooldownu a stabilní offline identity a živý Purpur akceptační postup.

## 1.4.0

- Rozdělen monolitický `config.yml`: kořenový soubor obsahuje pouze nastavení jádra,
  updater a přepínače modulů, zatímco moduly používají vlastní
  `<module>/config.yml`.
- Přidána jednorázová bezpečná migrace starého configu. Existující vlastní hodnoty
  se nejprve uloží do modulových souborů a až poté se odstraní z kořenového configu.
- Přidána krátkodobá relog session: přihlášený hráč se při návratu se stejným
  nickem a IP adresou do deseti minut automaticky ověří bez dalšího zadání hesla.
- Session zůstává pouze v paměti a ruší se při `/logout`, kicku, zrušení účtu,
  reloadu modulu i restartu serveru. Změna IP nebo vypršení platnosti vyžaduje login.
- Dvouminutový limit přihlášení nyní ukazuje průběžný odpočet v action baru a po
  vypršení hráče automaticky odpojí.
- Hlavní položka přihlašovacího a registračního GUI používá hlavu a skin právě
  připojeného hráče místo obecné ikony.
- `/login` a `/register` jsou pro běžné hráče výchozím stavem vypnuté. Nouzový
  fallback vyžaduje současně konfiguraci `auth.commands.fallback-enabled: true`
  a oprávnění `nekararpg.auth.fallback-commands`.
- Přidány jednotkové testy relog session a kontroly nových výchozích hodnot YAML.

## 1.3.0

- Přidán samostatně zapínatelný modul `auth` (NekaraAuth) pro registraci,
  přihlášení a ochranu nicků na offline-mode serveru.
- Přidáno výchozí souborové úložiště `auth/accounts.yml` za rozhraním
  `AccountRepository`, připraveným pro pozdější databázový backend propojený s webem.
- Hesla se ukládají pouze jako PBKDF2-HMAC-SHA256 hash s unikátní 128bitovou solí,
  výchozími 600 000 iteracemi a verzovaným formátem; plaintext se nezapisuje.
- Přidáno herní GUI účtu a zadávání hesla přes virtuální kovadlinu s dvojím
  potvrzením registrace. `/login` a `/register` zůstávají jako fallback.
- Nepřihlášeným hráčům se blokuje pohyb, teleport, chat, herní příkazy,
  inventáře, interakce, boj, stavění, těžení, hlad a manipulace s itemy.
- Přidána ochrana přesného zápisu registrovaného nicku, pět pokusů, minutový
  lockout, dvouminutový timeout a fail-closed odmítnutí loginu při chybě storage.
- Přidány administrativní příkazy `/nekaraauth status` a
  `/nekaraauth unregister <hráč>`; nepřihlášený operátor je nemůže použít.
- Přidána přechodová pojistka: pokud je při startu aktivní AuthMe, NekaraAuth se
  nezapne. Po odstranění AuthMe převezme autentizaci při následujícím plném restartu.
- Přidány jednotkové testy hashování, neplatných hashů, normalizace lockoutu a
  výchozích auth hodnot v YAML resources.

## 1.2.5

- Odhalování rud nově zohledňuje výšku. Overworld kandidáti respektují vanilla
  generační rozsahy a relativní výškové váhy, takže hlubinné rudy jako diamant
  a redstone nemohou vzniknout ve vysoké poloze.
- Přidána pravidla pro železo ve vysokých polohách, měď Y 0-96, zlato pod Y 32,
  lapis pod Y 64, diamant/redstone pod Y 16, uhlí nad Y 0 a vyšší Badlands zlato.
  Přesný seedový noise worldgenu ani potlačení rud vystavených vzduchu se
  záměrně nereprodukují.
- Odhalování v netherracku je omezené na běžný netherový rozsah Y 10-117. End
  stone stále nemá umělou přeměnu na rudu.
- Přidán samostatný zvuk odhalení rudy s jasnějším tónem amethyst cluster.
  Původní hlubší amethyst chime zůstává odlišným zvukem objevení žíly.
- Přidány čisté testy výškových hranic, váhy hlubinných rud, Badlands zlata,
  netherových hranic a váženého výběru.

## 1.2.4

- Výchozí šance Echo Vein zvýšena ze 4 % na 5 % a odstraněn hráčský cooldown.
  Stávající výchozí hodnota 4 % se migruje na 5 %; staré `cooldown-seconds` a
  uložené timestampy cooldownu se ignorují.
- Automatické spuštění a cíle jsou omezené na stone, deepslate, netherrack a
  end stone, aby aktivitu poháněla běžná těžba a přirozené rudy zůstaly výjimečné.
- Přirozené dokončení už neprobíhá kliknutím na značku, ale skutečným vytěžením
  označeného bloku. Jeho finální ValhallaMMO Mining XP tvoří jednorázový 25%
  bonus `PLUGIN` a jeho vlastní finální dropy poskytují volitelný bonusový item.
- Dokončený cíl má 50% šanci pokračovat na viditelný blok sousedící stěnou.
  Řetězený cíl nahradí běžný zvuk úspěchu dalším zvukem objevení a zároveň
  nemůže provést nezávislý 5% hod.
- Při zahájení těžby označeného bloku přidán jednorázový 25% hod na odhalení
  rudy. Stone a deepslate používají vážené Overworld rudy, netherrack quartz
  nebo Nether gold a end stone zůstává beze změny.
- Debug logy nově auditují spouštěcí XP, XP cílového bloku, udělený bonus,
  bonusové dropy, odhalení rud a výsledek řetězení.

## 1.2.3

- Herní modul přejmenován z `echo-vein` na `mining` (`NekaraMining`). Echo Vein
  zůstává jeho první aktivitou a starý přepínač modulu se migruje, pokud ještě
  neexistuje `modules.mining.enabled`.
- Aktivní Echo Vein pokračuje, když hráč udeří do jiného bloku nebo ho vytěží;
  dokončí ji pouze označený cíl. Timeout a dosavadní pravidla čištění zůstávají.
- Odstraněny všechny přirozené Echo Vein zprávy v chatu. Časovač v action baru
  zůstává a příkazové testy si ponechávají administrativní zpětnou vazbu.
- Zvuk objevení se přehraje jen jednou při vzniku žíly, ne s každým částicovým
  pulzem. Přirozený úspěch má jeden zvuk dokončení a timeout je tichý.

## 1.2.2

- Echo Vein zpřístupněna na každé ValhallaMMO Mining úrovni; staré nastavení
  `minimum-mining-level` se už nepoužívá.
- Cíle jsou omezené na bloky v Paper tagu `MINEABLE_PICKAXE`, takže se žílou
  nemůže stát dirt, gravel, wood ani jiný nevhodný jeskynní blok.
- Přidán vrstvený pulz s jemnou nápovědou v okolí jednoho bloku a hustším
  efektem na viditelné stěně cíle.
- Přirozený chat zredukován na jedinou zprávu o objevení se zbývajícím časem.
  Úspěch a neúspěch používají pouze zvukovou a vizuální zpětnou vazbu.
- `/nekararpg test vein` zůstává bez odměny; bonusová XP a jeden finální drop
  náleží pouze přirozeně spuštěné Echo Vein.

## 1.2.1

- V prvním patch releasu určeném pro úplný test updateru z ručně nainstalované
  verze 1.2.0 publikována kompletní Mining aktivita Echo Vein.
- Podmínka Mining levelu 50, ValhallaMMO XP bonus, bonus z finálních dropů,
  trvalý cooldown a bezodměnové chování `/nekararpg test vein` zůstaly beze změny.
- Oproti 1.2.0 se nezměnil gameplay ani výchozí konfigurace.

## 1.2.0

- Přidán volitelný modul `echo-vein`: hráči s Mining levelem 50 mohli vzácně
  odhalit pulzující blok a do šesti sekund ho zasáhnout krumpáčem.
- Přidán nastavitelný osmiminutový trvalý cooldown, 4% šance, radius hledání,
  časování pulzu, částice a `/nekararpg test vein`.
- Přidán 25% bonus z finální hodnoty ValhallaMMO Mining XP s důvodem `PLUGIN`,
  aby se Rested a globální násobitele neaplikovaly podruhé.
- Přidán jeden bonusový item vážený podle množství ze skutečných finálních
  přirozených a Valhalla-prepared Mining dropů spouštěcí akce. Množství je
  omezené na jeden item a metadata zůstávají. Nepoužívá se Digging treasure
  tabulka ani syntetický loot.
- Přidán updater napojený na nejnovější stabilní release
  `Jonaczech/nekara-plugins`, s automatickými kontrolami a příkazem
  `/nekararpg update check|status`.
- Přidáno přísné ověření stabilního assetu `NekaraRPG.jar`: důvěryhodná GitHub
  URL, limit velikosti, SHA-256, descriptor JARu, identita produktu a sémantická
  verze musí před přípravou aktualizace souhlasit.
- Ověřené aktualizace se připravují do update složky Paperu pro instalaci při
  dalším úplném restartu; aktivní JAR se za běhu nikdy nenahrazuje.
- Před přípravou aktualizace se vytvoří záloha běžícího JARu ověřená hashem, aby
  měl administrátor ruční možnost návratu.
- Přidány nastavitelné intervaly, automatické stažení, upozornění administrátora
  při připojení, timeouty, oprávnění a bezpečné hlášení stavu.
- Přidán nastavitelný 10% Rested XP bonus pro každý ValhallaMMO skill.
- XP bonus omezen na běžné skill akce a sdílené XP; příkazy, resety, redemption
  a migrační refundy zachovávají přesnou hodnotu.
- Odložené rybářské ValhallaMMO XP zůstává kompatibilní s Rested bez druhého
  přičtení bonusu při úspěšném doručení úlovku.
- Snížená ztráta hladu Rested nově závisí na smokeru v nabitém táboře. Samotný
  Rested už hlad nezpomaluje.
- Při chybějícím ValhallaMMO nebo nekompatibilním event API zůstává spuštění
  bezpečné a běžná skill XP se nemění.

## 1.1.0

- Přidán modul `sitting` s `/nekararpg sit` a `/nekararpg stand`.
- Přidána neviditelná, netrvalá armor-stand sedadla s čištěním při sesednutí,
  teleportu, smrti, odpojení, poškození, vypnutí modulu a ukončení pluginu.
- Přidán modul `campfire` pro sedící hráče u zapáleného campfire a soul campfire.
- Přidáno pomalé léčení, blokování ztráty hladu a malé nastavitelné doplnění hladu.
- Po 20 sekundách přidán skutečným časem měřený Rested bonus s výchozí délkou
  pět minut, který snižoval průměrnou ztrátu hladu.
- Přidány skupinové násobitele léčení a doplňování hladu pro každý oheň.
- Přidány nastavitelné české roleplay a progress zprávy v action baru.
- Nové zabalené zprávy fungují jako runtime defaults bez přepsání uživatelského
  `messages.yml` při upgradu.
- Nabitý Rested se obnovuje, dokud hráč zůstává u ohně, takže po postavení získá
  celou nakonfigurovanou délku.
- Přidány počty aktivních stavů modulu, oprávnění, konfigurace, unit testy
  výpočtu odpočinku, ověřený release postup a dokumentace živých testů.
- `/sit` zůstává neregistrovaný kvůli kolizím s CMI a jinými sitting pluginy.
- Výchozí pozice sedadla NekaraRPG doladěna na `0.20`; dřívější předprodukční
  posuny se při načtení stávající testovací konfigurace migrují.
- Přidána nastavitelná detekce externích sedadel s výchozím `ARMOR_STAND`, aby
  Campfire rozpoznal CMI sedadla bez převzetí CMI příkazů.
- Campfire oddělen od interního Sitting modulu při použití externího poskytovatele.
- Přidány nastavitelné částice aktivního odpočinku a volitelné Rested částice.
- Přidán řízený Haste s běžnou stavovou ikonou Minecraftu; silnější nebo
  nesouvisející externí Haste zůstává nedotčený.
- Přidán kompaktní opakovaný časovač Rested v action baru, který ustupuje
  roleplay nabíjení a rybářské minihře.
- Po dokončení 20sekundového nabíjení přidán jemný ametystový zvuk.
- Přidána kvalita vybavení tábora: každý unikátní nakonfigurovaný typ bloku do
  pěti bloků přidá jednu minutu Rested, maximálně 12 minut se všemi defaults.
- Haste I nově závisí na crafting table v táboře místo každého holého campfire.
- Časovač Rested stylizován jako stručný český text `Odpočatý | m:ss` bez
  bossbaru, který by se mohl plést se zdravím MythicMobs.
- Přidán výslovný fallback zabalených zpráv pro servery se starším `messages.yml`.
- Přidány bezpečné tábory řízené bedem s nastavitelným radiusem 24 bloků, které
  blokují přirozený vanilla spawn nepřátel bez mazání existujících mobů.
- Přidána volitelná integrace MythicMobs 5.12.1 před spawnem. Blokuje jen
  přirozené náhodné spawny nastavené frakce `NekaraHostile` a ponechává faunu i
  skriptovaná setkání.
- Staging deploy skript odmítne duplicitní verzovaný nebo starý Nekara plugin
  JAR před nahrazením stabilního `NekaraRPG.jar`.
- Release artefakty sjednoceny na jediný stabilní název `NekaraRPG.jar`; verze
  je uložena v metadatech pluginu, changelogu a Git tazích.

## 1.0.1

- Opraven `/nekararpg test`, aby se samostatná rybářská minihra správně spustila.
- Testovací relace se před spuštěním inicializuje do `WAITING_FOR_BITE`. Bez
  tohoto přechodu ji kontrola aktivní minihry odmítla a rozhraní se nevykreslilo.

## 1.0.0

- Plugin přejmenován z NekaraFishing na NekaraRPG.
- Přidáno modulární jádro s prvním výchozím modulem `modules.fishing.enabled`.
- Přidány `/nekararpg` a `/nrpg`; `/nekarafishing` a `/nfishing` zůstávají jako
  staré aliasy.
- Přidána nová oprávnění `nekararpg.*` při zachování starých `nekarafishing.*`.
- Zachována produkční rybářská minihra `DEFERRED_CATCH` pro Purpur 26.1.2.
- Zachován původní ItemStack úlovku, vanilla XP, přehrání profesních XP
  ValhallaMMO a připravené extra dropy bez syntetických fishing eventů a
  vlastních loot tabulek.
- Zachováno tiché chování chatu, časování v action baru, BossBar postupu,
  částice u splávku, efekty úspěchu/neúspěchu, přitažení háčku, časový bonus a
  úrovně obtížnosti podle ValhallaMMO.
