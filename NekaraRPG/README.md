# NekaraRPG

Modulární RPG plugin pro Purpur/Paper 26.1 a Java 25. Obsahuje autentizaci,
rybářskou minihru, táboření/Rested, Echo Vein, mounty a nativní Nekara Skills.

## 2.4.0

Těžba a Kopání mají přepracované tematické perk stromy, blokově specifické poklady
a napojení Echo Vein na úroveň Těžby. Statkářství urychluje pouze přípravu jídla.

## Nevydáno – sběratelské perky a Nová hra+

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
