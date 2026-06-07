package com.rombaro.tv.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rombaro.tv.R
import com.rombaro.tv.ui.browse.MainBrowseFragment
import com.rombaro.tv.ui.phone.PhoneRootScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single entry that dispatches to the right top-level UI for the form factor.
 *
 *  - Android TV / Fire TV  → MainBrowseFragment (Leanback)
 *  - Phone / tablet        → Compose PhoneRootScreen
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTelevision()) {
            setContentView(R.layout.activity_main_tv)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_browse_fragment, MainBrowseFragment())
                    .commit()
            }
        } else {
            // Wrap in a ComponentActivity-style setContent by using ComposeView from XML
            setContentView(R.layout.activity_main_phone)
            val composeView = findViewById<androidx.compose.ui.platform.ComposeView>(R.id.compose_root)
            composeView.setContent { PhoneRootScreen() }
        }
    }

    private fun isTelevision(): Boolean {
        val uiMode = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
