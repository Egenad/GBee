package es.atm.gbee.core.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import es.atm.gbee.R
import es.atm.gbee.core.sql.SQLManager

class GameSettingsFragment(private val gameId: Int) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.game_preferences, rootKey)
    }
    override fun onResume() {
        super.onResume()
        val rom = SQLManager.getDatabase(requireContext()).romDAO().getROMById(gameId)
        (activity as? AppCompatActivity)?.supportActionBar?.title = rom?.title ?: "Game Settings"
    }
}