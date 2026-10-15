# PlantApp

An Android app built with Kotlin and Jetpack Compose. Take a plant photo, see up to three Pl@ntNet matches with common/scientific names, family and match score, and read a Wikipedia description or open its reference page.

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
3. Create a personal API key at https://my.plantnet.org/ and paste it into **Menü → Beállítások**. Do not commit keys.
4. Take a close, well-lit photo of a leaf or flower, then tap **Növény felismerése**. The app explicitly uploads the photo only when you tap Identify.

The key remains in memory for the current app process (including screen rotation); it is not saved to disk. You must enter it again after the process exits. Identification requires a valid key and available quota. This personal-key prototype does not ship a shared service key. A public release should use a backend to manage credentials, quotas and abuse protection.

The system camera saves to private app cache. Images are resized to at most 1600 pixels on the longest side, rotated according to EXIF, and re-encoded to remove location metadata before upload. Replaced photos are deleted; abandoned camera files are cleaned after a day when taking another photo. Android may clear cached photos at any time. No gallery or camera permission is requested: capture is delegated to the installed camera app through a temporary FileProvider grant.

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
