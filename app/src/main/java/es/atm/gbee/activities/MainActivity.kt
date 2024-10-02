package es.atm.gbee.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityEmuBinding
import es.atm.gbee.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var selectRomButton: Button
    private lateinit var binding: ActivityMainBinding

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {

            val intent = Intent(this, EmuActivity::class.java).apply {
                putExtra(ROM_URI_EXTRA, it.toString())
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectRomButton = binding.selectRomButton

        selectRomButton.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        openFileLauncher.launch(arrayOf("application/octet-stream"))
    }
}