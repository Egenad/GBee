package es.atm.gbee.core.fragments

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import es.atm.gbee.R

const val VSYNC_PREFERENCE          = "vsync_preference"
const val FPS_PREFERENCE            = "fps_preference"
const val DMG_PALETTE_PREFERENCE    = "dmg_palette"
const val CGB_PALETTE_PREFERENCE    = "gbc_palette"

class VideoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.video_preferences, rootKey)

        configureLayout()
    }
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Video"
    }

    private fun configureLayout(){

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val vsync = preferences.getBoolean(VSYNC_PREFERENCE, true)
        val fps = preferences.getBoolean(FPS_PREFERENCE, false)

        val dmgPalPreference: ListPreference? = findPreference(DMG_PALETTE_PREFERENCE)
        dmgPalPreference?.let { preference ->
            val titleTextView = preference.title as? TextView
            titleTextView?.setTextColor(Color.BLACK)
        }

        val gbcPalPreference: ListPreference? = findPreference(CGB_PALETTE_PREFERENCE)
        gbcPalPreference?.let { preference ->
            val titleTextView = preference.title as? TextView
            titleTextView?.setTextColor(Color.BLACK)
        }

    }
}