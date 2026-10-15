# PlantApp 0.2.1 – Android-telepítő

A `PlantApp-0.2.1-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Ez fejlesztői aláírással készült APK, nem Play Áruház-kiadás. A verziószám 0.2.1, a verziókód 3. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Javítás a 0.2.1 verzióban

A felismerés gombja elkészült fénykép után API-kulcs nélkül is aktív. Ha még nincs kulcs, megnyomásakor egy magyar üzenet jelzi, hogy a Menü → Beállítások oldalon kell megadni. A főoldalról kikerült a külön Beállítások-gomb és a kulcsra figyelmeztető állandó szöveg. A tényleges felismerés továbbra is érvényes kulcsot és internetkapcsolatot igényel.

Az új felületi teszt képfájlt hoz létre és feldolgoztat az alkalmazással; ellenőrzi a gomb engedélyezését kulcs nélkül és kulccsal, a hiányzó kulcs jelzését, valamint a főoldali beállítási hivatkozás hiányát.

## A 0.2.0 verzió újdonságai

- Magyar felület, hibaüzenetek, dátumok és pontszámok.
- **Menü → Beállítások:** Pl@ntNet-kulcs megadása és elfelejtése. A kulcs csak a memóriában marad.
- **Menü → Előzmények:** az utolsó 100 sikeres felismerés dátummal, találatokkal, mentett leírással és Wikipédia-hivatkozással. Az adatok az alkalmazás újraindítása után is megmaradnak; fotót és API-kulcsot nem tartalmaznak.
- Előzmények törlése megerősítéssel.

## Ellenőrzés

Az `assembleDebug testDebugUnitTest lintDebug` parancs sikeresen lefutott: **14 teszt sikeres, 0 hibás, 0 kihagyott**, a lint nem jelzett problémát. Az APK aláírását az Android SDK `apksigner verify` eszköze ellenőrizte. A 3 Robolectric/Compose felületi teszt Android 14 szimulált környezetében futott; a többi 11 teszt az API-válaszok feldolgozását és az előzmények tárolását ellenőrzi.

A fizikai kamerát és az élő Pl@ntNet-felismerést ebben a felhős környezetben nem ellenőriztük. Telefonon: adj meg saját kulcsot a Beállításokban, készíts fotót, indíts felismerést, majd nézd meg az Előzményeket. Zárd be és nyisd újra az alkalmazást: az előzmény maradjon meg. Törléskor ellenőrizd a **Mégse** és a **Törlés** gomb működését is. A Wikipédia leírásai és cikkei egyelőre angol nyelvűek lehetnek.
