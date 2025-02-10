package es.atm.gbee.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import es.atm.gbee.R
import es.atm.gbee.activities.adapter.SkinAdapter
import es.atm.gbee.core.data.skins.SkinDataSource
import es.atm.gbee.core.data.skins.SkinsManagement
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.databinding.ActivityCustomSkinsBinding

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

        skinAdapter = SkinAdapter(SkinDataSource.skins, this)

        binding.skinList.adapter = skinAdapter

        createRecycledViewListeners()
    }

    private fun createRecycledViewListeners(){

        val recycledList = binding.skinList
        val recyclerAdapter = recycledList.adapter as SkinAdapter

        recyclerAdapter.setOnItemClickListener {
                skinPosition ->
            run {

                /*val selectedSkinTitle = skinAdapter.skinList[skinPosition].title

                if (selectedSkinTitle != null) {
                    val skinEntity = SQLManager.getDatabase(this).skinDAO().getSkinByTitle(selectedSkinTitle)

                    if (skinEntity != null)
                        startActivity(
                            Intent(this, CreateCustomSkinActivity::class.java)
                                .putExtra(SKIN_ID_EXTRA, skinEntity.id)
                        )
                }*/

            }
        }

        /*recyclerAdapter.setOnLongItemClickListener {
            if (customCallback.actionMode != null) {
                false
            } else {
                customCallback.startSupportActionMode((activity as AppCompatActivity?)!!, recyclerAdapter)
                customCallback.actionItemClicked(it)
                true
            }
        }*/
    }

    private fun configureLayout(){
        binding.addSkinButton.setOnClickListener {
            val intent = Intent(this, CreateCustomSkinActivity::class.java)
            this.startActivity(intent)
        }
    }
}