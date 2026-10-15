# PlantApp

An Android app built with Kotlin and Jetpack Compose. Take a plant photo, see up to three Pl@ntNet matches with common/scientific names, family and match score, and read a Wikipedia description or open its reference page.

## 0.6.1 – Képnagyítási súgószöveg eltávolítása

**Legújabb telepítő:** [`downloads/PlantApp-0.6.1-debug.apk`](downloads/PlantApp-0.6.1-debug.apk).

A részletes kép alatti „Koppints a képre a teljes méretű nézethez.” szöveget eltávolítottuk. A képre koppintva továbbra is megnyílik a teljes képernyős képnézet.

## 0.6.0 – Bélyegképről nyitható részletek és kuka ikon

**0.6.0 telepítő:** [`downloads/PlantApp-0.6.0-debug.apk`](downloads/PlantApp-0.6.0-debug.apk).

- Csak a növény közneve és latin neve kijelölhető, a felismerés és az előzmények oldalán egyaránt. A dátum, pontszám, család és leírás nem kijelölhető.
- Az egyedi törlés szöveges hivatkozását a kártya jobb felső sarkában kuka ikon váltotta fel. A törlés továbbra is megerősítést kér.
- A Részletek megjelenítése szöveges hivatkozás kikerült. A bélyegképre koppintás nyitja ki a részleteket; régi, fotó nélküli bejegyzésnél a kép helyén lévő mezőre lehet koppintani.
- Nyitott állapotban a **Részletek elrejtése** hivatkozás látható és visszacsukja a kártyát. A nagy részletes képre koppintva a teljes képernyős nézet nyílik meg.

## 0.5.1 – Másolható növénynevek és leírások

**0.5.1 telepítő:** [`downloads/PlantApp-0.5.1-debug.apk`](downloads/PlantApp-0.5.1-debug.apk).

A felismerési eredmények nevei (magyar/köznév és latin név), családja, pontszáma és leírása kijelölhető és másolható. Az előzmények összecsukott kártyáján a nevek és a dátum, a részletekben pedig az eredmények szövege ugyanígy másolható. Hosszan érintsd meg a kívánt szöveget, állítsd be a kijelölést a fogantyúkkal, majd válaszd az Android **Másolás** műveletét. A fotók és a gombok külön maradnak a kijelölhető szövegtől.

## 0.5.0 – Bélyegképek és teljes képernyős fotónézet

**0.5.0 telepítő:** [`downloads/PlantApp-0.5.0-debug.apk`](downloads/PlantApp-0.5.0-debug.apk).

- A mentett fotó bélyegképe a bezárt előzménykártyán is látszik.
- A Részletek megjelenítése 320 dp magas képet mutat a korábbi 240 dp helyett, a mentett fájlból nagyobb felbontással betöltve.
- A részletes képre koppintva teljes képernyős nézet nyílik. Csippentéssel, dupla koppintással és a Nagyítás/Kicsinyítés gombokkal 1–5× nagyítás érhető el. Nagyítva a kép eltolható, az Alaphelyzet gomb visszaállítja a nézetet. Bezárással vagy a rendszer Vissza gombjával visszatérhetsz az előzményekhez.
- Az Előzmények cím és a keresőmező közötti ismertető szöveget eltávolítottuk.
- A teljes nézet a mentett fájl összes pixelét tölti be. A korábbi képrögzítési folyamat legfeljebb 1600 pixeles fotót mentett; ez továbbra is a mentett kép felbontása, az eredeti kamerakép utólag nem állítható vissza. A nagyítás nem talál ki hiányzó részleteket.

## 0.4.0 – Egyenkénti törlés és név szerinti keresés

**0.4.0 telepítő:** [`downloads/PlantApp-0.4.0-debug.apk`](downloads/PlantApp-0.4.0-debug.apk).

- Az Előzmények oldalon minden kártyán külön **Előzmény törlése** gomb van. Megerősítés után csak az adott felismerés és a hozzá tartozó fotó törlődik; a többi bejegyzés és az API-kulcs megmarad.
- A **Keresés név szerint** mező részleges magyar, angol és latin neveket is keres, ékezettől és kis-/nagybetűtől függetlenül, az összes mentett találatban. Üres kereséskor a teljes lista visszaáll. A kereső melletti × csak a keresést üríti.
- A Pl@ntNet összes visszaadott köznevét megőrizzük. Kereséskor a még nem kiegészített nevekhez, ha a szolgáltatások elérhetők, Wikipédia-átirányítások és Wikidata magyar/angol címkék, illetve névváltozatok kerülnek a helyi névjegyzékbe. Csak a tudományos neveket küldjük ezekhez a szolgáltatásokhoz, fotót vagy API-kulcsot nem.
- A mentett nevek között a keresés offline is működik. A régi bejegyzések angol/magyar névváltozatainak első kiegészítéséhez internet kell; elérhető nevek hiányában az eredeti név és a latin név továbbra is kereshető. A névjegyzék frissítése nem hoz vissza közben törölt előzményeket. Sikertelen letöltés után a **Névváltozatok frissítése** gombbal újra lehet próbálni.
- Az **Összes előzmény törlése** továbbra is az egész listát törli, aktív keresés esetén is; ezt a külön megerősítés jelzi.

## 0.3.0 – Fotós előzmények és megjegyzett API-kulcs

**0.3.0 telepítő:** [`downloads/PlantApp-0.3.0-debug.apk`](downloads/PlantApp-0.3.0-debug.apk).

- A menü sorrendje: **Felismerés → Előzmények → Beállítások**.
- Az új, sikeres felismeréseknél a helyadatoktól megtisztított, legfeljebb 1600 pixeles fotót is az alkalmazás belső tárhelyére másoljuk. Az Előzmények → Részletek nézetben a kép is megjelenik. A gyorsítótár ürítése nem törli ezeket a fotókat.
- A korábbi, kép nélkül tárolt előzmények megmaradnak, de az elveszett régi fotókat nem lehet visszaállítani; ezeknél külön üzenet látható.
- Az előzmények törlése a mentett fotókat is törli. A 100 bejegyzéses korlátból kieső fotókat az alkalmazás eltávolítja.
- **Menü → Beállítások → Mentés:** az API-kulcs titkosítva megmarad bezárás és újraindítás után. A titkosítás AES-GCM, a titkosító kulcsot az Android Keystore kezeli. Az API-kulcs nem kerül az előzményekbe, felhőmentésbe vagy a forráskódba.
- **API-kulcs elfelejtése:** a tárolt API-kulcs törlése. Ha módosítod a kulcsot, a tartós megőrzéshez ismét nyomd meg a Mentés gombot.

Az alkalmazás eltávolítása vagy adatainak törlése a fotókat, az előzményeket és a mentett API-kulcsot is eltávolítja. A 0.2.x verziók alábbi leírásai az akkori működést mutatják.

## 0.2.1 – Felismerés gomb javítása

A fénykép előkészítése után a felismerés gombja API-kulcs nélkül is aktív. Hiányzó kulcs esetén megnyomáskor egy bezárható magyar üzenet jelzi, hogy a Menü → Beállítások oldalon kell megadni a kulcsot; az alkalmazás nem küld hitelesítés nélküli kérést. A főoldalon nincs külön Beállítások-hivatkozás vagy kulcsmegadást kérő szöveg. A tényleges felismerés továbbra is érvényes Pl@ntNet-kulcsot igényel.

**0.2.1 telepítő:** [`downloads/PlantApp-0.2.1-debug.apk`](downloads/PlantApp-0.2.1-debug.apk).

## 0.2.0 – Magyar felület és előzmények

- Magyar nyelvű felület, üzenetek, dátumok és pontszámok.
- **Menü → Beállítások:** Pl@ntNet API-kulcs megadása, igénylési hivatkozás és a kulcs elfelejtése. A kulcs csak a memóriában marad; az alkalmazás folyamatának újraindulása után ismét meg kell adni.
- **Menü → Előzmények:** az utolsó 100 sikeres felismerés, legújabb elöl, dátummal, a találatokkal és a már letöltött leírással. A részletek és a Wikipédia-hivatkozások később is megnyithatók; a mentett adatok megtekintése nem igényel internetet.
- Az előzmények az alkalmazás saját belső tárhelyén maradnak újraindítás után is. Nem tartalmaznak fotót, helyadatot vagy API-kulcsot, és nem kerülnek felhőmentésbe. Az alkalmazás eltávolításakor vagy adatainak törlésekor elvesznek.
- Az összes előzmény törlése megerősítést kér. Sérült előzményfájlt az alkalmazás nem ír felül csendben; hibát jelez, és lehetőséget ad az újrapróbálásra vagy törlésre.
- A Pl@ntNet magyar közneveket kér (`lang=hu`); elérhetőségük az adatbázistól függ. A tudományos nevek latinul, a Wikipédia leírásai és hivatkozásai egyelőre angolul jelenhetnek meg.

Telepítés: a [`downloads/PlantApp-0.2.0-debug.apk`](downloads/PlantApp-0.2.0-debug.apk) fájlnál kattints a **Download raw file** gombra, másold a telefonodra, majd nyisd meg. Android 8.0 vagy újabb szükséges. Ez egy fejlesztői kulccsal aláírt, közvetlenül telepíthető APK. A korábbi, ugyanebben a környezetben aláírt 0.1.0 verzióra frissítésként telepíthető. Az ellenőrzőösszeg a mellette található `.sha256` fájlban van.


## Run on a phone

1. Open this project in Android Studio, using JDK 21 and Android SDK 35. Allow Gradle sync to complete.
2. Run the `app` configuration on an Android 8.0+ device with a camera app and internet access.
3. Create a personal API key at https://my.plantnet.org/ and paste it into **Menü → Beállítások**, then tap **Mentés** to remember it. Do not commit keys.
4. Take a close, well-lit photo of a leaf or flower, then tap **Növény felismerése**. The app explicitly uploads the photo only when you tap Identify.

The API key is encrypted with AES-GCM in private internal storage when you tap **Mentés**. Its encryption key is generated and held by Android Keystore. It is loaded automatically by a new application process. **API-kulcs elfelejtése** deletes the persisted API credential; unsaved edits remain only in memory. Identification requires a valid personal key and available quota. This prototype does not ship a shared service key. A public release should use a backend to manage service credentials, quotas and abuse protection.

The system camera saves to private app cache. Images are resized to at most 1600 pixels on the longest side, rotated according to EXIF, and re-encoded to remove location metadata before upload. Replaced working photos are deleted; abandoned camera files are cleaned after a day when taking another photo. Successful identifications copy the sanitized image to private internal history storage. Cached working files can be removed independently; history photos persist until their record is pruned or the history is cleared. Android may clear cached photos at any time. No gallery or camera permission is requested: capture is delegated to the installed camera app through a temporary FileProvider grant.

Descriptions are fetched from English Wikipedia when available; identification still works if the summary is unavailable. Reference links use the predicted scientific name and may lead to a missing article. Identification is probabilistic and must not determine edibility or medical use. Pl@ntNet and Wikipedia have their own service terms, privacy policies and attribution requirements. Wikipedia text is credited in the app under CC BY-SA; linked pages provide authorship and revision history.

## Build and validate

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Tests cover real API-shaped response parsing, candidate ordering, empty/missing data, confidence bounds and Wikipedia URL escaping; history persistence across store instances, ordering, deduplication, the 100-entry limit, durable deletion, Unicode/null descriptions, invalid entries and corrupted/unknown files. Robolectric/Compose tests exercise Hungarian menu navigation, API-key editing outside the home screen, history reloading and detail expansion, and confirmation/cancellation of history deletion. A physical device or emulator plus a valid API key is required to verify the complete camera/network flow. No live recognition results are fabricated or bundled.

## Codex cloud environment

Use the existing `/workspace/PlantApp` checkout; each task is already isolated, so no Git worktree is needed.

```sh
python3 tools/bootstrap_cloud.py
. tools/cloud-env.sh
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

The bootstrap installs checksum-verified Android command-line tools and JDK 21 below `/workspace/toolchains`, accepts Android SDK licenses, and prepares SDK 35, build tools 35 and platform-tools. It configures Java proxy routing and trusts the platform CA while retaining TLS verification. It requires Python 3.12+, curl, an installed system Java CA truststore and HTTPS access to GitHub, Google's Android repositories, Gradle and Maven repositories. No API credentials are required to compile or run unit tests. There is no server process to start. Toolchains and dependencies can be retained in a published environment snapshot; shell exports must be restored per task.
