package rs.remoteblock

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent

class Prefs(context: Context) {
    val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("remoteblock", Context.MODE_PRIVATE)

    var blocked: Boolean
        get() = sp.getBoolean(KEY_BLOCKED, false)
        set(v) = sp.edit().putBoolean(KEY_BLOCKED, v).apply()

    var allowVolume: Boolean
        get() = sp.getBoolean(KEY_VOLUME, true)
        set(v) = sp.edit().putBoolean(KEY_VOLUME, v).apply()

    var lockApp: Boolean
        get() = sp.getBoolean(KEY_LOCK, true)
        set(v) = sp.edit().putBoolean(KEY_LOCK, v).apply()

    var lockedPkg: String?
        get() = sp.getString(KEY_LOCKED_PKG, null)
        set(v) = sp.edit().putString(KEY_LOCKED_PKG, v).apply()

    var combo: List<Int>
        get() = sp.getString(KEY_COMBO, null)
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size >= 3 }
            ?: DEFAULT_COMBO
        set(v) = sp.edit().putString(KEY_COMBO, v.joinToString(",")).apply()

    companion object {
        const val KEY_BLOCKED = "blocked"
        const val KEY_VOLUME = "allow_volume"
        const val KEY_LOCK = "lock_app"
        const val KEY_LOCKED_PKG = "locked_pkg"
        @Volatile var recording = false
        const val KEY_COMBO = "combo"

        // Gore, Gore, Dole, Dole, OK
        val DEFAULT_COMBO = listOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER
        )

        fun describe(combo: List<Int>) =
            combo.joinToString(" → ") { KeyEvent.keyCodeToString(it).removePrefix("KEYCODE_") }
    }
}
