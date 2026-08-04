# Nekara Plugins

Tento repozitář obsahuje serverové Minecraft pluginy pro ekosystém Nekara.

## Návaznost projektu

- [`HANDOFF.md`](HANDOFF.md) zachycuje aktuální vydanou verzi, provozní stav a
  bezprostřední další kroky.
- [`PROJECT_MEMORY.md`](PROJECT_MEMORY.md) uchovává dlouhodobá produktová,
  kompatibilitní a release rozhodnutí.
- [`ROADMAP.md`](ROADMAP.md) určuje schválenou prioritu další práce a rozsah
  kandidáta NekaraMounts.
- [`AGENTS.md`](AGENTS.md) říká Codexu na každém zařízení, že má před změnami
  repozitáře tento kontext přečíst a zachovat.

## Projekty

| Plugin | Stav | Popis |
| --- | --- | --- |
| `NekaraRPG` | aktivní | Centrální modulární plugin s NekaraAuth, činnostmi, Tábořením, NekaraMining, NekaraMounts a vývojovou platformou Nekara Skills. |

## Aktuální směr

`NekaraRPG` je centrální plugin pro propojené RPG a imerzivní systémy Nekary.
Každá herní oblast zůstává samostatně zapínatelným modulem uvnitř jednoho JARu.
Nejnovější stabilní release je 2.1.0. Přidává 16 českých dovedností, původní
katalog 90 perků, detailní GUI, transakční nákup za body hlavní úrovně, runtime
vertikály všech 15 trénovaných skillů, eventové perk efekty, omezenou XP frontu a
rozdělené konfigurace jednotlivých dovedností. Oprava ležení používá nativní
Paper/Purpur mannequin namísto ručně sestavovaných packetových metadat.
Součástí releasu je také operátorská staging správa s transakčním auditem v
SQLite schématu v2.

Vývojová větev 2.2.0 navazuje live-readiness vrstvou: opravuje orientaci a
duplicitní vybavení mannequin ležení, přidává měřitelné metriky XP fronty a
konzistentní read-only export SQLite+CSV pro zálohu a budoucí mapování profilů.

Všech 15 skillů je připraveno pro postupnou živou akceptaci, ale nativní modul
zůstává výchozím stavem vypnutý. ValhallaMMO je produkční autoritou až do ověřené
migrace, měření MSPT a rollbacku bez dvojího udělování XP nebo odměn. Aktuální rozsah a přesný další krok
jsou v `HANDOFF.md`, `ROADMAP.md` a
`NekaraRPG/docs/SKILLS_2_0_ROADMAP.md`.

## Vydání NekaraRPG

```text
cd NekaraRPG
scripts\build-release.cmd
```

Release skript spustí všechny testy, ověří interní verzi a changelog a vytvoří
jeden stabilně pojmenovaný artefakt pro nasazení:

```text
NekaraRPG/dist/NekaraRPG.jar
```

Release pravidla jsou v `NekaraRPG/DEVELOPMENT.md`. Postup nasazení a živé
akceptační testy jsou v `NekaraRPG/LIVE_TESTING.md`.
