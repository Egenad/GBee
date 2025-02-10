package es.atm.gbee.core.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import es.atm.gbee.R
import es.atm.gbee.activities.CustomSkinsActivity

const val CUSTOM_SKIN_PREFERENCE = "custom_skin"
const val USE_SKIN_PREFERENCE = "use_skins"

class LayoutSettingsFragment : PreferenceFragmentCompat() {

    private val activityCustomSkinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.layout_preferences, rootKey)

        val skinPreference: Preference? = findPreference(CUSTOM_SKIN_PREFERENCE)
        skinPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CustomSkinsActivity::class.java)
            activityCustomSkinLauncher.launch(intent)
            true
        }

    }
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Layout"
    }
}