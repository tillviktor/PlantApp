# PlantApp 0.6.0 – Android-telepítő

A `PlantApp-0.6.0-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Fejlesztői aláírással készült APK, verziószám: **0.6.0**, verziókód: **8**. A korábbi, ugyanebben a környezetben aláírt verziókra frissítésként telepíthető. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Újdonságok

- Csak a növény közneve és latin neve kijelölhető és másolható; a leírás és a többi adat nem. Hosszan érintsd meg a nevet, majd válaszd a Másolás műveletet.
- A kártya jobb felső sarkában kuka ikon jelzi az előzmény törlését. A megerősítés továbbra is szükséges; a kiválasztott fotó is törlődik.
- A részletek a bélyegképre koppintva nyílnak meg, külön Részletek megjelenítése hivatkozás nincs. Fotó nélküli régi bejegyzésnél a fotó helyén lévő mező is megnyitja a részleteket.
- A Részletek elrejtése csak kinyitott állapotban látható és visszacsukja a kártyát.
- A nagy részletes képre koppintva továbbra is teljes képernyős, nagyítható nézet nyílik.

A név szerinti keresés, a titkosítva megjegyzett API-kulcs és a mentett előzmények változatlanul használhatók. A teljes képnézet a mentett fájl felbontását használja, amelynek hosszabb oldala legfeljebb 1600 pixel. A fizikai kamerát, a telefonos gesztusokat, az Android Keystore készülékspecifikus működését és az élő külső szolgáltatásokat telefonon is ellenőrizni kell.

## Automatizált ellenőrzés

Az `assembleDebug testDebugUnitTest lintDebug` sikeres: **40 teszt sikeres, 0 hibás, 0 kihagyott**, a lint nem jelzett problémát. A módosított felületi tesztek a bélyegképről nyitást, a megnyitó szöveges hivatkozás hiányát, a kuka ikon elérését, a részletek elrejtését és újranyitását, valamint a teljes képnézetet és a korábbi törlés/keresés funkciókat ellenőrzik. A fotó nélküli bejegyzés részletei is megnyithatók. A névkijelölés Android/Compose SelectionContainer használatával működik, kizárólag a köznév és a latin név körül; a készülék Másolás menüjét telefonon is ellenőrizni kell. Az APK aláírása ellenőrzött és megegyezik a 0.5.1 kiadáséval.
