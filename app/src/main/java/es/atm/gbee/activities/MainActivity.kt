package es.atm.gbee.activities

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import es.atm.gbee.activities.adapter.ROMAdapter
import es.atm.gbee.core.ROMDataSource
import es.atm.gbee.core.RomManagement
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.databinding.ActivityMainBinding

const val ROM_UPDATED = "rom_updated"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var romAdapter: ROMAdapter

    // Single File
    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            if(!RomManagement.fileAlreadyExists(this, uri)) {
                val file = RomManagement.saveFileToPrivateStorage(this, uri)

                if (file != null && file.exists()) {
                    val romTitle = file.name
                    val romFilePath = file.absolutePath

                    RomManagement.addROM(this, romFilePath, romTitle)

                    romAdapter.notifyItemInserted(ROMDataSource.roms.size - 1)
                }
            }else{
                Toast.makeText(this, "La ROM seleccionada ya existe.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Folder selected
    private val openFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val files = RomManagement.getGBFilesFromFolder(this, it)

            files.forEach { file ->
                val romTitle = file.name
                val romFilePath = file.absolutePath

                RomManagement.addROM(this, romFilePath, romTitle)
            }

            romAdapter.notifyItemInserted(ROMDataSource.roms.size - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        configureLayout()
        createRecyclerView()
    }

    private fun openFilePicker() {
        openFileLauncher.launch(arrayOf("application/octet-stream"))
    }

    private fun configureLayout(){

        binding.addRomButton.setOnClickListener {
            openFilePicker()
        }

        binding.settingsButton.setOnClickListener {
            Intent (this, SettingsActivity::class.java).also { startActivity(it) }
        }
    }

    private fun createRecyclerView(){

        RomManagement.loadROMSFromDBIfNeeded(this)

        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            3
        } else {
            2
        }

        binding.romGrid.layoutManager = GridLayoutManager(this, spanCount)
        binding.romGrid.itemAnimator = DefaultItemAnimator()

        romAdapter = ROMAdapter(ROMDataSource.roms, spanCount, this)

        // Click Listener
        romAdapter.setOnItemClickListener { romPosition ->

            val selectedROMTitle = romAdapter.romList[romPosition].title

            if (selectedROMTitle != null) {
                val romEntity =
                    SQLManager.getDatabase(this).romDAO().getROMByTitle(selectedROMTitle)

                if (romEntity != null)
                    startActivity(
                        Intent(this, EmuActivity::class.java)
                            .putExtra(ROM_PATH_EXTRA, romEntity.filePath)
                    )
            }
        }

        binding.romGrid.adapter = romAdapter
    }

    override fun onResume() {
        super.onResume()
        romAdapter.notifyDataSetChanged()
    }
}