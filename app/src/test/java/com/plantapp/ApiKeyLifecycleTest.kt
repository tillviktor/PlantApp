package com.plantapp

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.KeyGenerator

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiKeyLifecycleTest {
    @get:Rule val folder = TemporaryFolder()
    private val models = mutableListOf<ViewModelStore>()
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { models.forEach { it.clear() }; Dispatchers.resetMain() }
    private fun model(store: ApiKeyStore): PlantViewModel {
        val model = PlantViewModel(ApplicationProvider.getApplicationContext<Application>(), store)
        models += ViewModelStore().apply { put("test", model) }
        awaitKey(model)
        return model
    }
    private fun awaitKey(model: PlantViewModel) = runBlocking {
        withTimeout(10000) { while (model.keyBusy) delay(10) }
    }
    @Test fun savedKeyLoadsInNewApplicationModelAndForgetIsDurable() {
        val file = File(folder.root, "api-key.enc")
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        fun store() = ApiKeyStore(file) { key }
        val first = model(store())
        first.updateApiKey("test-only-persisted-key"); first.saveApiKey(); awaitKey(first)
        assertTrue(first.hasStoredApiKey)
        models.first().clear()
        val restarted = model(store())
        assertEquals("test-only-persisted-key", restarted.apiKey)
        restarted.forgetApiKey(); awaitKey(restarted)
        assertEquals("", model(store()).apiKey)
        assertFalse(file.exists())
    }
    @Test fun unreadableSavedKeyIsReportedAndCanBeForgotten() {
        val file = File(folder.root, "api-key.enc").apply { writeText("damaged encrypted file") }
        val original = file.readBytes()
        val model = model(ApiKeyStore(file) { error("Cannot decrypt") })
        assertEquals("", model.apiKey)
        assertNotNull(model.keyStatus)
        assertTrue(model.hasStoredApiKey)
        assertArrayEquals(original, file.readBytes())
        model.forgetApiKey(); awaitKey(model)
        assertFalse(model.hasStoredApiKey)
        assertFalse(file.exists())
    }
}
