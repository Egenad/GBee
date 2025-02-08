package es.atm.gbee.core

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GBee : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}