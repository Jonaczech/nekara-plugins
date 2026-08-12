# ADR 0013: Rozšíření bone mealu podle podkladu

## Kontext

Vanilla bone meal umí na trávě vytvořit pouze omezenou část květin a na písku ani rudém písku nevytváří pouštní vegetaci. Nekara potřebuje zpřístupnit barviva a oživit pouštní biomy, aniž by nahradila nebo rozbila vanilla hnojení.

## Rozhodnutí

Modul `bone-meal` používá `BlockFertilizeEvent` pro úspěšné hnojení trávy. Vanilla operace se nejprve dokončí; plugin až poté zkusí umístit dvě další květiny na volné okolní bloky stejného podkladu.

Pro písek a rudý písek používá modul `PlayerInteractEvent`, protože vanilla hnojení zde nevytváří odpovídající výsledek. Událost se zruší a bone meal se spotřebuje až po alespoň jednom platném umístění. Tím nevzniká ztráta předmětu v zaplněném prostoru a nezasahuje se do případných budoucích vanilla úspěšných interakcí.

Všechna umístění ověřují volný cílový blok. Pouštní pool obsahuje pouze Dead Bush, Short dry grass, Tall dry grass a Cactus Flower. Cactus Flower se pokládá přímo na plnou horní plochu písku; modul nevytváří žádný Cactus.

## Důsledky

- Vanilla růst na trávě zůstává autoritativní a je pouze obohacený.
- Pouštní růst je determinovaný váhami, ale jeho konkrétní umístění zůstává náhodné a omezené prostorem.
- Mechanika není spojena se Skills ani s perky, proto funguje při vypnutém modulu dovedností a lze ji vypnout samostatně.
