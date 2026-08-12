# Runy NekaraRPG

Runy jsou samostatná vrstva výbavy vedle vanilla enchantů a kvality Řemesla.
Používají serverová PDC data, ne název nebo lore. Počet socketů určuje kvalita
vyrobeného předmětu a stejnou runu lze do jednoho předmětu vložit opakovaně.

## Postup výroby

```text
4 uhlí + 4 smooth stone + redstone
→ Prázdná runa

Prázdná runa + amethyst shard na kovadlině (3 levely)
→ Magická runa

Magická runa v ruce + pravý klik na enchanting table
→ otevři vlastní runové GUI
→ GUI spočítá white dye v inventáři
→ klikni na Tier I, II nebo III
→ Nestabilní Runa poznání

Nestabilní runa v ruce + pravý klik na prázdný lectern
→ okamžité probuzení se zvukem a částicemi
→ Probuzená Runa poznání

Výbava vlevo + Probuzená runa vpravo na kovadlině
→ runa se vková do prvního volného socketu
```

Magická, nestabilní i probuzená runa používají pouze vizuální enchantment glint;
neobsahují skutečný vanilla enchant.

## Tiery

- Tier I: Runotepectví 1 a I. hodnost `Čitelných run`; zisk XP dovedností +1 %.
- Tier II: Runotepectví 30 a III. hodnost `Šetrného zápisu`; zisk XP +3 %.
- Tier III: Runotepectví 70, III. hodnost `Šetrného zápisu` a III. hodnost
  `Za hranou písma`; zisk XP +5 %.

Custom GUI ukazuje Magickou runu, počet white dye v hráčově inventáři a tři
tlačítka Tier I–III. Základní cena tierů je 6 / 9 / 12 úrovní XP a perk
`Šetrný zápis` ji snižuje až o 20 %. GUI vždy zobrazuje skutečnou cenu po slevě.
Zápis vyžaduje 1 / 2 / 3 white dye přímo v inventáři. `Za hranou písma` má podle
hodnosti 5 / 10 / 15% šanci pigment zachovat. Barvivo se do GUI ručně nevkládá.
Zamčený nebo nezaplacený tier zůstává viditelný, ale server jeho volbu odmítne.

## Perktree Runotepectví

Levá větev je zaměřena na výrobu run:

- `Čitelné runy` mají 5 hodností. I. hodnost odemkne Tier I; každá hodnost
  snižuje cenu vanilla očarování o 3 %, maximálně o 15 %.
- `Šetrný zápis` má 5 hodností. Každá snižuje cenu zápisu run o 4 %, maximálně
  o 20 %; III. hodnost odemkne Tier II.
- `Za hranou písma` má 3 hodnosti. Zachovává pigment s šancí 5 / 10 / 15 %;
  III. hodnost odemkne Tier III.

Pravá větev zlepšuje vanilla enchantování a zkušenosti:

- `Čistý pigment` má 5 hodností a za každou přidává 4% šanci vrátit jeden lapis
  spotřebovaný vanilla očarováním, maximálně 20 %.
- `Výklad magie` má 3 hodnosti: +5 % XP z koulí; poté také +5 % XP do
  Runotepectví; na III. hodnosti jsou oba bonusy +10 %.

`Runová paměť` vyžaduje III. hodnost obou hlubokých perků. Při úspěšném vkování
má 10% šanci vrátit probuzenou runu a 25 % její základní ceny zápisu (2 / 2 / 3
úrovně XP podle tieru). Po New Game+ se šance vrácení zvýší na 20 % a statistické
bonusy perků se stejně jako u ostatních dovedností zesílí o 25 %. Knihovny,
vanilla enchanting table i její nabídky výbavy zůstávají beze změny.

## Sockety podle kvality

| Kvalita | Sockety |
|---|---:|
| Běžná | 1 |
| Neobyčejná | 1 |
| Vzácná | 2 |
| Epická | 2 |
| Legendární | 3 |

Lore používá znak `◇` pro prázdný a `◆` pro obsazený socket. Pod řádkem socketů
uvádí každou vloženou runu, její tier a přesný efekt. Runa poznání je univerzální:
lze ji vložit do každého socketu zbroje, zbraně nebo nástroje a lze ji opakovat.

Bonusy všech Run poznání na předmětech v hlavní ruce, vedlejší ruce a oblečené
zbroji se nejprve sečtou. Výsledný bonus násobí běžně získané XP všech aktivních
dovedností; nevztahuje se na syntetické a administrativní přidělení XP.

## Katalog

Aktuálně lze vyrobit jediný efekt:

| Barvivo | Runa | Cíl | Tier I / II / III |
|---|---|---|---|
| Bílé | Runa poznání | každý kvalitní kus výbavy se socketem | +1 / +3 / +5 % XP dovedností |

Dřívější experimentální ID run zůstávají čitelná kvůli již existujícím předmětům,
ale enchanting table je nově nenabízí a nelze je vyrábět.

## Vanilla interakce

- Běžná enchanting table se zbraní, nástrojem nebo zbrojí zůstává vanilla.
- Runové GUI se otevře jen při pravém kliku s Magickou runou v ruce. Používá
  běžný serverový inventář, protože klientský lapis slot nepřijímá white dye.
- Lectern s knihou zůstává vanilla. Nestabilní runa se okamžitě probudí pouze
  pravým klikem na prázdný lectern; znakovou minihru systém nepoužívá.
- Kovadlina vkládá probuzenou runu jen do kvalitní výbavy s volným socketem.
  Ostatní kombinace kovadliny zůstávají vanilla.

## Resource pack

Všechny fáze runy používají namespaced model `nekararpg:runes/placeholder` s dodanou
16×16 texturou prázdné runy. Magická, nestabilní a probuzená fáze nad ní zobrazují
glint. Herní identita, sockety a efekty závisejí pouze na PDC datech.
