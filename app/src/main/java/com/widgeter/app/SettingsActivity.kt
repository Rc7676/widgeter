package com.widgeter.app

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

/** Appearance (light/dark), accent color themes, and about. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val root = findViewById<View>(R.id.settings_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        findViewById<MaterialToolbar>(R.id.settings_toolbar)
            .setNavigationOnClickListener { finish() }

        setupAppearance()
        setupColors()
    }

    private fun setupAppearance() {
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

    private fun setupColors() {
        val container = findViewById<LinearLayout>(R.id.color_swatches)
        container.removeAllViews()
        val current = Store.getColorTheme(this)
        val names = resources.getStringArray(R.array.color_names)
        val ring = ContextCompat.getColor(this, R.color.on_surface)

        for (i in Themes.swatchColors.indices) {
            val color = ContextCompat.getColor(this, Themes.swatchColors[i])
            val swatch = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (i == current) setStroke(dp(3), ring)
                }
                contentDescription = names.getOrElse(i) { "" }
                setOnClickListener {
                    Store.setColorTheme(this@SettingsActivity, i)
                    recreate()
                }
            }
            val lp = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(12) }
            val wrap = FrameLayout(this).apply { layoutParams = lp }
            wrap.addView(swatch, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER))
            container.addView(wrap)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
