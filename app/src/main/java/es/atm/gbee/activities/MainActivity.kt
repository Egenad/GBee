package es.atm.gbee.activities

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import es.atm.gbee.R
import es.atm.gbee.activities.adapter.ROMAdapter
import es.atm.gbee.core.data.rom.ROMDataSource
import es.atm.gbee.core.data.rom.RomManagement
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.utils.UIManager
import es.atm.gbee.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var romAdapter: ROMAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        configureLayout()
        createRecyclerView()
    }

    private fun configureLayout(){
        binding.addRomButton.setOnClickListener {
            openFileLauncher.launch(arrayOf("application/octet-stream"))
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

        romAdapter = ROMAdapter(ROMDataSource.roms, spanCount)

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

        romAdapter.setOnLongItemClickListener { romPosition, v ->
            showPopupMenu(v, romPosition)
            true
        }

        binding.romGrid.adapter = romAdapter
    }

    private fun updateModifiedRom(romId: Int) {
        val position = ROMDataSource.getPositionById(romId)
        if (position != null && position >= 0) {
            romAdapter.notifyItemChanged(position)
        }
    }

    private fun showPopupMenu(v: View, position: Int) {
        val popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.PopupMenuStyle), v)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    handleSettings(position)
                    true
                }
                R.id.menu_delete -> {
                    handleDelete(position)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun handleSettings(position: Int) {
        val rom = SQLManager.getDatabase(this).romDAO().getROMById(romAdapter.romList[position].id)

        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(GAME_ID, rom?.id ?: -1)
        settingsLauncher.launch(intent)
    }

    private fun handleDelete(position: Int) {

        UIManager.showCustomAlertDialog(
            this,
            R.string.confirm_delete,
            R.string.check_sure,
            null,
            R.string.delete,
            R.string.cancel,
        ) { _ ->
            if(RomManagement.deleteRom(this, romAdapter.romList[position], position))
                romAdapter.notifyItemRemoved(position)
        }
    }

    /****
     *
     *  LAUNCHERS
     *
     ****/

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val modifiedRomId = result.data?.getIntExtra(GAME_ID, -1) ?: -1
            if (modifiedRomId != -1) {
                updateModifiedRom(modifiedRomId)
            }
        }
    }

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
}