# Testování NekaraRPG

Spusť úplnou sadu:

```powershell
.\gradlew.bat test --console=plain
.\scripts\build-release.cmd
```

Před nasazením ověř také `git diff --check` a SHA-256 artefaktu v `dist/NekaraRPG.jar`.

## Povinná živá kontrola

- Nativní Skills XP za sběr, činnosti, boj, rybaření a crafting bez duplicit.
- Rested násobí běžné Skills akce přes `campfire.rested.skills-experience`, ne admin operace.
- Rybaření používá nativní úroveň Rybaření, zachová serverem vytvořený vanilla úlovek a po úspěchu udělí jednu XP odměnu.
- Echo Vein běží bez externího pluginu, pouze pro běžnou těžbu, a bonus zapisuje do Těžby.
- Při reloadu, vypnutí modulu a odpojení hráče nezůstávají listenery, tasky, bossbary ani dočasné entity.
