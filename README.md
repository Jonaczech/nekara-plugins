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
| `NekaraRPG` | aktivní | Centrální modulární plugin s NekaraAuth, rybařením, sezením, odpočinkem u ohně, NekaraMining a NekaraMounts. |

## Aktuální směr

`NekaraRPG` se má stát centrálním pluginem pro propojené RPG a imerzivní
systémy Nekary. Každá herní oblast je samostatný modul, který lze zapnout nebo
vypnout v konfiguraci. Release 1.5.1 obsahuje moduly `auth`, `fishing`, `sitting`,
`campfire`, `mining` a `mounts`; Campfire používá Sitting jako základ stavu hráče.

NekaraMounts je vydaný ve verzi 1.5.1; rozsah a zbývající živá akceptace jsou v
`ROADMAP.md` a `NekaraRPG/LIVE_TESTING.md`.
Možné pozdější moduly:

- `lockpicking`
- `wounds`
- `world-events`
- `rumors`
- `territory`
- `reputation`

Plugin nemá duplikovat systémy, které už dobře řeší ValhallaMMO. Integrace s
ValhallaMMO musí zůstat volitelná a zachovat vanilla i ValhallaMMO odměny místo
jejich nahrazování.

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
