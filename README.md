# Nekara Plugins

Repozitář obsahuje serverové pluginy pro ekosystém Nekara. Aktuálně je hlavním
artefaktem `NekaraRPG` – modulární plugin pro Purpur/Paper 26.1.

## Aktuální release

**NekaraRPG 2.3.2** přináší nativní RPG postup, perk-tree GUI, vlastní zbraně,
integrované XP zdroje a přepracované dovednostní rozhraní.

- 13 aktivních hráčských dovedností a odvozená Hlavní úroveň.
- `Umění dlaně` a `Obchodování` zůstávají v interní databázové kompatibilitě,
  ale nejsou viditelné, neudělují XP, nemají aktivní efekty a neovlivňují Power.
- Každá dovednost má vlastní kompaktní perk-tree siluetu, zelené odemčené cesty
  a New Game+ vedle počátečního perku.
- Tooltipy perků ukazují jen relevantní účinek, stav, rank, cenu a skutečný
  postup podmínek.

## Resource pack

NekaraRPG používá namespaced modely a textury z odděleného repozitáře
[`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack).
Bez packu zůstává plugin hratelný díky vanilla fallbackům; pro finální vzhled GUI,
cest a vlastních zbraní je pack nutný.

## Build

```text
cd NekaraRPG
scripts\build-release.cmd
```

Artefakt vznikne jako `NekaraRPG/dist/NekaraRPG.jar`. Skript vždy spustí testy.

## Dokumentace

- [Handoff](HANDOFF.md) – aktuální stav, release a provozní postup.
- [Project memory](PROJECT_MEMORY.md) – závazná architektonická rozhodnutí.
- [Roadmap](ROADMAP.md) – další schválené kroky.
- [NekaraRPG README](NekaraRPG/README.md) – instalace, moduly a herní chování.
