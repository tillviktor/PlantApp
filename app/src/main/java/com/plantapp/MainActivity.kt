package com.plantapp

import android.os.Bundle
import android.content.ActivityNotFoundException
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF246748), background = Color(0xFFF6F8F2), surface = Color(0xFFF6F8F2))) {
                Surface(Modifier.fillMaxSize()) { PlantScreen() }
            }
        }
    }
}

@Composable
fun PlantScreen(model: PlantViewModel = viewModel()) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingPath?.let { path -> val file = File(path); if (success) model.acceptPhoto(file) else file.delete() }
        pendingPath = null
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PlantApp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Meet your green neighbors.", style = MaterialTheme.typography.headlineLarge)
        Text("Photograph a leaf or flower to discover what’s growing around you.", style = MaterialTheme.typography.bodyLarge)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Connect Pl@ntNet", style = MaterialTheme.typography.titleMedium)
                Text("Enter your personal API key. It stays in memory for this session and is sent only to Pl@ntNet.")
                OutlinedTextField(value = model.apiKey, onValueChange = { model.apiKey = it }, label = { Text("Pl@ntNet API key") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
                TextButton(onClick = { links.openUri("https://my.plantnet.org/") }) { Text("Get a free API key") }
            }
        }
        model.preview?.let { Image(it.asImageBitmap(), contentDescription = "Your plant photo", modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit) }
        OutlinedButton(enabled = !model.busy && pendingPath == null, modifier = Modifier.fillMaxWidth(), onClick = {
            val directory = File(context.cacheDir, "photos").apply { mkdirs() }
            // Only clean old, abandoned camera images; preserve the current photo.
            directory.listFiles()?.filter { it != model.photo && System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
            val file = File.createTempFile("camera-", ".jpg", directory)
            pendingPath = file.path
            try { camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.photos", file)) }
            catch (_: ActivityNotFoundException) { pendingPath = null; file.delete(); model.reportCameraError() }
        }) { Text(if (model.photo == null) "Take a plant photo" else "Take another photo") }
        Text("Identifying uploads this photo to Pl@ntNet. Location metadata is removed before upload.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = model::identify, enabled = model.photo != null && model.apiKey.isNotBlank() && !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Identify plant") }
        if (model.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Preparing your photo or finding a match…") }
        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        model.results.forEachIndexed { index, result ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (index == 0) "BEST MATCH" else "POSSIBLE MATCH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(result.match.commonName, style = MaterialTheme.typography.headlineSmall)
                    Text(result.match.scientificName, fontStyle = FontStyle.Italic)
                    Text("Match score: ${String.format(Locale.US, "%.1f", result.match.confidence * 100)}%")
                    if (result.match.family.isNotBlank()) Text("Family: ${result.match.family}")
                    if (index == 0) Text(result.summary ?: "A description is unavailable. Explore the reference below for more information.")
                    TextButton(onClick = { links.openUri(result.match.wikipediaUrl) }) { Text("Read on Wikipedia ↗") }
                }
            }
        }
        if (model.results.isNotEmpty()) Text("Identification is a suggestion, not a guarantee. Never use it to decide whether a plant is safe to eat or use medicinally.", style = MaterialTheme.typography.bodySmall)
        Text("Identification by Pl@ntNet · Descriptions from Wikipedia (CC BY-SA).", style = MaterialTheme.typography.bodySmall)
    }
}
