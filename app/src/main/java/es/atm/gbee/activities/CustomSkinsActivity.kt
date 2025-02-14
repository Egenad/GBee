package es.atm.gbee.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import es.atm.gbee.R
import es.atm.gbee.activities.adapter.SkinAdapter
import es.atm.gbee.core.data.skins.SkinDataSource
import es.atm.gbee.core.data.skins.SkinsManagement
import es.atm.gbee.databinding.ActivityCustomSkinsBinding

const val SELECTED_SKIN = "selected_skin"

class CustomSkinsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomSkinsBinding
    private lateinit var skinAdapter: SkinAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCustomSkinsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createRecyclerView()
        configureLayout()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createRecyclerView(){

        SkinsManagement.loadSkinsFromDBIfNeeded(this)

        binding.skinList.layoutManager = LinearLayoutManager(this)
        binding.skinList.itemAnimator = DefaultItemAnimator()

        skinAdapter = SkinAdapter(SkinDataSource.skins) {skinId ->
            val intent = Intent(this, CreateCustomSkinActivity::class.java)
            intent.putExtra(SKIN_ID_EXTRA, skinId)
            launcher.launch(intent)
        }

        binding.skinList.adapter = skinAdapter
    }

    private fun configureLayout(){
        binding.addSkinButton.setOnClickListener {
            val intent = Intent(this, CreateCustomSkinActivity::class.java)
            launcher.launch(intent)
        }

        skinAdapter.setOnItemClickListener { skinPosition ->

            // Deselect last skin and notify if needed
            val position = SkinDataSource.deselectLastSkin()
            var toSelect = -1

            if(position != -1) {
                skinAdapter.notifyItemChanged(position)
                SkinsManagement.updateSkin(this, SkinDataSource.skins[position])
            }

            if(position != skinPosition) {
                // Select skin in DataSource and notify
                SkinDataSource.selectSkinByPosition(skinPosition)
                skinAdapter.notifyItemChanged(skinPosition)

                // Save to shared preferences
                val selectedSkin = SkinDataSource.skins[skinPosition]
                toSelect = selectedSkin.id
                Toast.makeText(this, "Skin ${selectedSkin.title} selected", Toast.LENGTH_SHORT).show()

                // Save to DB
                SkinsManagement.updateSkin(this, SkinDataSource.skins[skinPosition])
            }

            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            preferences.edit().putInt(SELECTED_SKIN, toSelect).apply()
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val changesMade = result.data?.getBooleanExtra(CHANGES_MADE, false) ?: false
            if (changesMade) {
                val skinUpdated = result.data?.getIntExtra(SKIN_ID_EXTRA, -1) ?: -1
                if(skinUpdated != -1){
                    val skinPosition = SkinDataSource.getPositionById(skinUpdated) ?: -1
                    if(skinPosition != -1)
                        skinAdapter.notifyItemChanged(skinPosition)
                }else{
                    skinAdapter.notifyDataSetChanged()
                }
            }
        }
    }
}