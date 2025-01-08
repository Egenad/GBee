package es.atm.gbee.core.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import es.atm.gbee.R
import es.atm.gbee.activities.GAME_ID
import es.atm.gbee.core.ROMDataSource
import es.atm.gbee.core.sql.SQLManager

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
                val dao = SQLManager.getDatabase(requireContext()).romDAO()
                dao.updateCoverImage(gameId, it.toString())

                val coverImagePreference: Preference? = findPreference(COVER_KEY)
                coverImagePreference?.summary = it.toString()
                val preferences = requireContext().getSharedPreferences(gameId.toString(), Context.MODE_PRIVATE)
                preferences.edit().putString(COVER_KEY, it.toString()).apply()

                ROMDataSource.getROMById(gameId, requireContext()).imageRes = it.toString()
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

            val preferences = requireContext().getSharedPreferences(gameId.toString(), Context.MODE_PRIVATE)
            val dao = SQLManager.getDatabase(requireContext()).romDAO()
            val rom = dao.getROMById(gameId)

            if(rom != null) {
                // Title
                val gameTitle = preferences.getString(TITLE_KEY, rom.title)

                val titlePreference: EditTextPreference? = findPreference(TITLE_KEY)
                titlePreference?.summary = gameTitle

                titlePreference?.setOnPreferenceChangeListener { preference, newValue ->
                    preference.summary = newValue.toString()
                    preferences.edit().putString(TITLE_KEY, newValue.toString()).apply()
                    dao.updateTitle(gameId, newValue.toString())
                    ROMDataSource.getROMById(gameId, requireContext()).title = newValue.toString()
                    true
                }

                // Cover
                val coverImagePreference: Preference? = findPreference(COVER_KEY)

                coverImagePreference?.setOnPreferenceClickListener {
                    getImageLauncher.launch("image/*")
                    true
                }
            }
        }else{
            Toast.makeText(context, "An error has occurred", Toast.LENGTH_SHORT).show()
        }
    }
}