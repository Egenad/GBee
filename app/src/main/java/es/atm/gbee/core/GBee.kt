package es.atm.gbee.core

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import es.atm.gbee.core.data.skins.SkinDataSource

class GBee : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        SkinDataSource.init(this)
    }
}