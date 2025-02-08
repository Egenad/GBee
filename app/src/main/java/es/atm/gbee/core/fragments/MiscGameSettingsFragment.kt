package es.atm.gbee.core.fragments

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import es.atm.gbee.R
import es.atm.gbee.activities.GAME_ID
import es.atm.gbee.core.data.rom.ROMDataSource
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.utils.FileManager
import java.io.File

const val TITLE_KEY = "game_title"
const val COVER_KEY = "cover_image"

class MiscGameSettingsFragment : PreferenceFragmentCompat() {

    private var gameId: Int = -1
    private lateinit var getImageLauncher: ActivityResultLauncher<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.misc_game_preferences, rootKey)

        arguments?.let {
            gameId = it.getInt(GAME_ID, -1)
        }

        getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {

                val filePath = FileManager.copyFileFromUriToPrivateStorage(requireContext(), uri, gameId)

                if(filePath != null) {
                    val coverImagePreference: Preference? = findPreference(COVER_KEY)
                    coverImagePreference?.summary = filePath
                    setImageToCoverPreference(coverImagePreference, filePath)

                    val dao = SQLManager.getDatabase(requireContext()).romDAO()
                    val rom = dao.getROMById(gameId)

                    // Delete last imageRes if needed
                    if(rom?.imageRes != null)
                        FileManager.deleteFileFromPrivateStorage(rom.imageRes)

                    dao.updateCoverImage(gameId, filePath)

                    ROMDataSource.getROMById(gameId, requireContext()).imageRes = filePath
                }
            }
        }

        configureLayout()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Miscellaneous"
    }

    private fun configureLayout(){

        if(gameId != -1) {

            val dao = SQLManager.getDatabase(requireContext()).romDAO()
            val rom = dao.getROMById(gameId)

            if(rom != null) {
                // Title
                val titlePreference: EditTextPreference? = findPreference(TITLE_KEY)
                titlePreference?.summary = rom.title
                titlePreference?.text = rom.title

                titlePreference?.setOnPreferenceChangeListener { preference, newValue ->
                    if(FileManager.isTitleValid(newValue.toString())) {
                        preference.summary = newValue.toString()
                        dao.updateTitle(gameId, newValue.toString())
                        ROMDataSource.getROMById(gameId, requireContext()).title =
                            newValue.toString()
                        true
                    }else{
                        Toast.makeText(context, R.string.invalid_name, Toast.LENGTH_SHORT).show()
                        false
                    }
                }

                // Cover
                val coverImagePreference: Preference? = findPreference(COVER_KEY)

                if(rom.imageRes != null){
                    coverImagePreference?.summary = rom.imageRes
                    setImageToCoverPreference(coverImagePreference, rom.imageRes)
                }

                coverImagePreference?.setOnPreferenceClickListener {
                    getImageLauncher.launch("image/*")
                    true
                }
            }
        }else{
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setImageToCoverPreference(preference: Preference?, filePath: String){
        val file = File(filePath)
        if (file.exists()) {
            val drawable = Drawable.createFromPath(file.absolutePath)
            if (drawable != null) {
                preference?.icon = drawable
            }
        }
    }
}