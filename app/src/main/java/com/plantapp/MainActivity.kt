package com.plantapp

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF246748), background = Color(0xFFF6F8F2), surface = Color(0xFFF6F8F2))) {
                // Use this Activity's Application, avoiding a process-wide cached factory.
                val factory = remember { ViewModelProvider.AndroidViewModelFactory(application) }
                val model: PlantViewModel = viewModel(factory = factory)
                Surface(Modifier.fillMaxSize()) { PlantScreen(model) }
            }
        }
    }
}

private enum class Page { HOME, SETTINGS, HISTORY }

@Composable
fun PlantScreen(model: PlantViewModel) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    var pageName by rememberSaveable { mutableStateOf(Page.HOME.name) }
    val page = Page.valueOf(pageName)
    var menuOpen by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf(false) }
    var keyRequired by rememberSaveable { mutableStateOf(false) }
    val openLink: (String) -> Unit = { url ->
        try { links.openUri(url) } catch (_: ActivityNotFoundException) { linkError = true }
        catch (_: IllegalArgumentException) { linkError = true }
    }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingPath?.let { path -> val file = File(path); if (success) model.acceptPhoto(file) else file.delete() }
        pendingPath = null
    }
    BackHandler(enabled = page != Page.HOME) { pageName = Page.HOME.name }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (page == Page.HOME) Text(stringResource(R.string.app_name), modifier = Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.titleLarge)
            else TextButton(onClick = { pageName = Page.HOME.name }) { Text(stringResource(R.string.back)) }
            Box {
                TextButton(onClick = { menuOpen = true }) { Text(stringResource(R.string.menu)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    listOf(Page.HOME to R.string.home, Page.HISTORY to R.string.history, Page.SETTINGS to R.string.settings).forEach { (target, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { pageName = target.name; menuOpen = false })
                    }
                }
            }
        }
        when (page) {
            Page.SETTINGS -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.connect), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.key_info))
                OutlinedTextField(value = model.apiKey, onValueChange = model::updateApiKey, label = { Text(stringResource(R.string.api_key)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), enabled = !model.busy && !model.keyBusy)
                TextButton(onClick = { openLink("https://my.plantnet.org/") }) { Text(stringResource(R.string.get_key)) }
                Button(onClick = model::saveApiKey, enabled = model.apiKey.isNotBlank() && !model.busy && !model.keyBusy) { Text(stringResource(R.string.save_key)) }
                OutlinedButton(onClick = model::forgetApiKey, enabled = (model.apiKey.isNotEmpty() || model.hasStoredApiKey) && !model.busy && !model.keyBusy) { Text(stringResource(R.string.forget_key)) }
                if (model.keyBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                model.keyStatus?.let { Text(it) }
            }
            Page.HISTORY -> HistoryScreen(model, openLink, Modifier.weight(1f))
            Page.HOME -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.headline), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyLarge)
                model.preview?.let { Image(it.asImageBitmap(), contentDescription = stringResource(R.string.photo_description), modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit) }
                OutlinedButton(enabled = !model.busy && pendingPath == null, modifier = Modifier.fillMaxWidth(), onClick = {
                    val directory = File(context.cacheDir, "photos").apply { mkdirs() }
                    directory.listFiles()?.filter { it != model.photo && System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
                    val file = File.createTempFile("camera-", ".jpg", directory)
                    pendingPath = file.path
                    try { camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.photos", file)) }
                    catch (_: ActivityNotFoundException) { pendingPath = null; file.delete(); model.reportCameraError() }
                }) { Text(stringResource(if (model.photo == null) R.string.take_photo else R.string.retake_photo)) }
                Text(stringResource(R.string.upload_notice), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { if (model.apiKey.isBlank()) keyRequired = true else model.identify() }, enabled = model.photo != null && !model.busy && !model.keyBusy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.identify)) }
                if (model.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.working)) }
                model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                model.historySaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                model.results.forEachIndexed { index, result -> ResultCard(result, index, openLink) }
                if (model.results.isNotEmpty()) IdentificationNotice()
                Attribution()
            }
        }
    }
    if (keyRequired) AlertDialog(onDismissRequest = { keyRequired = false },
        title = { Text(stringResource(R.string.key_required_title)) },
        text = { Text(stringResource(R.string.key_missing)) },
        confirmButton = { TextButton(onClick = { keyRequired = false }) { Text(stringResource(R.string.close)) } })
    if (linkError) AlertDialog(onDismissRequest = { linkError = false }, text = { Text(stringResource(R.string.link_error)) },
        confirmButton = { TextButton(onClick = { linkError = false }) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun ResultCard(result: PlantResult, index: Int, openLink: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(if (index == 0) R.string.best_match else R.string.possible_match), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(result.match.commonName, style = MaterialTheme.typography.headlineSmall)
            Text(result.match.scientificName, fontStyle = FontStyle.Italic)
            Text(stringResource(R.string.score, String.format(Locale.forLanguageTag("hu-HU"), "%.1f", result.match.confidence * 100)))
            if (result.match.family.isNotBlank()) Text(stringResource(R.string.family, result.match.family))
            if (index == 0) {
                Text(result.summary ?: stringResource(R.string.no_description))
                Text(stringResource(R.string.wikipedia_language), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { openLink(result.match.wikipediaUrl) }) { Text(stringResource(R.string.wikipedia)) }
        }
    }
}

@Composable
private fun HistoryScreen(model: PlantViewModel, openLink: (String) -> Unit, modifier: Modifier = Modifier) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var fullPhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val clearSearchDescription = stringResource(R.string.clear_search)
    val filtered = remember(model.history, query) { searchHistory(model.history, query) }
    LaunchedEffect(query, model.history, model.historyNamesLoading) {
        if (query.isNotBlank()) model.enrichHistoryNames()
    }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text(stringResource(R.string.history_search)) },
                placeholder = { Text(stringResource(R.string.history_search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }, modifier = Modifier.semantics { contentDescription = clearSearchDescription }) { Text("×") } })
            Text(stringResource(R.string.history_search_info), style = MaterialTheme.typography.bodySmall)
            if (query.isNotBlank()) Text(stringResource(R.string.history_matches_count, filtered.size, model.history.size))
            if (model.historyNamesLoading) Text(stringResource(R.string.history_names_loading), style = MaterialTheme.typography.bodySmall)
            if (query.isNotBlank()) {
                model.historyNamesError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (!model.historyNamesLoading && model.history.any { entry -> entry.results.any { !it.match.namesEnriched } }) {
                    TextButton(onClick = model::retryHistoryNames) { Text(stringResource(R.string.retry_history_names)) }
                }
            }
        }
        if (model.historyBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.history_loading)) }
        model.historyError?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = model::reloadHistory, enabled = !model.historyBusy) { Text(stringResource(R.string.retry)) }
        } }
        if (model.history.isEmpty() && !model.historyBusy && model.historyError == null) item { Text(stringResource(R.string.history_empty)) }
        if (model.history.isNotEmpty() || model.historyError != null) item {
            OutlinedButton(onClick = { confirmClear = true }, enabled = !model.historyBusy && !model.busy) { Text(stringResource(R.string.clear_history)) }
        }
        if (model.history.isNotEmpty() && filtered.isEmpty() && !model.historyNamesLoading) item { Text(stringResource(R.string.history_no_matches)) }
        items(filtered, key = { it.id }) { entry ->
            var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("hu-HU")).format(Date(entry.recognizedAtMillis)), style = MaterialTheme.typography.labelLarge)
                    Text(entry.results.first().match.commonName, style = MaterialTheme.typography.titleLarge)
                    Text(entry.results.first().match.scientificName, fontStyle = FontStyle.Italic)
                    HistoryPhoto(model.historyPhoto(entry), expanded, onOpen = { fullPhotoId = entry.id })
                    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.hide_details else R.string.show_details)) }
                    TextButton(onClick = { pendingDeleteId = entry.id }, enabled = !model.historyBusy && !model.busy,
                        modifier = Modifier.testTag("delete-history-${entry.id}")) { Text(stringResource(R.string.delete_history_entry)) }
                    if (expanded) {
                        entry.results.forEachIndexed { index, result -> ResultCard(result, index, openLink) }
                    }
                }
            }
        }
        if (model.history.isNotEmpty()) item { IdentificationNotice() }
        item { Attribution() }
    }
    model.history.firstOrNull { it.id == fullPhotoId }?.let { entry ->
        FullPhotoViewer(model.historyPhoto(entry), onClose = { fullPhotoId = null })
    }
    model.history.firstOrNull { it.id == pendingDeleteId }?.let { entry ->
        AlertDialog(onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.confirm_delete_entry_title)) },
            text = { Text(stringResource(R.string.confirm_delete_entry_message, entry.results.first().match.commonName)) },
            confirmButton = { TextButton(onClick = { pendingDeleteId = null; model.deleteHistoryEntry(entry.id) }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.cancel)) } })
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.confirm_clear_title)) }, text = { Text(stringResource(R.string.confirm_clear_message)) },
        confirmButton = { TextButton(onClick = { confirmClear = false; model.clearHistory() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun IdentificationNotice() { Text(stringResource(R.string.identification_notice), style = MaterialTheme.typography.bodySmall) }
@Composable
private fun Attribution() { Text(stringResource(R.string.attribution), style = MaterialTheme.typography.bodySmall) }
