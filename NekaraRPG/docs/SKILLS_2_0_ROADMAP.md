# Nekara Skills 2.0 roadmap

Tento dokument rozděluje přechod z externího ValhallaMMO postupu na vlastní
autoritativní systém NekaraRPG. Architektonická rozhodnutí a licenční hranice jsou
v `docs/adr/0001-native-skills-platform.md`.

## Cílový katalog

Hráč uvidí šestnáct dovedností:

1. Power
2. Martial Arts
3. Trading
4. Smithing
5. Enchanting
6. Alchemy
7. Mining
8. Woodcutting
9. Digging
10. Farming
11. Fishing
12. Light Weapons
13. Heavy Weapons
14. Archery
15. Light Armor
16. Heavy Armor

Power nepřijímá přímé XP. Jeho úroveň je celočíselný průměr zbývajících patnácti
dovedností a poskytuje společné perk pointy a obecné milníky. Prvním plánovaným
milníkem je možnost odemknout mounta při Power 50; samotné příběhové udělení může
nadále vlastnit BetonQuest.

## Perk stromy

Každá přímo trénovaná dovednost dostane vlastní původní acyklický strom. Cílem je
srovnatelná hustota a hloubka jako u současného ValhallaMMO, včetně stejného počtu
uzlů tam, kde lze počet spolehlivě zjistit z veřejně dostupného hráčského
rozhraní. Přebírá se pouze tato číselná velikost stromu, nikoliv názvy, texty,
hodnoty, vazby, ikony či rozložení. Zjištěné počty musí být před tvorbou obsahu
zapsány jako samostatná ověřitelná tabulka; do té doby se přesná shoda netvrdí.

Každý uzel musí mít stabilní ID, pozici v 9×6 viewportu, maximální rank, cenu,
požadovanou úroveň, předchůdce a typované efekty. Konfigurace s cyklem, chybějícím
předchůdcem, kolizí pozic nebo cizím stromem se při startu odmítne.

## Implementační fáze

### 1. Doménové jádro — hotovo

- katalog 16 dovedností a strop 100,
- deterministická celočíselná XP křivka,
- odvozený Powerlevel,
- neměnný hráčský profil a repository rozhraní,
- validace perk DAG,
- skládání statistik bez nechtěného stackování stejného zdroje,
- nerekurzivní kritické zásahy, krvácení a omráčení,
- vzájemně výlučné dvojité/trojité dropy,
- bezpečnostní rozhodnutí pro aktivní schopnosti,
- XP policy a časově omezená deduplikace zdrojů.

### 2. Perzistence a transakce — rozpracováno

- hotovo: SQLite schéma v2 s WAL, cizími klíči a optimistic revision,
- hotovo: atomický zápis XP, perk ranku a utracených bodů,
- hotovo: transakční migrace v1 → v2 a audit administrativních změn,
- zbývá: jednorázové milníky,
- hotovo: slučitelné XP přírůstky procházejí omezenou frontou s kapacitou 8192 a
  dávkou nejvýše 256 zápisů,
- hotovo: fail-closed chování při chybě storage,
- zbývá: export a záloha bez editace za běhu.

### 3. GUI a navigace — hotovo pro katalog 2.1.0

- bezpečně vypnutelný 54slotový přehled všech dovedností, hlavní úrovně a XP,
- asynchronní čtení profilu a návrat do centrálního `/nrpg`,
- 90 původních uzlů, šest na každou trénovanou dovednost ve dvou větvích,
- detail dovednosti, přepínání sousedních stezek a stav naučeno/dostupné/zamčeno,
- dostupné body a srozumitelné důvody zamčených perků,
- bez zavírání při kliknutí na prázdný slot,
- vlastní vizuální jazyk Nekary; žádná kopie cizího layoutu či textů.

### 4. Bezpečný nákup perků — hráčský průchod hotov

- hotovo: potvrzovací dialog,
- hotovo: serverová kontrola úrovně, bodů, ranku a předchůdců,
- hotovo: atomické compare-and-save nad revision profilu,
- hotovo: zámek souběžného nákupu jednoho hráče a bezpečné znovunačtení profilu,
- hotovo: administrativní grant/reset s auditem; grantovaný rank započítává
  katalogovou cenu a reset perků vrací celý uložený rozpočet.

### 5. Sběrné dovednosti — pět vertikál v kódu

- hotovo v kódu: Mining, Woodcutting a Digging získávají XP z fyzicky zahájeného
  a dokončeného rozbití správným nástrojem,
- hotovo v kódu: perzistentní původ bloku, hráčem položené bloky, chunk heat,
  deduplikace zdroje a atomický zápis profilu pro všechny tři vertikály,
- hotovo v kódu: dvojitý/trojitý výtěžek klonuje skutečné finální vanilla/custom
  dropy namísto opakovaného volání loot tabulky,
- hotovo v kódu: rychlost nástroje, hornická pec, Žilobití, Řízený odstřel, Pád
  velikána, úsporná prkna a vzácné nálezy Zeměrytectví i listí,
- hromadné schopnosti používají skutečný hráčský break pro každý blok, nevynucují
  zrušené regionové bloky, nenačítají chunky a respektují limity, cooldown i
  vanilla durability,
- hotovo v kódu: Farming ověřuje zralost, násobí pouze finální dropy, umí bezpečně
  sklidit a znovu zasadit nejvýše devět bloků a zrychluje jen chunky s nedávným
  hráčským správcem; Fishing navazuje na skutečně doručený deferred catch,
- zbývá před produkcí: živá Purpur/Lands/exploit akceptace všech pěti vertikál.

### 6. Bojové dovednosti — runtime baseline v kódu

- hotovo v kódu: Martial Arts, Light/Heavy Weapons, Archery a Light/Heavy Armor
  mají XP pouze proti nepřátelům a jednotnou pipeline poškození,
- hotovo v kódu: damage, critical, power attack, armor, dodge, reflection, parry,
  charged shot, ammo save, armor sety, adrenalin/rage a omezené aktivní techniky,
- hotovo v kódu: sekundární reflection damage je označený a nemůže tvořit proc/XP
  smyčku; cooldowny jsou paměťové, omezené na online hráče a bez tickeru,
- hotovo v kódu: critical, stun a bleed mají oddělené proc pojistky; bleed používá
  centrální registr nejvýše 2048 cílů a perk GUI volí ikonku podle dominantního efektu,
- zbývá před produkcí: živé vyvážení proti MythicMobs bossům, custom itemům a
  měření souběhu s ValhallaMMO.

### 7. Výrobní dovednosti a obchod — runtime baseline v kódu

- hotovo v kódu: Smithing, Enchanting, Alchemy a Trading získávají XP jen z
  dokončených nativních událostí a používají typ výsledku/recipe místo názvu itemu,
- hotovo v kódu: kvalita, úspora zdrojů a XP, enchant power, potion power/speed,
  obchodní reputace, slevové refundy a omezené dary,
- automatický brewing bez nedávné ruční práce hráče není připsán žádnému profilu,
- zbývá před produkcí: ekonomické vyvážení a živé testy shift-clicku, hopperů,
  custom receptů a souběžných obchodů dvou hráčů.

### 8. Migrace a vydání

- hotovo: 2.1.0 je vydané jako bezpečně vypnutý staging baseline všech 15 vertikál,
- read-only export současného postupu z podporovaného veřejného API nebo dat,
- mapovací report a záloha před jediným importem,
- období porovnávací telemetrie bez dvojího udělování odměn,
- BetonQuest podmínky a Power milestone pro NekaraMounts,
- zátěžové, exploit a živé Purpur testy,
- odstranění ValhallaMMO až po potvrzeném paritním provozu a rollback plánu.

## Blokátory plné aktivace Nekara Skills

Modul smí být ve výchozím stavu zapnutý a převzít autoritu od ValhallaMMO až poté,
co všech patnáct dovedností má zdroj XP, původní perk strom, funkční efekty a
živé exploit testy; GUI umí bezpečný nákup; a je ověřena migrace, restart, rollback a
souběžný zápis profilu na živém Purpur prostředí. Release 2.1.0 vydává kompletní
runtime baseline, ale ponechává jej bezpečně vypnutý pro řízené testování.
