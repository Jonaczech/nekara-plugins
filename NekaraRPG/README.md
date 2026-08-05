# NekaraRPG

Modulární RPG plugin pro Purpur/Paper 26.1 a Java 25. Obsahuje autentizaci,
rybářskou minihru, táboření/Rested, Echo Vein, mounty a nativní Nekara Skills.

## 2.3.2 – perk-tree UX

- 13 aktivních dovedností a odvozená Hlavní úroveň.
- `Umění dlaně` a `Obchodování` jsou interně zachované, ale pro hráče neaktivní.
- Každá dovednost má vlastní tematickou perk-tree siluetu, kompaktní cesty a
  New Game+ s ikonou Trial Chamber klíče vedle startovního perku.
- Šipky jsou samostatné navigační sloty a perk ani cesta se pod nimi nevykreslí.
- Tooltipy ukazují pouze relevantní účinek a konkrétní postup odemčení.

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
