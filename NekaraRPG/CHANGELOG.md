# Přehled změn

## 2.4.2

- Kvalita vlastnoručně vyrobené výbavy používá pět jasně odlišených pečetí:
  `◇ Běžná`, `✦ Neobyčejná`, `◆ Vzácná`, `✹ Epická` a `✪ Legendární`.
  Epická a Legendární kvalita je navíc tučná; změna nemění hodnoty bonusů,
  hranice úrovní ani uložená data předmětů.
- Odemknutí materiálu výbavy a její kvalita jsou oddělené: úroveň Řemesla
  zpřístupňuje materiály, zatímco lepší kvality odemykají výhradně perky.
  Poctivé řemeslo navíc odemyká dokončení výkovu; kvalita se odhalí až po
  peci, kotlíku a u zbraně také po brusném kameni. Tooltipy pravděpodobností
  nyní používají znak `%`.
- Dokončený výkov má barvu, zvuk a částice podle získané kvality. Ikona právě
  otevřené dovednosti v perk stromu stručně ukazuje její aktivní perky i jejich
  skutečné souhrnné hodnoty. U sběratelských dovedností ukazuje celkový pasivní
  dvojitý výtěžek; ostatní procentní i číselné bonusy přebírá z aktuálního stavu
  postavy, tedy včetně všech odemčených hodností.
- Po odemčení `Poctivého řemesla` je vyrobená výbava vždy alespoň Neobyčejná.
  Vzácná, Epická a Legendární kvalita zůstávají náhodným vylepšením podle
  odemčených perků a hodnoty kvality výrobků.
- Váhy vyšších kvalit jsou výrazně štědřejší pro plně rozvinuté Řemeslo:
  bez Nové hry+ má kompletní strom přibližně `8 %` na Legendární kvalitu,
  s Novou hrou+ téměř `10 %`. Bonus Nové hry+ používá stávající násobení
  statistik perků a ovlivňuje proto přímo každý roll kvality.
- Perk Nová hra+ má ve všech stromech výrazný tooltip s ikonou Trial Key,
  názvem, stavem znovuzrození a krátkým přehledem trvalé síly perků i XP
  následujícího běhu.
- Globální Štěstí nyní zvyšuje také šanci na povýšení kvality vlastnoručně
  vyrobené výbavy. Každý bod přidá výchozích `5 %`, nejvýše `10 %` při
  současném limitu dvou bodů; hodnota je upravitelná v `skills/config.yml`.
- Hlavní okno má titul `Dovednosti`. Řemeslo, Statkářství a Rybaření jsou
  přesunuty do spodní řady, aby přehled dovedností měl čistší rozložení.

## 2.4.1

- Hlavní menu používá pro Dovednosti knihu s brkem ve středovém slotu.
- Přehled dovedností má kratší tooltipy: výrazný název, úroveň, stávající progress bar,
  zbývající XP a odkaz na perky; na maximu ukazuje pouze `100/100 • MAX`.
- Tooltip perků zobrazuje jeden řádek s hodnotou právě kupované hodnosti.
- Poklady Kopání dostaly rozšířené tematické tabulky pro písek, štěrk, zeminu, clay a mud.
  Apple, Egg, Soul Sand, Cake a Stick do nich nejsou zařazeny.
- Těžba, Lesnictví a Kopání už pro XP, pasivní dropy a poklady nevyžadují správný nástroj.
  Aktivní Vein Mining a Tree Feller jej nadále vyžadují. Pasivní dropy Statkářství
  nevyžadují motyku, ale aktivní sklizeň a Field Harvest ano.

## 2.4.0

- Přepracované stromy Kopání a Těžby, rankové levelové brány a New Game+ bonusy.
- Kopání používá globální i blokově specifické pokladové tabulky.
- Echo Vein získává až `+5 p. b.` šance z úrovně Těžby.
- Živá půda zrychluje přípravu jídla bez ovlivnění rud.

## 2.3.3

- Hlavní úroveň používá v přehledu dovedností i v milnících ikonu hráčské hlavy.
- Hlavní menu již neobsahuje nadbytečnou položku `Činnosti`; sezení a ležení zůstávají
  dostupné přímo z něj.
- `Můj kůň` se zobrazí až po automatickém odemknutí milníku Věrný společník na
  Hlavní úrovni 25.
- Všechna tlačítka návratu používají stejný model `skills/tree/button_return_to_menu`
  jako perk-tree.
- Pět sběratelských dovedností má přirozený double drop `0,20 %` za úroveň až do
  `20 %` na úrovni 100; New Game+ zpomaluje XP na `75 %`, posiluje perk statistiky
  o `10 %` za rank a přirozený double drop násobí `1,25×` za rank.

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
