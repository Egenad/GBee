package es.atm.gbee.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityEmuBinding
import es.atm.gbee.modules.Emulator
import es.atm.gbee.modules.ROM
import es.atm.gbee.views.GameSurfaceView

class EmuActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEmuBinding
    private lateinit var gameSurfaceView: GameSurfaceView

    private val emulator = Emulator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEmuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gameSurfaceView = binding.gameSurface

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        emulator.run("/Users/angelterol/Documents/Git/Android/GBee/roms/GoldenSacra.gb")
    }
}