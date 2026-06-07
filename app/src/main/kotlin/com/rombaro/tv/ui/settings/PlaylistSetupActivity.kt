package com.rombaro.tv.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rombaro.tv.data.repo.PlaylistRepository
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.domain.PlaylistType
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaylistSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { SetupScreen(onSaved = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupScreen(vm: SetupViewModel = hiltViewModel(), onSaved: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("My IPTV") }
    var type by rememberSaveable { mutableStateOf(PlaylistType.XTREAM) }
    var server by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var m3u by rememberSaveable { mutableStateOf("") }
    var xmltv by rememberSaveable { mutableStateOf("") }
    val saving by vm.saving.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Add Playlist") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                FilterChip(type == PlaylistType.XTREAM, { type = PlaylistType.XTREAM }, label = { Text("Xtream Codes") })
                Spacer(Modifier.width(8.dp))
                FilterChip(type == PlaylistType.M3U, { type = PlaylistType.M3U }, label = { Text("M3U URL") })
            }

            if (type == PlaylistType.XTREAM) {
                OutlinedTextField(server, { server = it }, label = { Text("Server URL (http://host:port)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedTextField(m3u, { m3u = it }, label = { Text("M3U URL") }, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(xmltv, { xmltv = it }, label = { Text("XMLTV EPG URL (optional)") }, modifier = Modifier.fillMaxWidth())

            Button(
                enabled = !saving,
                onClick = {
                    vm.save(
                        Playlist(
                            name = name.ifBlank { "My IPTV" },
                            type = type,
                            serverUrl = if (type == PlaylistType.XTREAM) server else m3u,
                            username = user.ifBlank { null },
                            password = pass.ifBlank { null },
                            m3uUrl = m3u.ifBlank { null },
                            xmltvUrl = xmltv.ifBlank { null },
                        ),
                        onDone = onSaved,
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Saving…" else "Save & Import") }
        }
    }
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: PlaylistRepository,
) : ViewModel() {
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun save(p: Playlist, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            try {
                repo.savePlaylist(p)
                onDone()
            } finally {
                _saving.value = false
            }
        }
    }
}
