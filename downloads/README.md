# PlantApp 0.5.1 – Android-telepítő

A `PlantApp-0.5.1-debug.apk` közvetlenül telepíthető Android 8.0 vagy újabb rendszerre. GitHubon a fájl oldalán a **Download raw file** gombbal tölthető le. Nyisd meg a telefonon, szükség esetén engedélyezd a telepítést a böngészőből/fájlkezelőből, majd válaszd a **Telepítés** lehetőséget.

Fejlesztői aláírással készült APK, verziószám: **0.5.1**, verziókód: **7**. A korábbi, ugyanebben a környezetben aláírt verziókra frissítésként telepíthető. A SHA-256 ellenőrzőösszeg a mellette található `.sha256` fájlban szerepel.

## Kijelölés és másolás

A felismerési eredmények és az előzmények magyar/köznevei, latin nevei és leírásai kijelölhetők. Hosszan érintsd meg a szöveget, igazítsd a kijelölést, majd válaszd a **Másolás** műveletet. Az előzményekben a nevek már a részletek megnyitása nélkül is másolhatók.

## Újdonságok a 0.5.1 verzióban

- A bélyegkép a Részletek megjelenítése megnyomása nélkül is látható.
- A részletekben nagyobb, jobb felbontással betöltött fotó jelenik meg.
- A részletes képre koppintva teljes képernyős nézet nyílik, csippentéses és dupla koppintásos nagyítással, eltolással, valamint Nagyítás/Kicsinyítés/Alaphelyzet gombokkal. A Bezárás vagy a rendszer Vissza gombja bezárja.
- Az Előzmények cím és a keresőmező közötti ismertető szöveg kikerült.
- A teljes nézet az elérhető mentett fájl teljes felbontását mutatja. A korábban legfeljebb 1600 pixelesre mentett képek eredeti kamerafelbontását nem lehet visszaállítani.

## Megmaradó funkciók

- Menü: **Felismerés → Előzmények → Beállítások**.
- Az új, sikeres felismerések fotói a Részletek nézetben is megjelennek. A korábbi, kép nélkül mentett bejegyzések is olvashatók, de fotójuk nem állítható vissza.
- A képek a telefon saját tárhelyén maradnak, nem csak a gyorsítótárban. Az előzmények törlése a fotókat is törli; a 100-as korlátból kieső képeket is eltávolítjuk.
- A Beállításokban a **Mentés** gomb titkosítva megőrzi az API-kulcsot bezárás után is. A kulcs újraindításkor automatikusan betöltődik. Az **API-kulcs elfelejtése** a tárolt kulcsot is törli.
- Titkosítás: AES-GCM, az Android Keystore által kezelt titkosító kulccsal. API-kulcs, fotó és előzmény nem kerül felhőmentésbe.

## Telefonos ellenőrzés

Adj meg saját Pl@ntNet-kulcsot, nyomd meg a Mentés gombot, majd teljesen zárd be és nyisd újra az alkalmazást. A kulcs maradjon meg. Készíts új sikeres felismerést, nyisd meg az Előzmények → Részletek nézetet: a fotó is jelenjen meg újraindítás után. Előzmények törlésekor ellenőrizd a Mégse és a Törlés működését; a kulcs külön, a Beállításokban felejthető el.

A fizikai kamerát, az Android Keystore készülékspecifikus működését, az élő Pl@ntNet-kérést és a névjegyzék szolgáltatásainak telefonos elérését a felhős munkakörnyezetben nem ellenőriztük. A felhős Wikipédia-próbát a hálózati proxy 403 válasszal blokkolta; a névfeldolgozást rögzített API-válaszokkal teszteltük. A Wikipédia leírásai és cikkei angol nyelvűek lehetnek.

## Automatizált ellenőrzés

Az `assembleDebug testDebugUnitTest lintDebug` sikeres: a meglévő **40 teszt sikeres, 0 hibás, 0 kihagyott**, a lint nem jelzett problémát. A másolást az Android/Compose natív szövegkijelölése biztosítja; a hosszan érintés és a rendszer Másolás menüjének készülékspecifikus működését telefonon is ellenőrizni kell. Az APK aláírását ellenőriztük, az aláíró megegyezik a 0.5.0 kiadáséval.
