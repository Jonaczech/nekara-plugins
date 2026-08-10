# Požadavky výroby výbavy

Tento přehled popisuje aktuálně vynucované požadavky NekaraRPG. Kromě níže
popsaného chainmail upgradu nemění vanilla recepty ani suroviny.

## Řemeslo podle materiálu

| Materiál | Požadovaný level Řemesla | Platí pro |
| --- | ---: | --- |
| Dřevo, kůže | 0 | zbraně, nástroje, zbroje |
| Kámen | 5 | zbraně a nástroje |
| Měď | 10 | zbraně, nástroje, zbroje |
| Zlato | 15 | zbraně, nástroje, zbroje |
| Železo, chainmail | 20 | zbraně, nástroje, zbroje |
| Diamant | 50 | zbraně, nástroje, zbroje |
| Netherite | 80 | zbraně, nástroje, zbroje |

Nekara kontroluje náhled i dokončení craftu, proto požadavek nelze obejít
shift-craftem nebo stonecutterem. Nezařazené vanilla předměty bez materiálového
tieru (luk, kuše, trojzubec a palcát) nemají další levelový požadavek.

## Zbraně a nástroje

- Vanilla meče, sekery a kopí používají tabulku materiálů výše.
- Vlastní dýky, obouruční meče a kladiva používají stejnou tabulku ve všech
  sedmi materiálových variantách.
- Krumpáče, lopaty a motyky používají stejnou tabulku materiálů.
- Dýky a kladiva se vyrábějí z příslušného materiálu a klacků:

| Vlastní zbraň | Schéma |
| --- | --- |
| Dýka | ` A ` / ` S ` |
| Kladivo | ` A ` / `ASA` / ` S ` |

`A` je příslušný materiál tieru; `S` je klacek. Obouruční meč vzniká ve smithing table
z příslušného vanilla meče a jednoho materiálu tieru (např. `Iron Sword` + železný ingot).

## Zbroje a kvalita

- Helma, chestplate, leggings a boots jsou řízené materiálovou tabulkou.
- Chainmail se vyrábí ve smithing table bez šablony: odpovídající kožený kus + 1 železný ingot.
- Po vyrobení získá výbava Řemeslný tier podle aktuální úrovně Řemesla:
  I (0), II (20), III (40), IV (70), V (100).
- Kovové zbraně a zbroje dále procházejí dílenským zpracováním; neopracovaný
  kus nemá aktivní plný Nekara bonus. Výkov se nejdříve skutečně nataví ve vysoké
  peci, poté se držený v hlavní ruce naklepává plížením a pravým kliknutím na
  kovadlinu a nakonec ochladí ve vodním kotli. Zbraně se po ochlazení navíc
  dokončí plížením u brusného kamene; zbroj je hotová už po ochlazení.

## Používání podle dovednosti

Vyrobit předmět a umět jej používat jsou dvě samostatné podmínky. Materiálová
tabulka výše určuje požadovaný level pro použití v příslušné dovednosti.

| Výbava | Požadovaná dovednost |
| --- | --- |
| Kožená, chainmail a diamantová zbroj | Stínový oděv |
| Měděná, zlatá, železná a netheritová zbroj | Plátová ochrana |
| Meče, dýky a kopí | Lehké zbraně |
| Obouruční meče a kladiva | Brutální boj |
| Krumpáče | Těžba |
| Lopaty | Kopání |
| Motyky | Statkářství |
| Sekery při držení a kácení | Lesnictví |
| Sekery při útoku | Brutální boj |

Při nedostatečné úrovni zůstává předmět použitelný, ale hra zobrazí vysvětlení:

- zbroj: pohybový postih a až o 60 % slabší dodatečná ochrana při čtyřech
  neovládnutých kusech;
- zbraně a nástroje: postih pohybu, 60 % snížení poškození při útoku a 85 %
  snížení rychlosti ničení bloků u nástrojů.
