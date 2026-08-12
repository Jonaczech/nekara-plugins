# Custom itemy NekaraRPG

## Použití editoru

Editor je pouze pro operátory nebo hráče s oprávněním `nekararpg.item.admin`.

1. Vezmi do hlavní ruky vanilla předmět, který má být základem itemu.
2. Spusť `/nrpg item create`.
3. Kliknutím v GUI nastav stabilní ID, název, model a statistiky.
4. Položka `Vanilla základ` znovu načte aktuální předmět z hlavní ruky.
5. Texty a čísla se zadávají přes GUI kovadliny. Znak `-` volitelnou statistiku
   nebo `CustomModelData` vypne.
6. Klikni na `Vytvořit a uložit`. Definice se zapíše do
   `plugins/NekaraRPG/custom-items/items.yml` a hotový item dostaneš do inventáře.

Existující ID se nepřepisuje. ID používá pouze malá písmena, čísla, pomlčku a
podtržítko; po vydání itemu jej považuj za neměnné.

## Resource-pack kontrakt

Primární vazba je moderní model `nekararpg:<model-key>`. Například model
`nekararpg:weapons/ocelovy_mec` musí mít odpovídající definici v resource packu v
namespace `nekararpg`. Číselné `CustomModelData` je pouze volitelný legacy údaj.
Bez resource packu zůstane předmět použitelný s nastaveným vanilla základem.

Serverová identita je vždy PDC `nekararpg:custom_item_id`; název ani textura
nejsou bezpečným podkladem pro herní logiku.

## Podporované statistiky

- poškození a rychlost útoku pro hlavní ruku,
- brnění a odolnost brnění pro slot odvozený z vanilla základu,
- bonus maximálního zdraví pro stejný slot.

Prázdná statistika zachová vanilla chování. Zadaná hodnota vytvoří standardní
namespaced atributový modifier, takže se zobrazí i v tooltipu Minecraftu.

## Povinný živý test

- Vytvoř meč, ověř PDC ID, model, poškození a rychlost útoku.
- Vytvoř každý typ brnění a ověř, že se bonus aktivuje jen ve správném slotu.
- Zkus uložit duplicitní ID; editor jej musí odmítnout bez přepsání YAML.
- Ověř item s resource packem i bez něj a také po restartu serveru.
- Během zadávání zavři kovadlinu, spusť `/nrpg reload` a ověř, že nezůstane otevřené
  ani rozpracované GUI.
