# Přehled změn

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
