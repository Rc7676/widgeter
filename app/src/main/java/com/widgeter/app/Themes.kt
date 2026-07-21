package com.widgeter.app

import android.content.Context

/** Maps the saved accent-theme index to a theme style resource. */
object Themes {
    val styles = intArrayOf(
        R.style.Theme_Widgeter,        // 0 Aqua
        R.style.Theme_Widgeter_Red,    // 1 Red
        R.style.Theme_Widgeter_Blue,   // 2 Blue
        R.style.Theme_Widgeter_Yellow, // 3 Yellow
        R.style.Theme_Widgeter_Black,  // 4 Black
        R.style.Theme_Widgeter_Sunset  // 5 Sunset
    )

    /** Primary color of each theme, for the settings swatches. */
    val swatchColors = intArrayOf(
        R.color.brand,
        R.color.red_primary,
        R.color.blue_primary,
        R.color.yellow_primary,
        R.color.black_primary,
        R.color.sunset_primary
    )

    fun styleFor(context: Context): Int =
        styles[Store.getColorTheme(context).coerceIn(0, styles.size - 1)]
}
