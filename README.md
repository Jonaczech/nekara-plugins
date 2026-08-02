# Nekara Plugins

Tento repozitář obsahuje serverové Minecraft pluginy pro ekosystém Nekara.

## Návaznost projektu

- [`HANDOFF.md`](HANDOFF.md) zachycuje aktuální vydanou verzi, provozní stav a
  bezprostřední další kroky.
- [`PROJECT_MEMORY.md`](PROJECT_MEMORY.md) uchovává dlouhodobá produktová,
  kompatibilitní a release rozhodnutí.
- [`ROADMAP.md`](ROADMAP.md) určuje schválenou prioritu další práce a rozsah
  připravovaného modulu NekaraMounts.
- [`AGENTS.md`](AGENTS.md) říká Codexu na každém zařízení, že má před změnami
  repozitáře tento kontext přečíst a zachovat.

## Projekty

| Plugin | Stav | Popis |
| --- | --- | --- |
| `NekaraRPG` | aktivní | Centrální modulární plugin s NekaraAuth, rybařením, sezením, odpočinkem u ohně a NekaraMining. |

## Aktuální směr

`NekaraRPG` se má stát centrálním pluginem pro propojené RPG a imerzivní
systémy Nekary. Každá herní oblast je samostatný modul, který lze zapnout nebo
vypnout v konfiguraci. Aktuální vydání obsahuje moduly `auth`, `fishing`,
`sitting`, `campfire` a `mining`; Campfire používá Sitting jako základ stavu hráče.

Schválenou nejbližší prioritou je `mounts`; konkrétní MVP je v `ROADMAP.md`.
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
