package com.widgeter.app

import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

/** Appearance (light/dark/system) and about. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val root = findViewById<android.view.View>(R.id.settings_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.settings_toolbar)
            .setNavigationOnClickListener { finish() }

        val group = findViewById<RadioGroup>(R.id.theme_group)
        group.check(
            when (Store.getThemeMode(this)) {
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.radio_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.radio_dark
                else -> R.id.radio_system
            }
        )
        group.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            Store.setThemeMode(this, mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
