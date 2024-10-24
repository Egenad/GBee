package es.atm.gbee.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityEmuBinding
import es.atm.gbee.modules.A_BUTTON
import es.atm.gbee.modules.B_BUTTON
import es.atm.gbee.modules.DOWN_DPAD
import es.atm.gbee.modules.Emulator
import es.atm.gbee.modules.IO
import es.atm.gbee.modules.LEFT_DPAD
import es.atm.gbee.modules.RIGHT_DPAD
import es.atm.gbee.modules.SELECT_BUTTON
import es.atm.gbee.modules.START_BUTTON
import es.atm.gbee.modules.UP_DPAD
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

        configureButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        obtainRom()
    }


    private fun configureButtons(){
        setButton(binding.dpadUp, UP_DPAD)
        setButton(binding.dpadDown, DOWN_DPAD)
        setButton(binding.dpadLeft, LEFT_DPAD)
        setButton(binding.dpadRight, RIGHT_DPAD)
        setButton(binding.buttonA, A_BUTTON)
        setButton(binding.buttonB, B_BUTTON)
        setButton(binding.buttonSelect, SELECT_BUTTON)
        setButton(binding.buttonStart, START_BUTTON)

        dbgButton = binding.switchButton

        if(debugMode){
            dbgButton.setOnClickListener {
                gameSurfaceView.debugMode = tilemap
                tilemap = !tilemap
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButton(button: Button, name: String){
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    //println("Button pressed: $name")
                    IO.setButtonPressed(name, true)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    //println("Button released: $name")
                    IO.setButtonPressed(name, false)
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun obtainRom(){
        val romUri: Uri? = intent.getStringExtra(ROM_URI_EXTRA)?.let { Uri.parse(it) }

        romUri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val romBytes = inputStream?.readBytes()
            inputStream?.close()

            emulator.run(romBytes)
        }
    }
}