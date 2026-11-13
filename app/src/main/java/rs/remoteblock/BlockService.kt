package rs.remoteblock

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Presrece tastere daljinskog. Dok je blokada ukljucena, svi tasteri se "gutaju"
 * (vracamo true), osim tastera za zvuk ako je to dozvoljeno. Kombinacija tastera
 * (sekvenca) ukljucuje/iskljucuje blokadu - prepoznaje se i dok je blokada aktivna.
 */
class BlockService : AccessibilityService() {

    private lateinit var prefs: Prefs
    private val recent = ArrayList<Int>()
    private var lastKeyAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        instance = this
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!::prefs.isInitialized) prefs = Prefs(this)

        // Detekcija kombinacije samo na pritisak (ne na ponavljanje drzanja)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && trackCombo(event.keyCode)) {
            val nowBlocked = !prefs.blocked
            prefs.blocked = nowBlocked
            Toast.makeText(
                this,
                if (nowBlocked) "Daljinski BLOKIRAN" else "Daljinski ODBLOKIRAN",
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        if (!prefs.blocked) return false
        if (prefs.allowVolume && event.keyCode in VOLUME_KEYS) return false
        return true
    }

    /** Vraca true kad je upravo zavrsena cela kombinacija. */
    private fun trackCombo(keyCode: Int): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastKeyAt > COMBO_GAP_MS) recent.clear()
        lastKeyAt = now

        val combo = prefs.combo
        recent.add(keyCode)
        while (recent.size > combo.size) recent.removeAt(0)
        if (recent == combo) {
            recent.clear()
            return true
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile var instance: BlockService? = null
        private const val COMBO_GAP_MS = 3000L
        private val VOLUME_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE
        )
    }
}
