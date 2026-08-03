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

- hotovo: SQLite schéma v1 s WAL, cizími klíči a optimistic revision,
- hotovo: atomický zápis XP, perk ranku a utracených bodů,
- zbývá: verzované migrace schématu a jednorázové milníky,
- write-behind fronta pouze pro slučitelné XP přírůstky,
- fail-closed chování při chybě storage,
- export, záloha a administrativní audit bez editace za běhu.

### 3. Read-only GUI a navigace — rozpracováno

- hotovo: bezpečně vypnutelný 54slotový přehled všech dovedností, Power a XP,
- hotovo: asynchronní čtení profilu a návrat do centrálního `/nrpg`,
- zbývá: detail dovednosti s viewportem perk grafu inspirovaným dodanou referencí,
- středový perk graf, směrové ovládání a trvalá lišta dovedností,
- XP progress, dostupné body a srozumitelné důvody zamčených perků,
- návrat do centrálního `/nrpg`, bez zavírání při kliknutí na prázdný slot,
- vlastní vizuální jazyk Nekary; žádná kopie cizího layoutu či textů.

### 4. Bezpečný nákup perků

- potvrzovací dialog,
- serverová kontrola úrovně, bodů, ranku a předchůdců,
- atomické compare-and-save nad revision profilu,
- idempotentní zpracování dvojkliku a packet replay,
- administrativní reset s auditem a explicitní cenovou politikou.

### 5. Sběrné dovednosti

- Mining, Woodcutting, Digging, Farming a Fishing,
- skutečné finální vanilla/custom dropy namísto syntetických náhrad,
- původ bloku, hráčem položené bloky, chunk heat a automatizace,
- Vein Mining, Tree Feller a Field Harvest s ochranou regionu, blokovým limitem,
  cooldownem, spotřebou durability a jedním odměnovým průchodem.

### 6. Bojové dovednosti

- Martial Arts, Light/Heavy Weapons, Archery a Light/Heavy Armor,
- jednotná pipeline poškození a typovaná provenance,
- kritické zásahy, krvácení, omráčení, parry a aktivní schopnosti,
- imunity bossů, PvP pravidla, cooldowny a zákaz sekundárních proc smyček,
- kompatibilita s MythicMobs a custom itemy přes úzké adaptéry.

### 7. Výrobní dovednosti a obchod

- Smithing, Enchanting, Alchemy a Trading,
- důvěryhodná identita receptu/itemu a ochrana proti přejmenovaným kopiím,
- žádné XP za zrušený craft, shift-click replay, opakované výstupní eventy nebo
  administrativně vytvořený předmět,
- economy pouze přes adaptér; žádná pevná závislost v doménovém jádře.

### 8. Migrace a vydání

- read-only export současného postupu z podporovaného veřejného API nebo dat,
- mapovací report a záloha před jediným importem,
- období porovnávací telemetrie bez dvojího udělování odměn,
- BetonQuest podmínky a Power milestone pro NekaraMounts,
- zátěžové, exploit a živé Purpur testy,
- odstranění ValhallaMMO až po potvrzeném paritním provozu a rollback plánu.

## Blokátory plné aktivace Nekara Skills

Modul smí být ve výchozím stavu zapnutý a převzít autoritu od ValhallaMMO až poté,
co všech patnáct dovedností má zdroj XP, původní perk strom, funkční efekty a
exploit testy; GUI umí bezpečný nákup; a je ověřena migrace, restart, rollback a
souběžný zápis profilu na živém Purpur prostředí. Release 2.0.0 vydává pouze
bezpečně vypnutý základ pro řízené testování.
