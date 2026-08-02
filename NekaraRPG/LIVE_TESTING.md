# Živé testování NekaraRPG

## Doporučené prostředí

Použij samostatný staging server Purpur 26.1.2 s Javou 25. Svět i port odděl od
produkce. Testuj ve dvou průchodech:

1. Pouze Purpur a NekaraRPG, aby se izolovalo chování pluginu.
2. Stejný server s CMI a ValhallaMMO pro ověření kompatibility.

Sestavení a nasazení z adresáře `NekaraRPG`:

```powershell
scripts\build-release.cmd
scripts\deploy-test.cmd -ServerPath D:\path\to\test-server
```

Před výměnou server zastav a potom ho znovu spusť. Bukkit `/reload` nepoužívej k
nahrazení JARu. `/nekararpg reload` slouží pouze k načtení konfigurace a zpráv.
Deploy skript odmítne pokračovat, pokud vedle stabilního `NekaraRPG.jar` najde
další `NekaraRPG*.jar` nebo starý `NekaraFishing*.jar`; nahlášený soubor nejdřív
přesuň mimo `plugins`.

## Profil pro rychlé testování

Pro krátký vývojový cyklus dočasně použij na staging serveru:

```yml
campfire:
  update-period-ticks: 10
  healing:
    amount: 2.0
    period-seconds: 1
  hunger:
    restore-amount: 1
    restore-period-seconds: 2
  rested:
    charge-seconds: 5
    duration-seconds: 20
  camping:
    duration-per-feature-seconds: 10
    spawn-protection:
      radius: 10.0
```

Po změně spusť `/nekararpg reload`. Před akceptací vrať produkční defaults:
20sekundové nabíjení, základ pět minut, jednu minutu za unikátní prvek, bezpečný
radius 24 bloků, jeden bod zdraví za pět sekund a jeden bod jídla za deset sekund.

## Akceptace sezení

1. Spusť `/nekararpg sit` na plných blocích, slabech, schodech a nerovném terénu.
2. Ověř dosednutí modelu na povrch s offsetem `0.20` bez průniku či levitování a odstranění sedadla přes `/nekararpg stand`.
3. Sedni znovu a použij běžnou klávesu sesednutí; neviditelné sedadlo musí zmizet.
4. Teleport, smrt, odpojení, poškození, reload a shutdown nesmí zanechat armor stand.
5. S CMI ověř nezměněný hlavní `/sit`. NekaraRPG registruje jen `/nekararpg sit` a `/nrpg sit`.
6. Použij `/cmi sit` nebo jeho alias u ohně a ověř spuštění Campfire bez příkazu NekaraRPG.
7. Vypni `modules.sitting.enabled`, reloaduj a opakuj CMI test. Externí sezení musí Campfire dál pohánět.

## Akceptace Campfire

1. Zraň hráče, sniž hlad a sedni do výchozího kulového radiusu pěti bloků od zapáleného ohně.
2. Ověř léčení v intervalu, neklesající hlad, pomalé doplňování a částice aktivního odpočinku.
3. Uhas nebo rozbij oheň a ověř konec odpočinku při další aktualizaci.
4. Opakuj se zapáleným soul campfire a ověř stejné chování.
5. Sedni mimo radius a ověř, že se efekty nespustí.
6. U holého ohně dokonči skutečné nabíjení. Ověř jednorázovou Rested zprávu, jemný chime a text `Odpočatý | m:ss`, ale žádný Haste ani bossbar. Nesmí se zobrazit surový klíč zprávy.
7. Odejdi od holého ohně a vyvolej pokles hladu; i s Rested musí klesat běžnou rychlostí.
8. Přidej smoker do pěti bloků, znovu nabij Rested a po odchodu ověř nastavený násobitel hladu.
9. Přidej crafting table, znovu nabij a ověř minutu navíc, Haste I a potion ikonu.
10. Přidávej ostatní prvky a ověř minutu za unikátní typ bez efektu duplicit.
11. Pro rychlou expiraci zkrať doby; časovač, snížení hladu a řízený Haste musí skončit.
12. Před odpočinkem dej hráči silnější Haste a ověř, že ho NekaraRPG nenahradí ani neodstraní.

## Akceptace bezpečného tábora

1. Polož bed do pěti bloků od zapáleného ohně a pohybuj se kolem výchozího radiusu 24 bloků.
2. Nech v noci probíhat přirozené spawny. Vanilla nepřátelé nesmí vzniknout uvnitř, ale mohou vejít zvenčí.
3. Nech chunky odnačíst, později se vrať a ověř obnovenou ochranu beze změny tábora.
4. Uhas oheň nebo přesuň bed mimo radius prvků a ověř návrat přirozených spawnů.
5. S MythicMobs ověř blokování přirozených `NekaraHostile` a pokračující `NekaraFauna`.
6. Vyvolej command, summon, quest nebo boss spawn uvnitř a ověř, že se nezruší.

## Akceptace skupiny a kompatibility

1. Posaď dva hráče u stejného ohně a ověř dva hráče a násobitel `1.15x` v action baru.
2. Porovnej léčení za stejný interval s jedním a dvěma hráči.
3. Přesuň jednoho k jinému ohni; obě skupiny se musí vrátit na `1.00x`.
4. `/nekararpg status` musí hlásit správné počty seated, resting a Rested.
5. S ValhallaMMO porovnej běžná a Rested XP více skillů; běžná skill-action a sdílená XP mají být přesně `1.10x`.
6. Dokonči rybaření s Rested. Původní loot zůstane a odložená XP dostanou 10 % právě jednou.
7. Uděl ValhallaMMO XP administrativním příkazem a ověř, že se nenásobí.
8. Vypínej moduly jednotlivě. S vypnutým Sitting má Campfire přijmout externí sedadlo, ale ne `/nrpg sit`.
9. Spusť rybářskou minihru při Rested a ověř, že její UI dočasně nahradí Rested časovač.

Budoucí výpočet kvality tábora může vycházet ze stávajícího skupinového klíče
pro každý oheň a přidat okolní struktury bez změny smlouvy sezení.

## Akceptace Echo Vein

1. Použij `/nekararpg test vein` v různých jeskyních. Cíl musí být viditelný, dosažitelný, jen pro testujícího hráče a omezený na stone, deepslate, netherrack nebo end stone.
2. Ověř jemný pulz v okolí jednoho bloku a výrazně hustší viditelnou stěnu. Ores, dirt, gravel ani wood nesmí být cílem či spouštěčem.
3. Nastav trigger na `1.0`, vytěž hostitelský blok s novým i zkušeným profilem a ověř start bez level gate až po původních odměnách.
4. Přirozený chat zůstává prázdný, hlubší chime objevení zazní jednou a objeví se časovač.
5. Vytěž jiné bloky; cíl i časovač zůstávají. Vytěž cíl a bez chainu ověř jeden zvuk úspěchu.
6. Zapni `plugin.debug`. Porovnej `markedBlockXp`, `bonusXp` a profil: bonus je jednou, 25 % finální hodnoty cíle a důvod `PLUGIN`.
7. Bonusový item je přesně jeden item z finálního přirozeného nebo Valhalla-prepared výstupu cíle včetně metadat, bez nového Fortune hodu. Testovací příkaz nedává nic.
8. Vynuť ore reveal `1.0` a testuj Y 70, Y 15, Y -32 a Y -64. Každý výsledek musí patřit do vanilla-kompatibilního pásma; vysoko nesmí diamond ani redstone.
9. Porovnej běžný biom a Badlands na Y 70. Testuj netherrack těsně uvnitř a vně Y 10-117. End stone se nemění.
10. Úspěšná přeměna přehraje jednou jasnější ametystový zvuk, zřetelně jiný než objevení žíly. Opakované poškození ho neopakuje.
11. Vynuť chain `1.0`, vytěž cíl vedle viditelného hostitele a ověř sousední pokračování s vlastním časovačem a odměnami. Poté vrať `0.50`.
12. Nech pokus vypršet a ověř tichý timeout bez chatu.
13. Testuj Rested, Silk Touch, plný inventář, odpojení, smrt, teleport a prioritu rybaření.
14. Ověř nový `modules.mining.enabled` i fallback starého `modules.echo-vein.enabled`.
15. Vrať defaults (`0.05`, bez cooldownu, `0.50` chain, `0.25` ore reveal) a ověř nezávislé po sobě jdoucí bloky.
16. Vypni ValhallaMMO nebo Mining skill. NekaraRPG musí bezpečně nastartovat, automatická Echo Vein zůstane nedostupná.

## Akceptace updateru

Použij jednorázový staging release novější než instalovaná verze. Jeho jediný
asset musí být stabilní `NekaraRPG.jar` vytvořený `build-release.cmd`.

1. Spusť `/nekararpg update check`; příkaz odpoví ihned a download pokračuje asynchronně.
2. Konzole ohlásí připravenou verzi a operátor s `nekararpg.update.notify` dostane české upozornění na restart.
3. Stažený soubor musí být v update složce Paperu, ne vedle aktivního JARu.
4. Spusť `/nekararpg update status`, znovu připoj operátora a ověř stejnou čekající verzi.
5. Server běžně zastav a spusť. Staging soubor se spotřebuje, zůstane jediný aktivní `NekaraRPG.jar` a status hlásí novou verzi.
6. Zkontroluj startup logy a před produkcí zopakuj smoke testy Fishing, Sitting, Campfire, Echo Vein, ValhallaMMO a MythicMobs.

Chybové režimy updateru nikdy netestuj na produkci. Pro špatné digesty, identity,
velikosti a verze použij staging release nebo dočasnou repository fixture.
