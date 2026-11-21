package com.vaulttune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaulttune.di.appModule
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.innertube.NetworkGatekeeperControl
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YouTubeLocale
import org.koin.compose.koinInject
import org.koin.core.context.startKoin

fun main() {
    val koin = startKoin { modules(appModule) }.koin
    NetworkGatekeeperControl.allowConnections()

    val settings = koin.get<AppSettings>()
    YouTube.locale = YouTubeLocale(gl = settings.localeGl, hl = settings.localeHl)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "VaultTune",
            state = rememberWindowState(width = 960.dp, height = 640.dp),
        ) {
            App()
        }
    }
}

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SearchScreen()
        }
    }
}

@Composable
fun SearchScreen() {
    val playback = koinInject<PlaybackService>()
    val audioPlayer = koinInject<AudioPlayer>()
    val settings = koinInject<AppSettings>()

    var query by remember { mutableStateOf(settings.lastQuery) }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resolvingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun play(song: SongItem) {
        scope.launch {
            resolvingId = song.id
            error = null
            try {
                val stream = playback.resolve(song)
                audioPlayer.play(stream.url)
            } catch (t: Throwable) {
                error = "Playback failed: ${t.message ?: t}"
            } finally {
                resolvingId = null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text("VaultTune", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search YouTube Music") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val q = query.trim()
                    if (q.isEmpty()) return@Button
                    scope.launch {
                        loading = true
                        error = null
                        results = emptyList()
                        try {
                            results = playback.searchSongs(q)
                            settings.lastQuery = q
                        } catch (t: Throwable) {
                            error = t.message ?: t.toString()
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = query.isNotBlank() && !loading,
            ) {
                Text("Search")
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            results.isEmpty() -> Text("No results yet.")
            else -> LazyColumn {
                items(results) { song ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (resolvingId == song.id) {
                            CircularProgressIndicator(Modifier.padding(horizontal = 16.dp))
                        } else {
                            TextButton(onClick = { play(song) }) { Text("Play") }
                        }
                    }
                }
            }
        }
    }
}
