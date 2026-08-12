# Nekara Plugins

Repozitář obsahuje serverové pluginy pro ekosystém Nekara. Hlavním artefaktem je `NekaraRPG` — modulární RPG plugin pro Purpur/Paper 26.1 a Java 25.

## Aktuální release

**NekaraRPG 2.9.0** přidává GUI tvorbu custom itemů, plnohodnotné Runotepectví, runové sockety a bezpečnější zpracování výbavy.

- Prázdná, Magická a Nestabilní runa procházejí craftingem, kovadlinou, enchanting table a lecternem. První Runa poznání přidává `+1 / +3 / +5 %` XP.
- Runotepecký perk-tree má hodnosti, číselné bonusy k vanilla enchantování, samostatné ceny run a bonusy New Game+.
- Kvalita určuje 1–3 barevné sockety. Tooltipy místo technických dat ukazují dovednost, kvalitu, výkov a efekty run.
- Administrátor vytváří custom itemy v GUI; stabilní ID a model lze navázat na oddělený resource pack.
- Bone meal rozšiřuje květiny na trávě a pouštní vegetaci na písku bez cactusu.
- Výkov vyžaduje kladivo odpovídajícího tieru. Chainmail a kůže čekají na Fletching Table.

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