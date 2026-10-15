package com.plantapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.app.Application
import android.graphics.Bitmap
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
        compose.waitUntil(10000) { compose.onAllNodesWithText("Összes előzmény törlése").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToKey("test-entry")
        compose.onNodeWithText("Részletek megjelenítése").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("A korábban felismert növény képe").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("A korábban felismert növény képe").performScrollTo().assertIsDisplayed()
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
        compose.waitUntil(10000) { compose.onAllNodesWithText("Összes előzmény törlése").fetchSemanticsNodes().isNotEmpty() }
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
        compose.waitUntil(10000) { compose.onAllNodesWithText("Még nincsenek felismerési előzmények. Fotózz le és ismertess fel egy növényt!").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        openMenu("Előzmények")
        compose.onNodeWithText("Még nincsenek felismerési előzmények. Fotózz le és ismertess fel egy növényt!").assertIsDisplayed()
        compose.onNodeWithText("Százszorszép").assertDoesNotExist()
    }
}
