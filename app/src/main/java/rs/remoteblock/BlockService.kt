package rs.remoteblock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
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

    // Poslednja "prava" aplikacija u prvom planu i ona zakljucana tokom blokade
    private var currentPkg: String? = null   // poslednja prava aplikacija (za zakljucavanje)
    private var foregroundPkg: String? = null // sta je trenutno u prvom planu
    private val handler = Handler(Looper.getMainLooper())
    private var lockedPkg: String?
        get() = prefs.lockedPkg
        set(v) { prefs.lockedPkg = v }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        instance = this
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!::prefs.isInitialized) prefs = Prefs(this)

        if (Prefs.recording) return false

        // Detekcija kombinacije samo na pritisak (ne na ponavljanje drzanja)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && trackCombo(event.keyCode)) {
            val nowBlocked = !prefs.blocked
            prefs.blocked = nowBlocked
            lockedPkg = if (nowBlocked && prefs.lockApp) currentPkg else null
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::prefs.isInitialized) prefs = Prefs(this)
        val pkg = event.packageName?.toString() ?: return
        if (pkg in IGNORED_PKGS) return
        // Nas ekran nije "prava" aplikacija: ne pamti se, ali je izlazak u njega izlazak iz zakljucane
        val own = pkg == packageName
        foregroundPkg = pkg

        if (prefs.blocked && prefs.lockApp) {
            if (lockedPkg == null) lockedPkg = currentPkg ?: if (own) null else pkg
            val locked = lockedPkg
            if (locked != null && pkg != locked) {
                // Izaslo se iz zakljucane aplikacije (npr. HOME) - vrati je, uz ponovne pokusaje
                handler.removeCallbacksAndMessages(null)
                for (delay in longArrayOf(0, 300, 800, 1600, 3000)) {
                    handler.postDelayed({ if (prefs.blocked && foregroundPkg != locked) relaunch(locked) }, delay)
                }
                return
            }
        } else {
            lockedPkg = null
        }
        if (!own) currentPkg = pkg
    }

    private fun relaunch(pkg: String) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        try { startActivity(intent) } catch (_: Exception) {}
    }
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile var instance: BlockService? = null
        private const val COMBO_GAP_MS = 3000L
        private val IGNORED_PKGS = setOf("com.android.systemui", "android")
        private val VOLUME_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE
        )
    }
}
