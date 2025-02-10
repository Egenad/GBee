package es.atm.gbee.activities

import android.app.AlertDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import es.atm.gbee.R
import es.atm.gbee.databinding.ActivityCreateCustomSkinBinding

const val SKIN_ID_EXTRA = "SKIN_ID"

class SkinViewModel : ViewModel() {
    val buttonUris = MutableLiveData<MutableMap<String, Uri?>>()
}

class CreateCustomSkinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCustomSkinBinding
    private lateinit var getImageLauncher: ActivityResultLauncher<String>
    private lateinit var skinViewModel: SkinViewModel

    private var optionsButton: ImageButton? = null
    private var currentButtonName: String? = null
    private var editMode = false
    private var screenOn = true
    private var pickedColor = "#DADADA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCreateCustomSkinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configureEditor()
    }

    private fun configureEditor(){

        skinViewModel = ViewModelProvider(this)[SkinViewModel::class.java]
        skinViewModel.buttonUris.value = mutableMapOf()

        val skinId = intent.getIntExtra(SKIN_ID_EXTRA, -1)

        if(skinId != -1){
            loadEditorImages(skinId)
        }

        optionsButton = binding.optionsButton
        optionsButton!!.setOnClickListener{
            showPopUpMenu()
        }

        getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentButtonName?.let { buttonName ->
                    val currentUris = skinViewModel.buttonUris.value ?: mutableMapOf()
                    currentUris[buttonName] = uri
                    skinViewModel.buttonUris.value = currentUris
                }

                /*val filePath = FileManager.copyFileFromUriToPrivateStorage(this, uri, -1)

                if(filePath != null) {

                    val dao = SQLManager.getDatabase(this).skinDAO()
                    val skin = dao.getSkinById(gameId)

                    // Delete last imageRes if needed
                    if(rom?.imageRes != null)
                        FileManager.deleteFileFromPrivateStorage(rom.imageRes)

                    dao.updateCoverImage(gameId, filePath)

                    ROMDataSource.getROMById(gameId, requireContext()).imageRes = filePath
                }*/
            }
        }

        configureButtons()

    }

    private fun showPopUpMenu(){
        val popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.PopupMenuStyle), optionsButton!!)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.skin_menu_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_save -> {

                    // Need a valid title for the skin if its new
                    if(!editMode){

                        val editText = EditText(this)
                        editText.hint = resources.getString(R.string.enter_title)

                        AlertDialog.Builder(this)
                            .setTitle(R.string.set_title)
                            .setView(editText)
                            .setPositiveButton(R.string.accept) { dialog, _ ->
                                val enteredTitle = editText.text.toString()
                                if (enteredTitle.isBlank()) {
                                    Toast.makeText(this, R.string.title_not_blank, Toast.LENGTH_SHORT).show()
                                }
                                dialog.dismiss()
                            }
                            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                            .show()
                    }

                    // Save skin to DataSource and DB




                    true
                }
                R.id.menu_exit -> {
                    val alertDialog = AlertDialog.Builder(this, R.style.DialogStyle)
                        .setTitle(R.string.confirm_exit)
                        .setMessage(R.string.check_sure_exit)
                        .setPositiveButton(R.string.exit) { _, _ ->
                            finish()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()

                    alertDialog.show()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun loadEditorImages(skinId: Int){

        editMode = true

    }

    private fun configureButtons(){

        binding.main.setOnClickListener{
            val editText = EditText(this)
            editText.hint = resources.getString(R.string.color_hint)

            AlertDialog.Builder(this)
                .setTitle(R.string.pick_color)
                .setView(editText)
                .setPositiveButton(R.string.accept) { dialog, _ ->
                    val enteredColor = editText.text.toString()
                    if (enteredColor.isBlank()) {
                        Toast.makeText(this, R.string.color_not_blank, Toast.LENGTH_SHORT).show()
                    }

                    if (isValidHexColor(enteredColor)) {
                        pickedColor = enteredColor
                        val color = Color.parseColor(enteredColor)
                        binding.main.setBackgroundColor(color)
                    } else {
                        Toast.makeText(this, R.string.color_not_valid, Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        binding.turnScreen.setOnClickListener{
            screenOn = !screenOn

            if(screenOn){
                binding.turnScreen.setImageResource(R.drawable.tv_on)
                binding.screenOnImage.visibility = View.VISIBLE
                binding.screenOffImage.visibility = View.GONE
            }else{
                binding.turnScreen.setImageResource(R.drawable.tv_off)
                binding.screenOnImage.visibility = View.GONE
                binding.screenOffImage.visibility = View.VISIBLE
            }
        }

        binding.buttonA.setOnClickListener {
            currentButtonName = "buttonA"
            getImageLauncher.launch("image/*")
        }

        binding.buttonB.setOnClickListener {
            currentButtonName = "buttonB"
            getImageLauncher.launch("image/*")
        }

        binding.buttonStart.setOnClickListener {
            currentButtonName = "buttonStart"
            getImageLauncher.launch("image/*")
        }

        binding.buttonSelect.setOnClickListener {
            currentButtonName = "buttonSelect"
            getImageLauncher.launch("image/*")
        }

        binding.switchButton.setOnClickListener {
            currentButtonName = "buttonHome"
            getImageLauncher.launch("image/*")
        }

        binding.dpadImage.setOnClickListener {
            currentButtonName = "dpad"
            getImageLauncher.launch("image/*")
        }

        binding.leftHome?.setOnClickListener{
            currentButtonName = "leftHome"
            getImageLauncher.launch("image/*")
        }

        binding.rightHome?.setOnClickListener{
            currentButtonName = "rightHome"
            getImageLauncher.launch("image/*")
        }

        binding.rightBottom.setOnClickListener{
            currentButtonName = "rightBottom"
            getImageLauncher.launch("image/*")
        }

        binding.leftBottom.setOnClickListener{
            currentButtonName = "leftBottom"
            getImageLauncher.launch("image/*")
        }

    }

    private fun isValidHexColor(hexColor: String): Boolean {
        return !TextUtils.isEmpty(hexColor) && hexColor.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))
    }
}