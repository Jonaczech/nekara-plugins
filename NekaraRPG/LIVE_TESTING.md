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
11. Ve smithing table bez šablony přeměň každý kožený kus a 1 železný ingot na odpovídající chainmail kus. Před dosažením Řemesla 20 nesmí jít výsledek vyzvednout. Chainmail vložený do pece ani vysoké pece nesmí vytvořit iron nugget ani vstoupit do kovárenského výkovu. U měděného, zlatého, železného, diamantového a netheritového výkovu zbraně, zbroje či nástroje ověř nahřátí ve vysoké peci s palivem; výsledek musí skončit v pravém výstupním slotu. Diamantový a netheritový kus se nahřeje po třech sekundách i bez vanilla tavicího receptu. Pak dej výkov do vedlejší ruky, do hlavní ruky odpovídající custom kladivo a při plížení pravým kliknutím na kovadlinu naklepávej dvě sekundy. Zbroj se nesmí obléknout ani proces zrušit; při přerušení či odpojení se výkov musí vrátit hráči. Měděné kladivo nesmí zpracovat železný ani diamantový výkov, vyšší kladivo nižší výkov smí. Teprve naklepaný kus ochlaď ve vodním kotli. Zbroj tím dokončíš, zbraň ještě plížením obrousíš na brusném kameni.

Unit testy nenahrazují tento průchod: ověřuj na Purpur 26.1.2 s Java 25.
12. S aktivní lehkou sadou a perkem Bleskové reflexy ověř +5 % rychlosti pohybu. Bonus musí zmizet po sejmutí sady; s Vynalézavým tulákem se musí aktivovat i při třech lehkých kusech.
13. Očaruj předmět v enchanting table s Runotepectvím i bez něj. Vanilla nabídky a maximální levely musí zůstat zachované; po dokončení musí lore obsahovat blok `Nekara · Výklad očarování`. S Čitelnými runami I–V ověř slevu 3/6/9/12/15 % z ceny XP a s Čistým pigmentem I–V šanci 4/8/12/16/20 % vrátit jeden lapis.
14. Vyrob Prázdnou runu z 4 uhlí, 4 smooth stone a redstonu. Na kovadlině ji spolu s amethyst shardem převeď na Magickou runu; musí mít glint bez skutečného vanilla enchantu a stát 3 levely.
15. S Magickou runou klikni na enchanting table. GUI musí zobrazit skutečnou cenu po slevě Šetrného zápisu: ze základu 6/9/12 úrovní XP až na 5/8/10 při V. hodnosti, plus 1/2/3 white dye. Tier II musí vyžadovat Runotepectví 30 a III. hodnost Šetrného zápisu; Tier III level 70 a III. hodnost Za hranou písma. S tímto perkem ověř 5/10/15% zachování barviva. Při zavření se Magická runa vrátí.
16. Pravý klik s Nestabilní runou na prázdný lectern ji musí probudit, přehrát efekt a udělit XP za krok `rune_awaken`. Ověř sockety ◇/◆ a sčítání Run poznání. Výklad magie musí na I. hodnosti přidat 5 % XP z koulí, na II. také 5 % XP do Runotepectví a na III. zvýšit oba bonusy na 10 %. Runová paměť musí mít 10% šanci vrátit runu a 2/2/3 úrovně XP; po New Game+ 20 % a o 25 % silnější statistické bonusy.

17. Použij bone meal na grass block a ověř zachovaný vanilla růst i dodatečné barevné květiny. Na rovné ploše sand i red sand musí bone meal vytvořit 1–3 pouštní porosty bez přepsání bloků; v zaplněném prostoru se nesmí spotřebovat. V kreativním režimu se nesmí spotřebovat nikdy.
