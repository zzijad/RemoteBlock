package rs.remoteblock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefs: Prefs
    private lateinit var serviceStatus: TextView
    private lateinit var blockStatus: TextView
    private lateinit var comboText: TextView
    private lateinit var btnRecord: Button

    private var recording = false
    private val recorded = ArrayList<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private val finishRecording = Runnable { stopRecording() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        serviceStatus = findViewById(R.id.serviceStatus)
        blockStatus = findViewById(R.id.blockStatus)
        comboText = findViewById(R.id.comboText)
        btnRecord = findViewById(R.id.btnRecord)

        findViewById<Button>(R.id.btnAccess).setOnClickListener { openAccessibility() }
        findViewById<Button>(R.id.btnToggle).setOnClickListener { prefs.blocked = !prefs.blocked }
        btnRecord.setOnClickListener { startRecording() }
        findViewById<CheckBox>(R.id.chkVolume).apply {
            isChecked = prefs.allowVolume
            setOnCheckedChangeListener { _, c -> prefs.allowVolume = c }
        }
        findViewById<TextView>(R.id.hint).text =
            "Kad je blokada uključena, daljinski ne radi — otkucaj kombinaciju da je isključiš.\n" +
            "Taster HOME Android obično ne dozvoljava da se presretne."
    }

    override fun onResume() {
        super.onResume()
        prefs.sp.registerOnSharedPreferenceChangeListener(this)
        refresh()
    }

    override fun onPause() {
        prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
        stopRecording(save = false)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) = refresh()

    private fun refresh() {
        val on = isServiceEnabled()
        serviceStatus.text = if (on) "Servis: UKLJUČEN" else "Servis: ISKLJUČEN (uključi ga u Pristupačnosti)"
        serviceStatus.setTextColor(if (on) 0xFF66BB6A.toInt() else 0xFFEF5350.toInt())
        blockStatus.text = if (prefs.blocked) "Blokada: AKTIVNA" else "Blokada: neaktivna"
        blockStatus.setTextColor(if (prefs.blocked) 0xFFEF5350.toInt() else 0xFFFFFFFF.toInt())
        comboText.text = "Kombinacija: " + Prefs.describe(prefs.combo)
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = ComponentName(this, BlockService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, true) }
    }

    private fun openAccessibility() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Podešavanja nisu dostupna. Koristi adb (vidi README).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- Snimanje nove kombinacije ---
    private fun startRecording() {
        recording = true
        recorded.clear()
        btnRecord.text = "Pritiskaj tastere... (čeka se 3 s pauze)"
        comboText.text = "Snimanje: pritisni 3–8 tastera"
    }

    private fun stopRecording(save: Boolean = true) {
        handler.removeCallbacks(finishRecording)
        if (!recording) return
        recording = false
        btnRecord.text = "Snimi novu kombinaciju"
        if (save) {
            if (recorded.size >= 3) {
                prefs.combo = recorded.toList()
                Toast.makeText(this, "Kombinacija sačuvana", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Najmanje 3 tastera — nije sačuvano", Toast.LENGTH_LONG).show()
            }
        }
        refresh()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (recording) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                recorded.add(event.keyCode)
                comboText.text = "Snimanje: " + Prefs.describe(recorded)
                handler.removeCallbacks(finishRecording)
                if (recorded.size >= 8) stopRecording() else handler.postDelayed(finishRecording, 3000)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
