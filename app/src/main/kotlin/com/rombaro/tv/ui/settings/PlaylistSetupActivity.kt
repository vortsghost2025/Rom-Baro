package com.rombaro.tv.ui.settings

import android.content.res.Configuration
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

private fun Modifier.tvVerticalDpadNavigation(
    isTv: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager,
): Modifier = onPreviewKeyEvent { event ->
    if (!isTv || event.type != KeyEventType.KeyDown) {
        return@onPreviewKeyEvent false
    }
    when (event.key) {
        Key.DirectionUp -> {
            focusManager.moveFocus(FocusDirection.Up)
            true
        }
        Key.DirectionDown -> {
            focusManager.moveFocus(FocusDirection.Down)
            true
        }
        else -> false
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

    val isTv =
        (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val nameFocus = remember { FocusRequester() }
    val xtreamChipFocus = remember { FocusRequester() }
    val m3uChipFocus = remember { FocusRequester() }
    val m3uUrlFocus = remember { FocusRequester() }
    val serverFocus = remember { FocusRequester() }
    val userFocus = remember { FocusRequester() }
    val passFocus = remember { FocusRequester() }
    val xmltvFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }

    Scaffold(topBar = { TopAppBar(title = { Text("Add Playlist") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LaunchedEffect(Unit) {
                if (type == PlaylistType.XTREAM) {
                    xtreamChipFocus.requestFocus()
                } else {
                    m3uChipFocus.requestFocus()
                }
            }
            OutlinedTextField(
                name, { name = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(nameFocus)
                    .focusProperties {
                        down = if (type == PlaylistType.XTREAM) xtreamChipFocus else m3uChipFocus
                    }
                    .onFocusChanged { state ->
                        if (isTv && state.isFocused) keyboardController?.hide()
                    }
                    .tvVerticalDpadNavigation(isTv, focusManager)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TypeChip(
                    label = "Xtream Codes",
                    selected = type == PlaylistType.XTREAM,
                    focusRequester = xtreamChipFocus,
                    onClick = { type = PlaylistType.XTREAM },
                    upFocus = nameFocus,
                    downFocus = if (type == PlaylistType.XTREAM) serverFocus else m3uUrlFocus,
                    rightFocus = m3uChipFocus,
                )
                TypeChip(
                    label = "M3U URL",
                    selected = type == PlaylistType.M3U,
                    focusRequester = m3uChipFocus,
                    onClick = { type = PlaylistType.M3U },
                    upFocus = nameFocus,
                    leftFocus = xtreamChipFocus,
                    downFocus = if (type == PlaylistType.XTREAM) serverFocus else m3uUrlFocus,
                )
            }

            if (type == PlaylistType.XTREAM) {
                OutlinedTextField(
                    server, { server = it },
                    label = { Text("Server URL (http://host:port)") },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(serverFocus)
                        .focusProperties {
                            up = xtreamChipFocus
                            down = userFocus
                        }
                        .onFocusChanged { state ->
                            if (isTv && state.isFocused) keyboardController?.hide()
                        }
                        .tvVerticalDpadNavigation(isTv, focusManager)
                )
                OutlinedTextField(
                    user, { user = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(userFocus)
                        .focusProperties {
                            up = serverFocus
                            down = passFocus
                        }
                        .onFocusChanged { state ->
                            if (isTv && state.isFocused) keyboardController?.hide()
                        }
                        .tvVerticalDpadNavigation(isTv, focusManager)
                )
                OutlinedTextField(
                    pass, { pass = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(passFocus)
                        .focusProperties {
                            up = userFocus
                            down = xmltvFocus
                        }
                        .onFocusChanged { state ->
                            if (isTv && state.isFocused) keyboardController?.hide()
                        }
                        .tvVerticalDpadNavigation(isTv, focusManager)
                )
            } else {
OutlinedTextField(
                    m3u, { m3u = it },
                    label = { Text("M3U URL") },
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(m3uUrlFocus)
                        .focusProperties {
                            up = m3uChipFocus
                            down = xmltvFocus
                        }
                        .onFocusChanged { state ->
                            if (isTv && state.isFocused) keyboardController?.hide()
                        }
                        .tvVerticalDpadNavigation(isTv, focusManager)
                )
            }

            OutlinedTextField(
                xmltv, { xmltv = it },
                label = { Text("XMLTV EPG URL (optional)") },
                modifier = Modifier.fillMaxWidth()
                    .focusRequester(xmltvFocus)
                    .focusProperties {
                        up = if (type == PlaylistType.XTREAM) passFocus else m3uUrlFocus
                        down = saveFocus
                    }
                    .onFocusChanged { state ->
                        if (isTv && state.isFocused) keyboardController?.hide()
                    }
                    .tvVerticalDpadNavigation(isTv, focusManager)
            )

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
                    .focusRequester(saveFocus)
                    .focusProperties {
                        up = xmltvFocus
                    }
            ) { Text(if (saving) "Saving…" else "Save & Import") }
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    upFocus: FocusRequester,
    downFocus: FocusRequester,
    leftFocus: FocusRequester? = null,
    rightFocus: FocusRequester? = null,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = Modifier.focusRequester(focusRequester)
            .focusProperties {
                up = upFocus
                down = downFocus
                leftFocus?.let { left = it }
                rightFocus?.let { right = it }
            },
    ) {
        Text(label)
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