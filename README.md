# Nekara Plugins

Repozitář obsahuje serverové pluginy pro ekosystém Nekara. Aktuálně je hlavním
artefaktem `NekaraRPG` – modulární plugin pro Purpur/Paper 26.1.

## Aktuální release

**NekaraRPG 2.4.1** přináší nativní RPG postup, perk-tree GUI, vlastní zbraně,
integrované XP zdroje, gathering odměny a přepracované dovednostní rozhraní.

- 13 aktivních hráčských dovedností a odvozená Hlavní úroveň.
- `Umění dlaně` a `Obchodování` zůstávají v interní databázové kompatibilitě,
  ale nejsou viditelné, neudělují XP, nemají aktivní efekty a neovlivňují Power.
- Každá dovednost má vlastní kompaktní perk-tree siluetu, zelené odemčené cesty
  a New Game+ vedle počátečního perku.
- Tooltipy perků i dovedností jsou kompaktní a ukazují jen aktuálně podstatné údaje.
- Dovednosti jsou v hlavním menu uprostřed jako kniha s brkem.
- Hlavní úroveň používá ikonu hráčské hlavy; automatické milníky odemykají Rested
  na Power 1 a Mého koně na Power 25.
- Hlavní menu bez Činností a všechna návratová tlačítka sdílejí vizuální jazyk perk-tree.
- Gathering má `0,20 %` přirozeného double dropu za level do maxima `20 %`;
  New Game+ zpomaluje XP a přitom zvyšuje získané pasivní bonusy.
- Kopání používá globální i blokově specifické tabulky pokladů; Těžba, Lesnictví a
  Kopání nevyžadují pro běžný postup správný nástroj. Aktivní gathering schopnosti
  jej vyžadují dál a motyka zůstává podmínkou aktivní sklizně Statkářství.

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
