# Pokyny pro repozitář

Tyto pokyny platí pro celý repozitář `nekara-plugins`.

## Začni zde

Před změnou kódu nebo návrhem nového pluginu přečti:

1. `HANDOFF.md` pro aktuální release a bezprostřední další kroky.
2. `PROJECT_MEMORY.md` pro dlouhodobá produktová a kompatibilitní rozhodnutí.
3. `README.md`, `CHANGELOG.md`, `DEVELOPMENT.md` a `TESTING.md` cílového pluginu,
   pokud existují.

Pokud záleží na aktuálním stavu, znovu ověř Git, GitHub, výstup buildu a server;
snapshot v handoffu může zastarat.

## Komunikace

- S uživatelem komunikuj česky, pokud nepožádá o jiný jazyk.
- Hráčské Minecraft zprávy udržuj v češtině.
- Navržený samostatný plugin neměň bez upozornění na modul NekaraRPG ani naopak.
  Nejdřív stanov hranici vlastnictví.

## Vývoj

- Zachovej existující integrace pluginu a serveru, pokud změna není výslovná.
- Moduly nech samostatně konfigurovatelné a při vypnutí nebo reloadu vyčisti
  listenery, tasky, entity, UI a hráčský stav.
- Preferuj typovanou konfiguraci, zabalené výchozí hodnoty a zaměřené testy
  čistého časování, škálování, deduplikace a stavové logiky.
- Necommituj výstup buildu, serverové soubory, přihlašovací údaje, Gradle cache,
  lokální truststore ani nic pod `dist/`.
- Nikdy nezveřejňuj přihlašovací údaje z lokální serverové nebo FTP konfigurace.

## Vydávání NekaraRPG

- Interně, v nadpisech changelogu a Git tazích používej sémantické verze.
- Jediný nasazovaný název je `NekaraRPG.jar`; verzi do názvu nikdy nepřidávej.
- Před publikací spusť `NekaraRPG\scripts\build-release.cmd`.
- V serverovém `plugins` ponech právě jeden NekaraRPG JAR, nahrazuj ho jen při
  vypnutém serveru a místo Bukkit reloadu proveď restart.
- Release publikuj přes zkontrolovanou větev/PR, sluč do `main` a potom vytvoř
  GitHub release, jehož jediným pluginovým assetem je `NekaraRPG.jar`.

## Nekara Skills 2.x

- Před změnou skillů přečti `NekaraRPG/docs/adr/0001-native-skills-platform.md`
  a `NekaraRPG/docs/SKILLS_2_0_ROADMAP.md`.
- Zachovej clean-room hranici: lze implementovat veřejně popsanou herní funkci,
  ale nekopíruj cizí zdrojový kód, konfiguraci, texty, názvy, hodnoty ani layout.
- České názvy dovedností bere celé UI z `SkillPresentation`; Trading se zobrazuje
  jako `Obchodování`.
- `DefaultPerkTree` je původní katalog 90 uzlů. Změna uzlu musí zachovat stabilní
  ID nebo dodat explicitní migraci uložených `perk_ranks`.
- Perk nákup musí zůstat autoritativní na serveru a atomický přes revision
  profilu. Nikdy neodečítej body jen podle stavu otevřeného GUI.
- Dokud nejsou zapojené a živě ověřené XP zdroje i herní efekty všech aktivních
  perků, nech `modules.skills.enabled` výchozím stavem vypnutý a ValhallaMMO
  ponech jako produkční autoritu.
