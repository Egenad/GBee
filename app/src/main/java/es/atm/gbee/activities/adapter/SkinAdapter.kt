package es.atm.gbee.activities.adapter

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import es.atm.gbee.R
import es.atm.gbee.core.data.rom.ROM
import es.atm.gbee.core.data.skins.Skin
import java.io.File

class SkinAdapter(val skinList: MutableList<Skin>, private val context: Context)  :
    RecyclerView.Adapter<SkinAdapter.ViewHolder?>() {

    private var listener: (skinPosition: Int) -> Unit = {}
    private var listenerLong: (skinPosition: Int, v: View) -> Boolean = { _: Int, _: View -> false}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.skin_item, parent, false)

        val holder = ViewHolder(v)

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

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var name: TextView
        private val view = v

        fun bind(it: Skin) {
            name.text = it.title
        }

        init {
            name = view.findViewById(R.id.itemName)
        }
    }

    fun setOnItemClickListener(listener: (skinPosition: Int) -> Unit) {
        this.listener = listener
    }

    fun setOnLongItemClickListener(listener: (skinPosition: Int, v: View) -> Boolean) {
        this.listenerLong = listener
    }

}