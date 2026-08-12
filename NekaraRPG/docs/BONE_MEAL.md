# Bone meal a vegetace

Modul `bone-meal` rozšiřuje používání bone mealu bez zásahu do dovedností. Lze jej samostatně vypnout v `modules.bone-meal.enabled`.

## Tráva

Po úspěšném vanilla hnojení `grass_block` plugin provede dvě dodatečné pokusy o osázení blízké trávy. Výběr zahrnuje všechny vanilla květiny použitelné pro výrobu barviv, včetně tulipánů, vysokých květin, torchfloweru a pitcher plantu. Vanilla růst trávy, poppy a dandelion zůstává beze změny.

## Písek a rudý písek

Použití bone mealu na `sand` nebo `red_sand` vytvoří v dosahu tří bloků jeden až tři nové pouštní porosty. Použité váhy jsou:

- Dead Bush: 35 %
- Short dry grass: 25 %
- Tall dry grass: 20 %
- Cactus flower: 20 %

Bone meal se odečte pouze tehdy, pokud vznikne alespoň jedna rostlina; v kreativním režimu se neodečítá. Modul nikdy nepřepisuje bloky. Cactus flower se pokládá přímo na písek; bone meal žádný cactus nevytváří.

## Povinný živý test

1. Na běžné trávě opakovaně použij bone meal a ověř dodatečné barevné květiny.
2. Na rovné ploše písku i rudého písku použij bone meal a ověř pouštní vegetaci, včetně cactus floweru, ale bez jediného nového cactusu.
3. Zopakuj pokus v zaplněném prostoru: nesmí se odečíst bone meal ani přepsat sousední blok.
4. Ověř kreativní režim: porost vzniká, ale bone meal se nespotřebuje.
