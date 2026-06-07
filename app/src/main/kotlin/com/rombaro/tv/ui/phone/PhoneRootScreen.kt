package com.rombaro.tv.ui.phone

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rombaro.tv.data.repo.PlaylistRepository
import com.rombaro.tv.domain.Channel
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.player.PlayerActivity
import com.rombaro.tv.ui.settings.PlaylistSetupActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneRootScreen(vm: PhoneViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val playlists by vm.playlists.collectAsState()
    val channels by vm.channels.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rom Baro") },
                actions = {
                    TextButton(onClick = {
                        context.startActivity(Intent(context, PlaylistSetupActivity::class.java))
                    }) { Text("Add Playlist") }
                }
            )
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("No playlists yet")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        context.startActivity(Intent(context, PlaylistSetupActivity::class.java))
                    }) { Text("Add your first playlist") }
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(channels, key = { it.id }) { ch ->
                    ListItem(
                        headlineContent = { Text(ch.name) },
                        supportingContent = { ch.category?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val repo: PlaylistRepository,
) : ViewModel() {
    private val _selectedPlaylist = MutableStateFlow<Long?>(null)

    val playlists: StateFlow<List<Playlist>> = repo.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val channels: StateFlow<List<Channel>> = _selectedPlaylist
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeChannels(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            playlists.collect { list ->
                if (_selectedPlaylist.value == null) _selectedPlaylist.value = list.firstOrNull()?.id
            }
        }
    }
}
