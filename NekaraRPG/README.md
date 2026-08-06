# NekaraRPG

Modulární RPG plugin pro Purpur/Paper 26.1 a Java 25. Obsahuje autentizaci,
rybářskou minihru, táboření/Rested, Echo Vein, mounty a nativní Nekara Skills.

## 2.5.1 – plazení, ležení a ikony perků

- `/nekararpg crawl` přepíná serverem řízené plazení se skutečným nízkým hitboxem.
  Je dostupné všem hráčům, funguje i z `/nrpg`, klientská klávesa je ve výchozím stavu
  `C` a Shift plazení ukončí. Při plazení lze plynule vystoupat na překážku vysokou blok.
- Ležení zobrazuje stabilní mannequin natočený nohama ve směru pohybu; změna pohledu
  hráče ležící animaci neruší. Dřívější výchozí natočení `-90°` se při upgradu opraví.
- Perk-tree používá pro každý perk jedinečnou tematickou ikonu.

## 2.5.0 – boj, výstroj, rybaření a perk-tree

- Perk strom Lehké zbraně tvoří siluetu meče a zachovává stabilní interní ID
  uložených ranků. Rychlejší útok platí pouze při držení meče, dýky nebo kopí.
- Nové perky posilují poškození, kritické zásahy a krvácení; materiálové
  požadavky na výrobu i plné používání dřevěných až netheritových zbraní se
  nemění.
- Nanášení lektvarového účinku na zbraň je odemknutí Alchymie přes Bojové esence.
- Těžké zbraně používají asymetrický strom ve tvaru sekyry. Plně nabitý útok je
  společný základ stromu a perk Široký rozmach mu dává plošný účinek odlišný pro
  sekeru, obouruční meč a kladivo.
- Kopí má při prázdné vedlejší ruce dosah +1,25 bloku; vlastní dýky mají rychlejší
  základní útok, ale dosah kratší o 0,5 bloku. Požadavky levelu na materiál zbraně
  jsou zachované.
- Stínový oděv poskytuje až +30% účinnost zbroje, +30% úsporu hladu, +20% úhyb a
  bez pohybové zátěže. Perk Vynalézavý tulák zpřístupní setové bonusy i se třemi
  kusy čistě lehké zbroje; Adrenalin při ≤25% zdraví na 5 s udělí Rychlost II a
  Regeneraci I s cooldownem 60 s.
- Plátová ochrana má šest perků ve vlastní siluetě: Zpevněná výstroj, Nohy z oceli,
  Vitální ocel, Hněv, Vynalézavý pěšák a Ostnatý Juggernaut. Kompletní sada dává
  obranu, léčení, odraz poškození, odolnost proti knockbacku a imunitu vůči Slowness,
  Weakness a Levitation; Hněv má cooldown 60 s.
- Rybaření používá vlastní doplňkovou tabulku pokladů vedle původního vanilla úlovku:
  mimo jiné prismarine, slime ball, echo shard, armor trimy, netherite upgrade a
  velmi vzácně nether star. Globální Štěstí ovlivňuje poklady i Potopenou schránku.
  Schránka je na 90 s zamčená pouze pro rybáře a nelze ji vytěžit jiným hráčem ani
  vybrat pomocí hopperu. Naladění vody přidává až 10 stacků po 2% šance na poklad,
  které jsou vidět jen jako vodní částice u háčku.
- New Game+ zvyšuje přirozenou šanci na dvojitý úlovek stejně jako u ostatních
  sběratelských dovedností (`×1,25` za rank).
- Cesty perk stromů jsou zkrácené na 2–3 mezilehlé sloty. Každá aktivní dovednost
  má odlišnou tematickou siluetu; rohové spoje zůstávají viditelné ve viewportu.
- Pro testování jsou dostupné příkazy `/nekararpg skills admin max <hráč>` (level 100,
  všechny perky a New Game+) a `/nekararpg skills admin reset-all <hráč>` (úplný reset).

## 2.4.2

Řemeslo nyní odděluje odemčení materiálu od kvality vyrobené výbavy. Po perku
`Poctivé řemeslo` je kvalita alespoň Neobyčejná; další perky a globální Štěstí
zvyšují šanci na Vzácnou, Epickou a Legendární kvalitu. Kovová výbava prochází
krátkým dokončením v peci a kotlíku, zbraně navíc broušením.

Přehled se jmenuje `Dovednosti`. Ikona otevřené dovednosti v perk stromu ukazuje
aktuální bonusy a aktivní perky včetně přesných hodnot. Zadání pro budoucí vlastní
ikony dovedností je v [docs/SKILL_ICON_BRIEFS.md](docs/SKILL_ICON_BRIEFS.md).

## 2.4.1

Dovednosti jsou v hlavním menu uprostřed jako kniha s brkem. Přehledy dovedností i
perků používají kompaktní tooltipy bez duplicit. Kopání má rozšířenou tabulku
tematických nálezů podle bloku. Těžba, Lesnictví a Kopání nevyžadují pro běžný postup
správný nástroj; Statkářství ponechává motyku pro aktivní sklizeň a Field Harvest.

## 2.4.0

Těžba a Kopání mají přepracované tematické perk stromy, blokově specifické poklady
a napojení Echo Vein na úroveň Těžby. Statkářství urychluje pouze přípravu jídla.

### Sběratelské perky a Nová hra+

- Pětihodnostní perky už nelze koupit celé hned na začátku: kořenový uzel
  vyžaduje pro hodnosti `1–5` levely `0 / 10 / 20 / 35 / 50` a navazující
  pětihodnostní uzel `20 / 35 / 50 / 70 / 85`. Požadavek platí pro každou
  aktivní dovednost, ověřuje jej server při nákupu a GUI ukazuje požadavek
  následující hodnosti.
- Statkářství stále používá šest uzlů: Plná ošatka, Živá půda, Péče o stádo,
  Obratná sklizeň, Včelařova péče a Záběr pole.
- Plná ošatka poskytuje samostatný roll dodatečné sklizně až `20 %` na ranku 5;
  nemíchá se s capovaným double dropem z levelu.
- Péče o stádo zrychluje růst zvířat, posiluje jejich loot i XP z rozmnožování.
  Včelařova péče uklidňuje včely a s `50 %` šancí obnoví med sklizeného úlu.
- Záběr pole je plížením aktivovaná schopnost na 8 sekund: sklizeň a opětovné
  sázení zralých plodin v ploše `5×5`, s cooldownem 45 sekund.
- Nová hra+ má `50 %` XP, `+25 %` síly perk statistik a u Statkářství trvalý
  nezávislý `30 %` roll dodatečného výtěžku ze sklizně a zvířat zabitých hráčem.
- Lesnictví má přepracované uzly Míza lesa, Jistý zásek/Arborista, Aktivní život,
  Pád velikána, Zlaté listí a Křišťálové listí. Zlaté jablko z listí používá
  samostatný nízký roll bez globálního Lucku; Nová hra+ přidává nezávislý `30 %`
  roll dodatečného logu nebo stemu z přirozeně pokáceného stromu.
- Kopání nyní používá Kopáče (`+6 %` rychlosti a `+0,4` vanilla XP za rank),
  Bagr (`+10 %` rychlosti za rank), Síto (`+2,5 %` vzácného nálezu a `+2` XP),
  Archeologa, Replikaci zeminy a Skrytý poklad (`+5 %` vzácného nálezu).
  Archeolog obnoví po dokončeném čištění suspicious sand/gravel s `20 %` šancí;
  Replikace zeminy zpřístupní výrobu 4 grass blocků nebo rooted dirtů ze 4 hlíny,
  příslušné přírodní suroviny a bone mealu. Nová hra+ Kopání přidává samostatný
  `30 %` roll dalšího finálního dropu z přirozeně vykopaného bloku.
- Poklady Kopání mají globální fallback (železný/zlatý nugget, ametyst, prismarine)
  a specifické tabulky podle bloku. Písek dává mořské nálezy, štěrk flint a vzácně
  emerald, zemina bone/roots/moss, mud slime ball, soul bloky nether suroviny a
  sníh ledové suroviny. Specifická tabulka má vždy přednost.

## 2.3.3 – postup, výbava a sjednocené GUI

- 13 aktivních dovedností a odvozená Hlavní úroveň.
- `Umění dlaně` a `Obchodování` jsou interně zachované, ale pro hráče neaktivní.
- Každá dovednost má vlastní tematickou perk-tree siluetu, kompaktní cesty a
  New Game+ s ikonou Trial Chamber klíče vedle startovního perku.
- Šipky jsou samostatné navigační sloty a perk ani cesta se pod nimi nevykreslí.
- Tooltipy ukazují pouze relevantní účinek a konkrétní postup odemčení.
- Hlavní úroveň je hráčská hlava; na úrovni 1 automaticky aktivuje Tábořiště a
  Rested, na úrovni 25 zviditelní a odemkne Mého koně.
- Hlavní menu neobsahuje samostatné Činnosti. Všechna tlačítka návratu používají
  stejný grafický model jako perk-tree.
- Těžba, Lesnictví, Kopání, Statkářství a Rybaření získávají `0,20 %` přirozené
  šance na dvojitý výtěžek za úroveň až do `20 %` na úrovni 100.
- New Game+ má `75 %` XP, `+10 %` perk statistik a `×1,25` přirozeného double dropu
  za rank.

## Resource pack

Finální modely perk-tree, cest a vlastních zbraní poskytuje
[`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack).
Bez packu plugin použije vanilla fallbacky.

## Build

```text
scripts\build-release.cmd
```

Výstup je `dist/NekaraRPG.jar`. Skript spustí testy i produkční build.

## Bezpečné nasazení

Server nejdříve zastav. Potom použij
`C:\Users\jonac\Documents\Nekara\FTP\deploy-nekararpg-safe.ps1` s parametrem
`-Artifact <cesta-k-NekaraRPG.jar>`. Skript vytvoří obě zálohy a ověří SHA-256.
Po výměně je nutný plný restart.
