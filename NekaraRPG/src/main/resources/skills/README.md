# Skill resource layout

Every trainable skill owns a folder with its runtime configuration and player-facing messages.
Gathering skills may also own a `loot-tables.yml`. Shared progression, storage and queue controls
remain in `skills/config.yml`.

At startup, legacy values from the former monolithic `skills/config.yml` are migrated into these
folders without overwriting an existing per-skill file.

## Přirozený výtěžek a New Game+

`skills/config.yml` řídí přirozený double drop sběratelských dovedností.
Výchozí hodnota `0.002` znamená `0,20 %` za úroveň, maximum `0.20` znamená
`20 %` na úrovni 100. New Game+ používá `experience-multiplier: 0.50`,
`perk-stat-bonus-per-rank: 0.25` a
`innate-gathering-double-drop-multiplier-per-rank: 1.25`.

## Štěstí a kvalita Řemesla

Sekce `luck` v `skills/config.yml` řídí i povýšení kvality vlastní výroby.
`crafting-quality-chance-bonus-per-point: 0.05` přidá `5 %` šance za každý bod
Štěstí; s výchozím limitem dvou bodů je maximum `10 %`. Bonus se uplatní pouze
na kvalitu výbavy vytvořené hráčem, ne na loot ani vanilla recepty bez Řemesla.

## Zdroje XP

Každá dovednost má vlastní `experience-sources.yml`. Uprav číselnou hodnotu
vybraného zdroje a změnu načti příkazem `/nekararpg reload`. Hodnota `0`
vypne zdroj aktivity; hodnoty bloků musí zůstat kladné.

Těžba, Lesnictví a Kopání používají sekce `blocks` a `tags`; ostatní dovednosti
používají `sources`. Hráčem položené bloky ani režimy Creative a Spectator
nezískávají gathering XP. Bonus Echo Vein zůstává násobitelem v
`mining/config.yml`, protože patří modulu těžby, ne samostatnému zdroji XP.
