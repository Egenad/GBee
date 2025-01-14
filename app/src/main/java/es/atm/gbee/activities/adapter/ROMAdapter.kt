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
import java.io.File

class ROMAdapter(val romList: MutableList<ROM>, private val spanCount: Int, private val context: Context)  :
    RecyclerView.Adapter<ROMAdapter.ViewHolder?>() {

    private var listener: (romPosition: Int) -> Unit = {}
    private var listenerLong: (romPosition: Int, v: View) -> Boolean = { _: Int, _: View -> false}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.rom_grid_item, parent, false)

        val holder = ViewHolder(v)

        v.setOnClickListener {
            listener(holder.adapterPosition)
        }

        v.setOnLongClickListener {
            listenerLong(holder.adapterPosition, holder.itemView)
        }

        val screenWidth = parent.resources.displayMetrics.widthPixels
        val cellWidth = screenWidth / spanCount
        val params = v.layoutParams
        params.width = cellWidth
        params.height = cellWidth
        v.layoutParams = params

        return holder
    }

    override fun getItemCount(): Int {
        return romList.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = romList[position]
        holder.bind(item)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var name: TextView
        private var icon: ImageView
        private val view = v

        fun bind(it: ROM) {
            name.text = it.title

            icon.setImageResource(R.drawable.rom_icon)

            if(it.imageRes != null) {
                val file = File(it.imageRes!!)
                if (file.exists())
                    icon.setImageURI(Uri.fromFile(file))
            }

            if(it.selected)
                view.setBackgroundColor(Color.GRAY)
            else
                view.setBackgroundColor(Color.WHITE)
        }

        init {
            name = view.findViewById(R.id.rom_title)
            icon = view.findViewById(R.id.rom_image)
        }
    }

    fun setOnItemClickListener(listener: (romPosition: Int) -> Unit) {
        this.listener = listener
    }

    fun setOnLongItemClickListener(listener: (romPosition: Int, v: View) -> Boolean) {
        this.listenerLong = listener
    }

}