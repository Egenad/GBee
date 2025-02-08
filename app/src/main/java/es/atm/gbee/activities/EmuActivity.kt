package es.atm.gbee.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityEmuBinding
import es.atm.gbee.databinding.CustomSkinBinding
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
import java.io.File
import java.io.FileInputStream

const val ROM_PATH_EXTRA = "ROM_PATH"

class EmuActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEmuBinding
    private lateinit var bindingCS : CustomSkinBinding
    private lateinit var gameSurfaceView: GameSurfaceView

    private lateinit var dbgButton: View

    private val debugMode = true
    private var tilemap = true
    private var customSkin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if(customSkin) {
            bindingCS = CustomSkinBinding.inflate(layoutInflater)
            setContentView(bindingCS.root)

            gameSurfaceView = bindingCS.gameSurface
        }else{
            binding = ActivityEmuBinding.inflate(layoutInflater)
            setContentView(binding.root)

            gameSurfaceView = binding.gameSurface
        }

        configureButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if(Emulator.isRunning()){
            Emulator.resume()
        }else {
            obtainRom()
        }
    }


    private fun configureButtons(){
        if(customSkin){
            setButton(bindingCS.dpadUp, UP_DPAD)
            setButton(bindingCS.dpadDown, DOWN_DPAD)
            setButton(bindingCS.dpadLeft, LEFT_DPAD)
            setButton(bindingCS.dpadRight, RIGHT_DPAD)
            setButton(bindingCS.buttonA, A_BUTTON)
            setButton(bindingCS.buttonB, B_BUTTON)
            setButton(bindingCS.buttonSelect, SELECT_BUTTON)
            setButton(bindingCS.buttonStart, START_BUTTON)
            dbgButton = bindingCS.switchButton
        }else{
            setButton(binding.dpadUp, UP_DPAD)
            setButton(binding.dpadDown, DOWN_DPAD)
            setButton(binding.dpadLeft, LEFT_DPAD)
            setButton(binding.dpadRight, RIGHT_DPAD)
            setButton(binding.buttonA, A_BUTTON)
            setButton(binding.buttonB, B_BUTTON)
            setButton(binding.buttonSelect, SELECT_BUTTON)
            setButton(binding.buttonStart, START_BUTTON)
            dbgButton = binding.switchButton
        }

        if(debugMode){
            dbgButton.setOnClickListener {
                gameSurfaceView.debugMode = tilemap
                tilemap = !tilemap
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setButton(button: View, name: String){
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

        val romPath = intent.getStringExtra(ROM_PATH_EXTRA)

        if (!romPath.isNullOrEmpty()) {
            val romFile = File(romPath)

            if (romFile.exists()) {
                try {
                    FileInputStream(romFile).use { stream ->
                        val romBytes = stream.readBytes()
                        Emulator.run(romBytes)
                    }
                } catch (e: Exception) {
                    Log.e("ROMManagement", "Error al procesar el archivo: $romPath", e)
                }
            } else {
                Log.e("ROMManagement", "El archivo no existe: $romPath")
            }
        } else {
            Log.e("ROMManagement", "El path de la ROM es nulo o vacío.")
        }
    }

    override fun onResume() {
        super.onResume()
        gameSurfaceView.resume()
        Emulator.resume()
    }

    override fun onPause() {
        super.onPause()
        gameSurfaceView.pause()
        Emulator.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isChangingConfigurations) {
            Emulator.pause()
        } else {
            gameSurfaceView.release()
            Emulator.stop()
        }
    }
}