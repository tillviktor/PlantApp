# PlantApp 0.3.0 – Android-telepítő

A `PlantApp-0.3.0-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Fejlesztői aláírással készült APK, verziószám: **0.3.0**, verziókód: **4**. A korábbi, ugyanebben a környezetben aláírt verziókra frissítésként telepíthető. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Újdonságok

- Menü: **Felismerés → Előzmények → Beállítások**.
- Az új, sikeres felismerések fotói a Részletek nézetben is megjelennek. A korábbi, kép nélkül mentett bejegyzések is olvashatók, de fotójuk nem állítható vissza.
- A képek a telefon saját tárhelyén maradnak, nem csak a gyorsítótárban. Az előzmények törlése a fotókat is törli; a 100-as korlátból kieső képeket is eltávolítjuk.
- A Beállításokban a **Mentés** gomb titkosítva megőrzi az API-kulcsot bezárás után is. A kulcs újraindításkor automatikusan betöltődik. Az **API-kulcs elfelejtése** a tárolt kulcsot is törli.
- Titkosítás: AES-GCM, az Android Keystore által kezelt titkosító kulccsal. API-kulcs, fotó és előzmény nem kerül felhőmentésbe.

## Telefonos ellenőrzés

Adj meg saját Pl@ntNet-kulcsot, nyomd meg a Mentés gombot, majd teljesen zárd be és nyisd újra az alkalmazást. A kulcs maradjon meg. Készíts új sikeres felismerést, nyisd meg az Előzmények → Részletek nézetet: a fotó is jelenjen meg újraindítás után. Előzmények törlésekor ellenőrizd a Mégse és a Törlés működését; a kulcs külön, a Beállításokban felejthető el.

A fizikai kamerát, az Android Keystore készülékspecifikus működését és az élő Pl@ntNet-kérést a felhős munkakörnyezetben nem ellenőriztük. A Wikipédia leírásai és cikkei angol nyelvűek lehetnek.

## Automatizált ellenőrzés

Az `assembleDebug testDebugUnitTest lintDebug` sikeres: **26 teszt sikeres, 0 hibás, 0 kihagyott**, a lint nem jelzett problémát. A tesztek a menüsorrendet, a mentett kép megjelenítését, a régi előzmények olvasását, a fotók megőrzését/törlését, a titkosított kulcs újratöltését új tároló- és alkalmazásmodell-példányban, a kulcs elfelejtését és a sérült titkosított adatok kezelését is ellenőrzik. A kulcstárolási tesztek valódi AES-GCM titkosítást használnak, tesztben biztosított titkosító kulccsal; a készülék Android Keystore szolgáltatását nem helyettesítik. Az APK aláírását az Android SDK `apksigner verify` eszköze ellenőrizte.
