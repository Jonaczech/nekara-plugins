# Živá akceptace NekaraRPG

1. Zastav server, zálohuj současný JAR i `plugins/NekaraRPG`, nahraj nový JAR a proveď plný restart.
2. Ověř v konzoli načtení všech zapnutých modulů bez chybějící závislosti.
3. Otevři `/nrpg`, ověř nativní přehled Dovedností a každý směr perk-tree.
4. Vytěž stone nebo deepslate s aktivním `modules.mining.enabled`; Echo Vein musí být schopná označit blízký hostitelský blok.
5. Dokonči označený blok: jeden nativní bonus XP do Těžby, nejvýše jeden bonusový přirozený drop a žádné druhé Fortune přepočítání.
6. Dokonči rybářskou minihru na levelech Rybaření 1, 31, 61 a 100; ověř příslušnou obtížnost, původní vanilla úlovek a jednu nativní XP odměnu.
7. Bez Rested a s Rested zopakuj těžbu, rybaření a boj. Bonus musí být přesně dle `campfire.rested.skills-experience.multiplier` a nesmí se týkat admin příkazů.
8. Ověř `/nrpg reload`, odpojení během minihry, teleport a vypnutí každého modulu. Nesmí zůstat aktivní task, bossbar ani dočasná odměna.

Unit testy nenahrazují tento průchod: ověřuj na Purpur 26.1.2 s Java 25.
