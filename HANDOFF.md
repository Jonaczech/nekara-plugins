# Předání projektu Nekara Plugins

## Aktuální stav – NekaraRPG 2.3.3

`NekaraRPG` je nativní autorita RPG postupu. XP z Rested, Rybaření a Echo Vein
se připisují přímo do Nekara Skills. Plugin využívá klientský resource pack z
[`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack)
pro finální modely perk-tree a vlastních zbraní; bez něj fungují vanilla fallbacky.

### Změny v 2.3.3

- Hlavní úroveň používá hráčskou hlavu v dovednostech a milnících.
- V hlavním menu už není Činnosti; Můj kůň se zobrazí až po Power milníku 25.
- Tlačítka zpět v GUI používají jednotný model perk-tree.
- Gathering má `0,20 %` přirozeného double dropu za level, maximum `20 %`; perky
  přidávají jeden `+5 %` double node pro každou větev a čtyři `+3 %` triple nodes.
- New Game+ používá `75 %` XP, `+10 %` perk statistik a `×1,25` přirozeného
  double dropu za rank.

### Dřívější změny v 2.3.2

- `Umění dlaně` a `Obchodování` jsou interně zachované, ale úplně neaktivní a
  skryté z menu, navigace i administrativního výběru.
- Power počítá pouze 13 aktivních dovedností.
- Každá perk-tree mapa má unikátní tematickou siluetu.
- New Game+ je vedle startovního perku a používá `TRIAL_KEY`.
- Graf používá celou viditelnou plochu mimo osm navigačních šipek; perk ani cesta
  se pod šipkou nevykreslují.
- Tooltipy perků jsou zkrácené na účinek, stav, hodnost, cenu a skutečné podmínky.

## Release a nasazení

1. Sestav `NekaraRPG/dist/NekaraRPG.jar` skriptem `scripts\build-release.cmd`.
2. GitHub release musí mít tag `v2.3.3`, jeden asset `NekaraRPG.jar` a hash
   staženého assetu musí odpovídat lokálnímu JARu.
3. Pro FTP nasazení nejdřív zastav server. Použij
   `C:\Users\jonac\Documents\Nekara\FTP\deploy-nekararpg-safe.ps1` s parametrem
   `-Artifact`; skript vytváří lokální i vzdálenou zálohu a ověřuje finální hash.
4. Po výměně JARu je nutný úplný restart serveru.

Server v této chvíli nemusí obsahovat přesně stejný JAR jako GitHub release;
nasazení a release jsou samostatné kroky a vždy se ověřují hashem.

## Další doporučený krok

Živě projít hlavní menu, perk-tree GUI a mount milestone ve všech 13 aktivních
dovednostech, zejména zobrazení šipek, zelených cest, New Game+, návratových tlačítek
a čitelnost tooltipů s aktuálním resource packem.
