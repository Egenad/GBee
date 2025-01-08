package es.atm.gbee.activities.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import es.atm.gbee.R
import es.atm.gbee.activities.GAME_ID
import es.atm.gbee.activities.SettingsActivity
import es.atm.gbee.core.ROM
import es.atm.gbee.core.RomManagement
import es.atm.gbee.core.sql.SQLManager

class ROMAdapter(val romList: MutableList<ROM>, private val spanCount: Int, private val context: Context)  :
    RecyclerView.Adapter<ROMAdapter.ViewHolder?>() {

    private var listener: (romPosition: Int) -> Unit = {}
    private var listenerLong: (romPosition: Int) -> Boolean = {false}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.rom_grid_item, parent, false)

        val holder = ViewHolder(v)

        v.setOnClickListener {
            listener(holder.adapterPosition)
        }

        v.setOnLongClickListener {
            listenerLong(holder.adapterPosition)
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

        holder.itemView.setOnLongClickListener { v ->
            showPopupMenu(v, position)
            true
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private var name: TextView
        private var icon: ImageView
        private val view = v

        fun bind(it: ROM) {
            name.text = it.title

            if(it.imageRes != null)
                icon.setImageURI(Uri.parse(it.imageRes))
            else
                icon.setImageResource(R.drawable.rom_icon)

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

    fun setOnItemClickListener(listener: (filmPosition: Int) -> Unit) {
        this.listener = listener
    }

    fun setOnLongItemClickListener(listener: (filmPosition: Int) -> Boolean) {
        this.listenerLong = listener
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(ContextThemeWrapper(context, R.style.PopupMenuStyle), view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    handleSettings(position)
                    true
                }
                R.id.menu_delete -> {
                    handleDelete(position)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun handleSettings(position: Int) {
        val intent = Intent(context, SettingsActivity::class.java)

        val rom = SQLManager.getDatabase(context).romDAO().getROMById(romList[position].id)

        intent.putExtra(GAME_ID, rom?.id ?: -1)
        context.startActivity(intent)
    }

    private fun handleDelete(position: Int) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.check_sure)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteRom(romList[position], position)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun deleteRom(rom: ROM, position: Int) {
        val dao = SQLManager.getDatabase(context).romDAO()
        val romEntity = dao.getROMByTitle(rom.title ?: "")
        if(romEntity != null) {
            // Delete private file and database entry
            if(RomManagement.deleteROM(context, romEntity)) {
                // Delete from Adapter
                romList.removeAt(position)
                notifyItemRemoved(position)

                Toast.makeText(context, "${rom.title} has been deleted", Toast.LENGTH_SHORT).show()

                return
            }
        }
        Toast.makeText(context, "An error has occurred", Toast.LENGTH_SHORT).show()
    }
}