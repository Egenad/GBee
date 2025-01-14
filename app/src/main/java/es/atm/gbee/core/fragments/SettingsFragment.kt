package es.atm.gbee.core.fragments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import es.atm.gbee.R
import es.atm.gbee.activities.AboutActivity

const val ABOUT_PREFERENCE = "about_settings"

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val aboutPreference: Preference? = findPreference(ABOUT_PREFERENCE)
        aboutPreference?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
            true
        }

    }
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Settings"
    }
}