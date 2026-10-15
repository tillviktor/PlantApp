# PlantApp 0.2.0 – Android-telepítő

A `PlantApp-0.2.0-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Ez fejlesztői aláírással készült APK, nem Play Áruház-kiadás. A verziószám 0.2.0, a verziókód 2. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Újdonságok

- Magyar felület, hibaüzenetek, dátumok és pontszámok.
- **Menü → Beállítások:** Pl@ntNet-kulcs megadása és elfelejtése. A kulcs csak a memóriában marad.
- **Menü → Előzmények:** az utolsó 100 sikeres felismerés dátummal, találatokkal, mentett leírással és Wikipédia-hivatkozással. Az adatok az alkalmazás újraindítása után is megmaradnak; fotót és API-kulcsot nem tartalmaznak.
- Előzmények törlése megerősítéssel.

## Ellenőrzés

Az `assembleDebug testDebugUnitTest lintDebug` parancs sikeresen lefutott: **13 teszt sikeres, 0 hibás, 0 kihagyott**, a lint nem jelzett problémát. Az APK aláírását az Android SDK `apksigner verify` eszköze ellenőrizte. A 2 Robolectric/Compose felületi teszt Android 14 szimulált környezetében futott; a többi 11 teszt az API-válaszok feldolgozását és az előzmények tárolását ellenőrzi.

A fizikai kamerát és az élő Pl@ntNet-felismerést ebben a felhős környezetben nem ellenőriztük. Telefonon: adj meg saját kulcsot a Beállításokban, készíts fotót, indíts felismerést, majd nézd meg az Előzményeket. Zárd be és nyisd újra az alkalmazást: az előzmény maradjon meg. Törléskor ellenőrizd a **Mégse** és a **Törlés** gomb működését is. A Wikipédia leírásai és cikkei egyelőre angol nyelvűek lehetnek.
