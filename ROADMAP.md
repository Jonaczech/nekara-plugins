# Roadmap Nekara Plugins

## Aktuální release

**NekaraRPG 2.9.0**: GUI custom itemy, plnohodnotné Runotepectví s první Runou poznání, sockety podle kvality, číselné vanilla enchanty, rozšířený bone meal a bezpečnější tierové naklepávání výbavy.

## Bezprostřední priorita

1. Živě projít tvorbu Runy poznání včetně cen, tierových bran, zvuků, částic, XP a vracení vstupů.
2. Ověřit sockety a kumulaci skill XP bonusu na všech kvalitách výbavy.
3. Ověřit výkov měděné, železné a diamantové zbroje se správným i příliš slabým kladivem; chainmail se nesmí tavit.
4. Ověřit bone meal na trávě, písku a rudém písku podle `NekaraRPG/LIVE_TESTING.md`.
5. Teprve po živé akceptaci nasadit release JAR na vypnutý produkční server a restartovat.

## Následné směry

- Navrhnout Vyztuženou koženou zbroj jako custom lehký mezitier mezi kůží a chainmailem; musí mít stabilní `nekararpg` identitu, resource-pack model a serverově vynucené požadavky.
- Navrhnout Ocelovou plátovou zbroj jako custom těžký tier 50 vedle diamantové lehké zbroje; nejdříve rozhodnout recept, atributy a roli zlata v těžké větvi.
- Rozšířit katalog run až po samostatném návrhu efektů a balancu; neměnit stabilní formát socketů ani Runu poznání bez migrace.
- Navrhnout zpracování kožené a chainmail zbroje přes Fletching Table.

## Mimo rozsah

- Nevracet neaktivní Obchodování a Umění dlaně bez nového návrhu jejich XP, perků, GUI a balancu.
- Nevytvářet paralelní externí skill systém.
- Nevydávat resource pack jako součást pluginového JARu; zůstává samostatným repozitářem a releasem.