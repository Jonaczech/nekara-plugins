# Živá akceptace NekaraRPG

1. Zastav server, zálohuj současný JAR i `plugins/NekaraRPG`, nahraj nový JAR a proveď plný restart.
2. Ověř v konzoli načtení všech zapnutých modulů bez chybějící závislosti.
3. Otevři `/nrpg`, ověř nativní přehled Dovedností a každý směr perk-tree.
4. Vytěž stone nebo deepslate s aktivním `modules.mining.enabled`; Echo Vein musí být schopná označit blízký hostitelský blok.
5. Dokonči označený blok: jeden nativní bonus XP do Těžby, nejvýše jeden bonusový přirozený drop a žádné druhé Fortune přepočítání.
6. Dokonči rybářskou minihru na levelech Rybaření 1, 31, 61 a 100; ověř příslušnou obtížnost, původní vanilla úlovek a jednu nativní XP odměnu.
7. Bez Rested a s Rested zopakuj těžbu, rybaření a boj. Bonus musí být přesně dle `campfire.rested.skills-experience.multiplier` a nesmí se týkat admin příkazů.
8. V boji ověř výrazné, ale krátké potvrzení proců: kritický zásah vytvoří bílé jiskry, krvácení tmavě červený záblesk a každý plně nabitý útok těžkou zbraní oblouk silového útoku. Krvácení pak nad cílem při každém svém tiku použije jeden indikátor poškození.
9. Ověř `/nrpg reload`, odpojení během minihry, teleport a vypnutí každého modulu. Nesmí zůstat aktivní task, bossbar ani dočasná odměna.
10. Na drakovi zkus proletět zdí, nízkým stropem, do jeskyně a pod terén. Pohyb se musí na hraně otevřeného prostoru zastavit; drak nesmí projít bloky ani pokračovat pod povrchem.
11. Ve smithing table bez šablony přeměň každý kožený kus a 1 železný ingot na odpovídající chainmail kus. Před dosažením Řemesla 20 nesmí jít výsledek vyzvednout. Kovový výkov musí projít skutečným tavením ve vysoké peci; kliknutí s předmětem v ruce nesmí stav přeskočit. Pak při plížení a pravém kliknutí na kovadlinu drž výkov dvě sekundy: musí hrát údery kovadliny a objevit se jiskry i kouř. Teprve naklepaný kus ochlaď ve vodním kotli. Zbroj tím dokončíš, zbraň ještě plížením obrousíš na brusném kameni.

Unit testy nenahrazují tento průchod: ověřuj na Purpur 26.1.2 s Java 25.
12. S aktivní lehkou sadou a perkem Bleskové reflexy ověř +5 % rychlosti pohybu. Bonus musí zmizet po sejmutí sady; s Vynalézavým tulákem se musí aktivovat i při třech lehkých kusech.
