package es.atm.gbee.activities.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import es.atm.gbee.R
import es.atm.gbee.activities.CreateCustomSkinActivity
import es.atm.gbee.activities.SKIN_ID_EXTRA
import es.atm.gbee.core.data.skins.Skin
import java.io.File

class SkinAdapter(
    private val skinList: MutableList<Skin>,
    private val onEditClicked: (Int) -> Unit
)  :
    RecyclerView.Adapter<SkinAdapter.ViewHolder?>() {

    private var listener: (skinPosition: Int) -> Unit = {}
    private var listenerLong: (skinPosition: Int, v: View) -> Boolean = { _: Int, _: View -> false}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.skin_item, parent, false)

        val holder = ViewHolder(v, onEditClicked)

        v.setOnClickListener {
            listener(holder.adapterPosition)
        }

        v.setOnLongClickListener {
            listenerLong(holder.adapterPosition, holder.itemView)
        }

        return holder
    }

    override fun getItemCount(): Int {
        return skinList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = skinList[position]
        holder.bind(item)
    }

    class ViewHolder(v: View, private val onEditClicked: (Int) -> Unit) : RecyclerView.ViewHolder(v) {
        private var name: TextView
        private var id: Int
        private var image: ImageView
        private var editButton: ImageButton
        private val view = v

        fun bind(it: Skin) {
            name.text = it.title
            id = it.id

            image.setImageResource(R.drawable.rom_icon)

            if(it.previewRes != null) {
                val file = File(it.previewRes!!)
                if (file.exists())
                    image.setImageURI(Uri.fromFile(file))
            }

            view.setBackgroundColor(
                if (it.selected) Color.parseColor("#2D2D2D") else Color.BLACK
            )

            editButton.visibility = if (it.editable) View.VISIBLE else View.INVISIBLE

            editButton.setOnClickListener {
                onEditClicked(id)
            }
        }

        init {
            name = view.findViewById(R.id.itemName)
            image = view.findViewById(R.id.itemImage)
            editButton = view.findViewById(R.id.editButton)
            id = -1
        }
    }

    fun setOnItemClickListener(listener: (skinPosition: Int) -> Unit) {
        this.listener = listener
    }

    fun setOnLongItemClickListener(listener: (skinPosition: Int, v: View) -> Boolean) {
        this.listenerLong = listener
    }



}