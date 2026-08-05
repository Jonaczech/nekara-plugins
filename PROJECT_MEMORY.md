# Projektová paměť Nekara Plugins

## Architektura

- `NekaraRPG` je modulární Purpur/Paper plugin pro Minecraft 26.1 a Java 25.
- Nativní `skills` jsou jedinou autoritou RPG XP, perků a Power. Nevracej externí
  skill bridge ani paralelní XP pipeline.
- Rybaření musí zachovat serverem vytvořený vanilla úlovek a XP; minihra pouze
  řídí okamžik vrácení reálného catchu.
- Perk nákup je server-authoritative přes SQLite optimistic revision. GUI nikdy
  není autoritou pro rank, cenu, podmínky ani body.
- Custom itemy používají stabilní namespaced ID a statistiky řeší server.

## Perk-tree pravidla

- Aktivních je 13 dovedností. `MARTIAL_ARTS` a `TRADING` jsou zachované pouze
  kvůli profilu/datové kompatibilitě a nesmí se zobrazovat ani získávat XP.
- Každá aktivní dovednost má vlastní tematický layout; rozestupy cest jsou 2–4
  buněk a New Game+ je vedle startovního perku.
- Cesty: šedá zamčená, bílá dostupná, zelená odemčená.
- Osm navigačních šipek je mimo viditelný graf a vždy má klikací prioritu.
- Tooltip ukazuje u mechanik jediný konkrétní účinek; u statových perků doplňuje
  přesnou hodnotu krátkým popisem.

## Resource pack a release

- Textury a modely NekaraRPG patří do
  [`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack).
  Plugin obsahuje jen jejich ID a vanilla fallbacky.
- Build prováděj `NekaraRPG/scripts/build-release.cmd`.
- Před FTP výměnou vždy vyžádej potvrzení, že server neběží, ověř zálohu a hash
  finálně staženého souboru. GitHub release a živý server jsou nezávislé stavy.
