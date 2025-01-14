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

const val DMG_SKIN_PREFERENCE = "dmg_skin"
const val CGB_SKIN_PREFERENCE = "gbc_skin"

class LayoutSettingsFragment : PreferenceFragmentCompat() {

    private val activityDMGLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

        }
    }

    private val activityGBCLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.layout_preferences, rootKey)

        val dmgSkinPreference: Preference? = findPreference(DMG_SKIN_PREFERENCE)
        dmgSkinPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CustomSkinsActivity::class.java)
            activityDMGLauncher.launch(intent)
            true
        }

        val gbcSkinPreference: Preference? = findPreference(CGB_SKIN_PREFERENCE)
        gbcSkinPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), CustomSkinsActivity::class.java)
            activityGBCLauncher.launch(intent)
            true
        }

    }
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Layout"
    }
}