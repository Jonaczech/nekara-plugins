# Přehled změn

## 2.3.2

- Skryté a neaktivní `Umění dlaně` a `Obchodování` se nezobrazují v GUI,
  navigaci ani administrativním výběru; jejich XP a runtime efekty jsou vypnuté.
- Power počítá jen 13 aktivních dovedností.
- Každá perk-tree mapa má vlastní tematickou siluetu a New Game+ s `TRIAL_KEY`
  vedle počátečního perku.
- Viditelný graf vyplňuje všechny nenavigační sloty perk-tree GUI; šipky nemohou
  překrýt perk ani cestu.
- Tooltipy perků dostaly stručný účinek, ikony a konkrétní postup podmínek bez
  duplicitních popisů.

## 2.3.1

- NekaraRPG je plně samostatný: byly odstraněny všechny externí skill bridge,
  soft-dependency, konfigurační sekce a kompatibilní menu fallbacky.
- Echo Vein nyní používá nativní těžbu a svůj bonus připisuje do Těžby.
- Rybářská minihra se řídí nativní úrovní Rybaření a zachovává původní vanilla úlovek.
- Rested násobí standardní XP akce NekaraRPG Skills přes
  `campfire.rested.skills-experience`.
- Dovednosti mají české názvy a migrovaná ASCII interní ID: `tezba`, `kopani`,
  `rybareni`, `lesnictvi`, `statkarstvi`, `lehke_zbrane` a `runotepectvi`.
- Perk-tree používá kompaktní spojité cesty, navigační šipky s prioritou a zelené
  cesty pro odemčené perky.
- Přidán katalog vlastních zbraní, craftovací požadavky, bojové efekty a jejich
  napojení na nativní Skills perky.

## 2.3.0

- Stabilní základ nativního systému dovedností, perk-tree GUI a RPG výbavy.
