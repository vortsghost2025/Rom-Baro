package com.rombaro.tv.ui.phone

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rombaro.tv.data.repo.FavoritesRepository
import com.rombaro.tv.data.repo.PlaylistRepository
import com.rombaro.tv.domain.ChannelWithNow
import com.rombaro.tv.domain.Playlist
import com.rombaro.tv.player.PlayerActivity
import com.rombaro.tv.ui.settings.PlaylistSetupActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneRootScreen(vm: PhoneViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val playlists by vm.playlists.collectAsState()
    val channelsNow by vm.channelsNow.collectAsState()
    val favoriteIds by vm.favoriteStreamIds.collectAsState()

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
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No playlists yet")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        context.startActivity(Intent(context, PlaylistSetupActivity::class.java))
                    }) { Text("Add your first playlist") }
                }
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(channelsNow, key = { it.channel.id }) { cwn ->
                    val isFav = cwn.channel.streamId in favoriteIds
                    ListItem(
                        headlineContent = { Text(cwn.channel.name) },
                        supportingContent = {
                            Column {
                                if (cwn.now != null) {
                                    Text(
                                        text = "Now: ${cwn.now.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (cwn.next != null) {
                                        Text(
                                            text = "Next: ${cwn.next.title}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        )
                                    }
                                } else {
                                    cwn.channel.category?.let { Text(it) }
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                                tint = if (isFav) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.clickable { vm.toggleFavorite(cwn) },
                            )
                        },
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
    private val favoritesRepo: FavoritesRepository,
) : ViewModel() {
    private val _selectedPlaylist = MutableStateFlow<Long?>(null)

    val playlists: StateFlow<List<Playlist>> = repo.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val channelsNow: StateFlow<List<ChannelWithNow>> = _selectedPlaylist
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeChannelsWithNow(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteStreamIds: StateFlow<Set<String>> = _selectedPlaylist
        .flatMapLatest { id ->
            if (id == null) flowOf(emptySet())
            else favoritesRepo.observe(id).map { favs -> favs.mapTo(mutableSetOf()) { it.streamId } }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavorite(cwn: ChannelWithNow) {
        viewModelScope.launch {
            favoritesRepo.toggle(cwn.channel.playlistId, cwn.channel.streamId)
        }
    }

    init {
        viewModelScope.launch {
            playlists.collect { list ->
                if (_selectedPlaylist.value == null) _selectedPlaylist.value = list.firstOrNull()?.id
            }
        }
    }
}
