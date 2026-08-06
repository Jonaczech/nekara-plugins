# Přehled změn

## 2.5.0

- Lehké zbraně mají nový strom ve tvaru meče: rukojeť tvoří Čepel v pohybu,
  Rytmus souboje a Lehký krok tvoří záštitu, Hluboký řez a Vražedný úhel čepel
  a Tisíc řezů její hrot. Cesty používají vlastní směry ohybů, aby silueta
  zůstala čitelná i ve viewportu.
- Strom přidává rychlost útoku při držení lehké zbraně, samostatnou šanci
  krvácení po kritickém zásahu a malý plochý bonus poškození krvácením.
  Materiálové levelové požadavky pro používání zbraní zůstávají beze změny.
- Jed na ostří už není perk lehkých ani těžkých zbraní. Odemčení nanášení
  lektvarů na libovolnou podporovanou zbraň nyní vlastní alchymistický perk
  Bojové esence, který nadále zachovává i slučování lektvarů.
- Těžké zbraně mají nový asymetrický strom ve tvaru sekyry. Plně nabitý útok
  je nyní základ společného „brutálního“ stylu: Drtivý nápřah a Otřes země jej
  zesilují, Prasklá obrana a Drtivý průlom přidávají průraznost a Široký rozmach
  odemyká odlišný plošný účinek pro sekeru, obouruční meč a kladivo.
- Kopí má při prázdné vedlejší ruce skutečný bonus dosahu +1,25 bloku. Vlastní
  dýky dostaly vyšší základní rychlost útoku a kratší dosah -0,5 bloku.
  Materiálové levelové požadavky všech zbraní zůstávají beze změny.
- Stínový oděv nyní nabízí Železné cvoky, Nespoutaný krok, Řízený metabolismus,
  Vynalézavého tuláka, Adrenalin a Bleskové reflexy. Dokončený strom poskytuje
  +30% účinnost lehké zbroje, +30% úsporu hladu, +20% šanci úhybu a odstraní její
  pohybovou zátěž. Po Vynalézavém tulákovi fungují setové bonusy už se třemi
  čistě lehkými kusy zbroje.
- Adrenalin při ≤25% zdraví udělí na 5 s Rychlost II a Regeneraci I; jeho cooldown
  je 60 s. Požadavky materiálů pro chainmail a diamantovou lehkou zbroj zůstávají.
- Plátová ochrana má nový strom se šesti perky: Zpevněná výstroj, Nohy z oceli,
  Vitální ocel, Hněv, Vynalézavý pěšák a Ostnatý Juggernaut. Hněv má cooldown 60 s;
  Juggernaut vyžaduje kompletní těžkou sadu a poskytuje odraz poškození, odolnost
  proti knockbacku a imunitu vůči Slowness, Weakness a Levitation.
- Rybaření získalo vlastní doplňkovou loot tabulku, která nikdy nenahrazuje vanilla
  úlovek. Obsahuje prismarine crystals, slime ball, echo shard, armor trimy,
  netherite upgrade a velmi vzácný nether star. Globální Štěstí se promítá do
  šance na poklad i Potopenou schránku.
- Potopená schránka se vytváří vedle rybáře na 90 s, je trvale označená vlastníkem
  a otevře či rozbije ji pouze on; hoppery do ní ani z ní nepřenášejí předměty.
  Naladění vody ukládá až 10 stacků po 2% šance na poklad a zobrazuje je výhradně
  částicemi vody v místě háčku.
- New Game+ Rybaření násobí přirozenou šanci na dvojitý úlovek `×1,25` za rank.
- Cesty mezi perky používají nejvýše 3 mezilehlé sloty. Všechny aktivní stromy mají
  navzájem odlišnou tematickou siluetu a viditelné rohové spoje cest.
- Přidány příkazy `/nekararpg skills admin max <hráč>` pro úroveň 100, všechny perky
  a New Game+ a `/nekararpg skills admin reset-all <hráč>` pro úplné vynulování.

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
