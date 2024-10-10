package es.atm.gbee.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityEmuBinding
import es.atm.gbee.modules.Emulator
import es.atm.gbee.views.GameSurfaceView

const val ROM_URI_EXTRA = "ROM_URI"

class EmuActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEmuBinding
    private lateinit var gameSurfaceView: GameSurfaceView
    private lateinit var dbgButton: Button

    private val debugMode = true
    private var tilemap = true

    private val emulator = Emulator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEmuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameSurfaceView = binding.gameSurface
        dbgButton = binding.switchButton

        if(!debugMode){
            dbgButton.visibility = Button.GONE
        }

        dbgButton.setOnClickListener {
            gameSurfaceView.debugMode = tilemap
            tilemap = !tilemap
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val romUri: Uri? = intent.getStringExtra(ROM_URI_EXTRA)?.let { Uri.parse(it) }

        romUri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val romBytes = inputStream?.readBytes()
            inputStream?.close()

            emulator.run(romBytes)
        }
    }
}