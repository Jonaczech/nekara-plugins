# Nekara Plugins

Repozitář obsahuje serverové pluginy pro ekosystém Nekara. Hlavním artefaktem je `NekaraRPG` — modulární RPG plugin pro Purpur/Paper 26.1 a Java 25.

## Aktuální release

**NekaraRPG 2.8.0** rozvíjí výbavu, bojovou odezvu a bezpečný pohyb draka.

- Drak při letu ověřuje trasu, bloky i otevřený prostor nad terénem; nemůže projít zdí, stropem ani letět pod povrchem.
- Kritické zásahy, krvácení a silové útoky mají stručnou vizuální a zvukovou odezvu.
- Chainmail se vyrábí ve smithing table z koženého kusu a železného ingotu. Vyžaduje Řemeslo 20 a patří do lehké větve výstroje.
- Kovová výbava se dokončuje skutečným tavením ve vysoké peci, naklepáním na kovadlině, ochlazením ve vodním kotli a u zbraní také broušením.
- `Bleskové reflexy` dávají aktivní lehké sadě +5 % rychlosti pohybu. `Vynalézavý tulák` aktivuje setové bonusy už se třemi lehkými kusy.
- `Dračí pouto` se odemyká na Power 100; ve světě je aktivní vždy právě jeden companion — kůň nebo drak.

GitHub release a živé nasazení na server jsou samostatné kroky. Release asset je vždy ověřen staženým SHA-256; nasazení vyžaduje zastavený server a plný restart.

## Resource pack

NekaraRPG používá namespaced modely a textury z odděleného repozitáře [`Jonaczech/nekara-resourcepack`](https://github.com/Jonaczech/nekara-resourcepack). Bez packu zůstává plugin hratelný díky vanilla fallbackům; pro finální vzhled GUI a vlastních předmětů je pack nutný.

## Build

```text
cd NekaraRPG
scripts\build-release.cmd
```

Artefakt vznikne jako `NekaraRPG/dist/NekaraRPG.jar`. Skript vždy spustí testy a ověří manifest i SHA-256.

## Dokumentace

- [Handoff](HANDOFF.md) – aktuální stav, release a provozní postup.
- [Project memory](PROJECT_MEMORY.md) – závazná architektonická rozhodnutí.
- [Roadmap](ROADMAP.md) – ověření a další schválené kroky.
- [NekaraRPG README](NekaraRPG/README.md) – instalace, moduly a herní chování.
- [Live testing](NekaraRPG/LIVE_TESTING.md) – povinná živá akceptace před nasazením.