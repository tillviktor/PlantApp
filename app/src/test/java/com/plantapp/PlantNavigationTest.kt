package com.plantapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "hu")
class PlantNavigationTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val historyFile get() = File(ApplicationProvider.getApplicationContext<Application>().filesDir, "recognition-history.json")
    private val historyFixture = object : ExternalResource() {
        override fun before() {
            historyFile.delete()
            File(historyFile.parentFile, "api-key.enc").delete()
            HistoryStore(historyFile).add(HistoryEntry("test-entry", 1700000000000,
                listOf(PlantResult(PlantMatch("Bellis perennis", "Százszorszép", "Asteraceae", 0.95), "Mentett növényleírás"))))
        }
        override fun after() { HistoryStore(historyFile).clear(); File(historyFile.parentFile, "api-key.enc").delete() }
    }
    @get:Rule val rules: TestRule = RuleChain.outerRule(historyFixture).around(compose)

    private fun openMenu(destination: String) {
        compose.onNodeWithText("Menü").performClick()
        // Invoke the accessible menu action: Robolectric popup windows do not
        // provide physical screen coordinates for touch injection.
        compose.onNode(hasText(destination) and hasAnyAncestor(isPopup()))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun awaitHistory() {
        compose.waitUntil(10000) {
            var ready = false
            compose.runOnIdle { ready = !ViewModelProvider(compose.activity)[PlantViewModel::class.java].historyBusy }
            ready
        }
    }

    @Test fun apiKeyLivesInSettingsAndSurvivesNavigation() {
        compose.onNodeWithText("Növény lefotózása").assertIsDisplayed()
        compose.onNodeWithText("Pl@ntNet-kapcsolat").assertDoesNotExist()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        openMenu("Beállítások")
        compose.onNodeWithText("Pl@ntNet-kapcsolat").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("test-only-not-a-real-key")
        compose.onNodeWithText("Vissza").performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        openMenu("Beállítások")
        compose.onNodeWithText("API-kulcs elfelejtése").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10000) { compose.onAllNodes(hasText("API-kulcs elfelejtése") and isNotEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun menuListsHistoryBeforeSettings() {
        compose.onNodeWithText("Menü").performClick()
        val history = compose.onNode(hasText("Előzmények") and hasAnyAncestor(isPopup())).fetchSemanticsNode()
        val settings = compose.onNode(hasText("Beállítások") and hasAnyAncestor(isPopup())).fetchSemanticsNode()
        org.junit.Assert.assertTrue(history.boundsInRoot.top < settings.boundsInRoot.top)
    }

    @Test fun storedPhotoAppearsInHistoryDetails() {
        val image = File(compose.activity.cacheDir, "history-fixture.jpg")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val store = HistoryStore(historyFile)
        store.add(store.load().first(), image)
        image.delete()
        compose.activityRule.scenario.onActivity { ViewModelProvider(it)[PlantViewModel::class.java].reloadHistory() }
        openMenu("Előzmények")
        awaitHistory()
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithText("Az utolsó 100 sikeres felismerést ezen a készüléken tároljuk, a legújabbal kezdve. A felismerésekhez tartozó fotókat is megőrizzük. Az API-kulcs nem kerül az előzményekbe.").assertDoesNotExist()
        compose.onNodeWithText("Részletek elrejtése").assertDoesNotExist()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("A mentett növény bélyegképe").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("A mentett növény bélyegképe").performScrollTo().assertIsDisplayed().assertHeightIsEqualTo(144.dp)
        compose.onNodeWithText("Részletek megjelenítése").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("A korábban felismert növény képe").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("A korábban felismert növény képe").performScrollTo().assertIsDisplayed().assertHeightIsEqualTo(320.dp).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("A mentett növény teljes felbontású képe").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("full-photo-viewer").assertIsDisplayed()
        compose.onNodeWithText("Nagyítás").performClick()
        compose.onNodeWithText("150%").assertIsDisplayed()
        compose.onNodeWithText("Alaphelyzet").performClick()
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("Bezárás").performClick()
        compose.onNodeWithTag("full-photo-viewer").assertDoesNotExist()
        compose.onNodeWithText("Részletek elrejtése").assertExists()
    }

    @Test fun preparedPhotoEnablesRecognitionAndMissingKeyIsExplainedOnTap() {
        val missingKey = "A felismeréshez add meg az API-kulcsot a Menü → Beállítások oldalon."
        compose.onNodeWithText("Beállítások").assertDoesNotExist()
        compose.onNodeWithText(missingKey).assertDoesNotExist()
        compose.onNodeWithText("Növény felismerése").performScrollTo().assertIsNotEnabled()
        compose.activityRule.scenario.onActivity { activity ->
            val directory = File(activity.cacheDir, "photos").apply { mkdirs() }
            val photo = File(directory, "regression-camera.jpg")
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            ViewModelProvider(activity)[PlantViewModel::class.java].acceptPhoto(photo)
        }
        compose.waitUntil(10000) {
            compose.onAllNodes(hasText("Növény felismerése") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Növény felismerése").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("API-kulcs szükséges").assertIsDisplayed()
        compose.onNodeWithText(missingKey).assertIsDisplayed()
        compose.onNodeWithText("Bezárás").performClick()
        compose.onNodeWithText(missingKey).assertDoesNotExist()
        compose.onNodeWithText("Beállítások").assertDoesNotExist()
        openMenu("Beállítások")
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("test-only-not-a-real-key")
        compose.onNodeWithText("Vissza").performClick()
        compose.onNodeWithText("Növény felismerése").performScrollTo().assertIsEnabled()
        compose.onNodeWithContentDescription("A lefotózott növény").assertExists()
    }

    @Test fun historyReloadsShowsDetailsAndRequiresConfirmationToClear() {
        openMenu("Előzmények")
        awaitHistory()
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithText("Százszorszép").assertIsDisplayed()
        compose.onNodeWithText("Részletek megjelenítése").performScrollTo().performClick()
        compose.onNodeWithText("Mentett növényleírás").assertExists()
        compose.onNodeWithText("Egyezési pontszám: 95,0%").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Összes előzmény törlése"))
        compose.onNodeWithText("Összes előzmény törlése").performClick()
        compose.onNodeWithText("Mégse").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithText("Részletek elrejtése").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Összes előzmény törlése"))
        compose.onNodeWithText("Összes előzmény törlése").performClick()
        compose.onNodeWithText("Törlés").performClick()
        awaitHistory()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Még nincsenek felismerési előzmények. Fotózz le és ismertess fel egy növényt!"))
        compose.activityRule.scenario.recreate()
        openMenu("Előzmények")
        awaitHistory()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Még nincsenek felismerési előzmények. Fotózz le és ismertess fel egy növényt!"))
        compose.onNodeWithText("Még nincsenek felismerési előzmények. Fotózz le és ismertess fel egy növényt!").assertIsDisplayed()
        compose.onNodeWithText("Százszorszép").assertDoesNotExist()
    }
    @Test fun searchFindsEnglishAndLatinAliasesAndDeletesOnlySelectedEntry() {
        val store = HistoryStore(historyFile)
        val original = store.load().first()
        store.add(original.copy(results = original.results.map { it.copy(match = it.match.copy(
            alternativeNames = listOf("Common daisy", "Százszorszép"), namesEnriched = true)) }))
        store.add(HistoryEntry("rose-entry", 1700000001000, listOf(PlantResult(
            PlantMatch("Rosa canina", "Gyepűrózsa", "Rosaceae", 0.9, listOf("Dog rose"), true), null))))
        compose.activityRule.scenario.onActivity { ViewModelProvider(it)[PlantViewModel::class.java].reloadHistory() }
        openMenu("Előzmények"); awaitHistory()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("DAISY")
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithText("Százszorszép").assertIsDisplayed()
        compose.onNodeWithText("Gyepűrózsa").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("BELLIS")
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithTag("delete-history-test-entry").performScrollTo().performClick()
        compose.onNodeWithText("Mégse").performClick()
        org.junit.Assert.assertEquals(2, store.load().size)
        compose.onNodeWithTag("delete-history-test-entry").performClick()
        compose.onNodeWithText("Törlés").performClick(); awaitHistory()
        org.junit.Assert.assertEquals(listOf("rose-entry"), store.load().map { it.id })
        compose.onNodeWithContentDescription("Keresés törlése").performScrollTo().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("rose-entry")
        compose.onNodeWithText("Gyepűrózsa").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("gyepurozsa")
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("rose-entry")
        compose.onNodeWithText("Gyepűrózsa").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("nincs ilyen")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nincs a keresésnek megfelelő előzmény."))
        compose.onNodeWithText("Nincs a keresésnek megfelelő előzmény.").assertIsDisplayed()
    }

}
