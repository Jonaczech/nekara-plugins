# Roadmap Nekara Plugins

## Aktuální release

**NekaraRPG 2.8.0**: bezpečný serverový let draka, bojová vizuální odezva, chainmailový upgrade, rozšířený pracovní postup kovové výbavy a rychlostní bonus lehké sady.

## Bezprostřední priorita

1. Živě ověřit draka u zdí, stropů, jeskyní a pod terénem podle `NekaraRPG/LIVE_TESTING.md`.
2. Ověřit celý kovářský průchod pro chainmail, železnou zbroj a jednu zbraň: vysoká pec, kovadlina, vodní kotlík a brusný kámen.
3. Ověřit +5 % rychlosti `Bleskových reflexů` se čtyřmi kusy lehké zbroje i se třemi kusy po `Vynalézavém tulákovi`.
4. Teprve po živé akceptaci nasadit přesný release JAR na vypnutý produkční server a provést plný restart.

## Následné směry

- Navrhnout Vyztuženou koženou zbroj jako custom lehký mezitier mezi kůží a chainmailem; musí mít stabilní `nekararpg` identitu, resource-pack model a serverově vynucené požadavky.
- Navrhnout Ocelovou plátovou zbroj jako custom těžký tier 50 vedle diamantové lehké zbroje; nejdříve rozhodnout recept, atributy a roli zlata v těžké větvi.
- Dopracovat Runotepectví a návrh vlastních run bez nahrazení vanilla enchantingu.

## Mimo rozsah

- Nevracet neaktivní Obchodování a Umění dlaně bez nového návrhu jejich XP, perků, GUI a balancu.
- Nevytvářet paralelní externí skill systém.
- Nevydávat resource pack jako součást pluginového JARu; zůstává samostatným repozitářem a releasem.