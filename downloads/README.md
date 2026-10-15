# PlantApp 0.6.1 – Android-telepítő

A `PlantApp-0.6.1-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Fejlesztői aláírással készült APK, verziószám: **0.6.1**, verziókód: **9**. A korábbi, ugyanebben a környezetben aláírt verziókra frissítésként telepíthető. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Újdonságok

- Csak a növény közneve és latin neve kijelölhető és másolható; a leírás és a többi adat nem. Hosszan érintsd meg a nevet, majd válaszd a Másolás műveletet.
- A kártya jobb felső sarkában kuka ikon jelzi az előzmény törlését. A megerősítés továbbra is szükséges; a kiválasztott fotó is törlődik.
- A részletek a bélyegképre koppintva nyílnak meg, külön Részletek megjelenítése hivatkozás nincs. Fotó nélküli régi bejegyzésnél a fotó helyén lévő mező is megnyitja a részleteket.
- A Részletek elrejtése csak kinyitott állapotban látható és visszacsukja a kártyát.
- A nagy részletes képre koppintva továbbra is teljes képernyős, nagyítható nézet nyílik.

A név szerinti keresés, a titkosítva megjegyzett API-kulcs és a mentett előzmények változatlanul használhatók. A teljes képnézet a mentett fájl felbontását használja, amelynek hosszabb oldala legfeljebb 1600 pixel. A fizikai kamerát, a telefonos gesztusokat, az Android Keystore készülékspecifikus működését és az élő külső szolgáltatásokat telefonon is ellenőrizni kell.

## Ellenőrzés

A 0.6.1 csak a kép alatti magyarázó szöveget távolítja el. A teljes képernyős nézet továbbra is megnyitható a képre koppintva. Ennél a szövegmódosításnál az APK buildjét, a lintet és az aláírást ellenőrizzük; a tesztcsomagot nem futtatjuk újra. A 0.6.0 verzió 40 sikeres tesztjének eredménye a korábbi kiadás leírásában szerepel.
