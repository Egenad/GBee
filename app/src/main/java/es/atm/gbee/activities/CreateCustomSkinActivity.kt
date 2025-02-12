package es.atm.gbee.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import es.atm.gbee.R
import es.atm.gbee.core.data.skins.Skin
import es.atm.gbee.core.data.skins.SkinDataSource
import es.atm.gbee.core.data.skins.SkinsManagement
import es.atm.gbee.core.sql.SQLManager
import es.atm.gbee.core.utils.FileManager
import es.atm.gbee.core.utils.UIManager
import es.atm.gbee.databinding.ActivityCreateCustomSkinBinding

const val SKIN_ID_EXTRA     = "SKIN_ID"

const val BUTTON_A          = "buttonA"
const val BUTTON_B          = "buttonB"
const val BUTTON_START      = "buttonStart"
const val BUTTON_SELECT     = "buttonSelect"
const val BUTTON_HOME       = "buttonHome"
const val LEFT_HOME         = "leftHome"
const val RIGHT_HOME        = "rightHome"
const val BUTTON_DPAD       = "dpad"
const val SCREEN_ON         = "screen_on"
const val SCREEN_OFF        = "screen_off"
const val RIGHT_BOTTOM      = "rightBottom"
const val LEFT_BOTTOM       = "leftBottom"

class CreateCustomSkinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCustomSkinBinding
    private lateinit var getImageLauncher: ActivityResultLauncher<String>
    private lateinit var buttonMap: Map<View?, String>

    private val buttonUris = mutableMapOf<String, Uri?>()
    private var currentButtonName: String? = null
    private var editMode = false
    private var screenOn = true
    private var skinId = -1
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

         buttonMap = mapOf(
             binding.buttonA to BUTTON_A,
             binding.buttonB to BUTTON_B,
             binding.buttonStart to BUTTON_START,
             binding.buttonSelect to BUTTON_SELECT,
             binding.switchButton to BUTTON_HOME,
             binding.dpadImage to BUTTON_DPAD,
             binding.leftHome to LEFT_HOME,
             binding.rightHome to RIGHT_HOME,
             binding.rightBottom to RIGHT_BOTTOM,
             binding.leftBottom to LEFT_BOTTOM,
             binding.screenOnImage to SCREEN_ON,
             binding.screenOffImage to SCREEN_OFF
        )

        skinId = intent.getIntExtra(SKIN_ID_EXTRA, -1)

        if(skinId != -1){
            loadEditorImages(skinId)
        }

        binding.optionsButton.setOnClickListener{
            showPopUpMenu()
        }

        getImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentButtonName?.let { buttonName ->
                    buttonUris[buttonName] = uri

                    val buttonView = buttonMap.entries.find { it.value == buttonName }?.key
                    if (buttonView is ImageView) {
                        buttonView.setImageURI(uri)
                    } else {
                        buttonView?.background = Drawable.createFromPath(uri.path)
                    }
                }
            }
        }

        configureButtons()
    }

    private fun showPopUpMenu() {
        UIManager.showPopupMenu(this, binding.optionsButton, R.menu.skin_menu_options) { itemId ->
            when (itemId) {
                R.id.menu_save -> {
                    if (!editMode)
                        saveNewSkin()
                    else
                        updateSkin()
                    true
                }
                R.id.menu_exit -> {
                    UIManager.showCustomAlertDialog(
                        this,
                        R.string.check_sure_exit,
                        null,
                        null,
                        R.string.accept,
                        R.string.cancel
                    ) { dialog ->
                        dialog.dismiss()
                        /*Handler(Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 100)*/
                    }

                    true
                }
                else -> false
            }
        }
    }

    private fun loadEditorImages(skinId: Int){

        editMode = true

    }

    private fun configureButtons(){

        binding.main.setOnClickListener{
            val editText = EditText(this)
            editText.hint = resources.getString(R.string.color_hint)

            UIManager.showCustomAlertDialog(
                this,
                R.string.pick_color,
                null,
                editText,
                R.string.accept,
                R.string.cancel
            ) { dialog ->
                val enteredColor = editText.text.toString()
                if (enteredColor.isBlank()) {
                    Toast.makeText(this, R.string.color_not_blank, Toast.LENGTH_SHORT).show()
                } else if (isValidHexColor(enteredColor)) {
                    changePickedColor(enteredColor)
                } else {
                    Toast.makeText(this, R.string.color_not_valid, Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
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

        buttonMap.forEach { (button, value) ->
            button?.setOnClickListener {
                currentButtonName = value
                getImageLauncher.launch("image/*")
            }
        }

    }

    private fun isValidHexColor(hexColor: String): Boolean {
        return !TextUtils.isEmpty(hexColor) && hexColor.matches(Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"))
    }

    private fun saveNewSkin(){

        val skinDao = SQLManager.getDatabase(this).skinDAO()

        val editText = EditText(this)
        val container = UIManager.obtainEditTextContainer(this, resources.getString(R.string.enter_title), editText)
        
        // Need a valid title for the skin if its new

        UIManager.showCustomAlertDialog(
            this,
            R.string.set_title,
            null,
            container,
            R.string.accept,
            R.string.cancel
        ) { dialog ->
            val enteredTitle = editText.text.toString()
            if (enteredTitle.isNotBlank()) {
                // Check that there's not another skin with the same name
                val oldSkin = skinDao.getSkinByTitle(enteredTitle)
                if(oldSkin == null){
                    // Save to DataSource and DB
                    SkinsManagement.addSkin(this, generateNewSkin(enteredTitle))
                    Toast.makeText(this, R.string.skin_saved_correctly, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }else{
                    Toast.makeText(this, R.string.title_exists, Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, R.string.title_not_blank, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateNewSkin(title: String): Skin{
        val newSkin = SkinDataSource.getDefaultSkin()
        newSkin.deletable = true
        newSkin.editable = true

        newSkin.title = title
        newSkin.backgroundColor = pickedColor

        buttonUris.forEach { (viewName: String, uri) ->

            if(uri != null) {
                val bytes = FileManager.uriToByteArray(this, uri)

                if (bytes != null) {
                    when (viewName) {
                        BUTTON_A -> newSkin.aButton = bytes
                        BUTTON_B -> newSkin.bButton = bytes
                        BUTTON_START -> newSkin.startSelectButtons = bytes
                        BUTTON_SELECT -> newSkin.startSelectButtons = bytes
                        BUTTON_HOME -> newSkin.homeButton = bytes
                        LEFT_BOTTOM -> newSkin.leftBottomImage = bytes
                        RIGHT_BOTTOM -> newSkin.rightBottomImage = bytes
                        LEFT_HOME -> newSkin.leftHomeImage = bytes
                        RIGHT_HOME -> newSkin.rightHomeImage = bytes
                        BUTTON_DPAD -> newSkin.dpad = bytes
                        SCREEN_ON -> newSkin.screenOn = bytes
                        SCREEN_OFF -> newSkin.screenOff = bytes
                    }
                }
            }
        }

        return newSkin
    }

    private fun changePickedColor(colorStr: String){
        pickedColor = colorStr
        val color = Color.parseColor(colorStr)
        binding.main.setBackgroundColor(color)

        val colorState = ColorStateList.valueOf(color)

        // Change tint color for images
        if(binding.leftHome != null)
            ImageViewCompat.setImageTintList(binding.leftHome!!, colorState)

        if(binding.rightHome != null)
            ImageViewCompat.setImageTintList(binding.rightHome!!, colorState)

        ImageViewCompat.setImageTintList(binding.rightBottom!!, colorState)
        ImageViewCompat.setImageTintList(binding.leftBottom!!, colorState)
    }

    private fun updateSkin(){

    }
}