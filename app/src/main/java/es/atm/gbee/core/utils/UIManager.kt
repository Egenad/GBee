package es.atm.gbee.core.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import es.atm.gbee.R

object UIManager {

    fun showCustomAlertDialog(
        context: Context,
        titleResId: Int,
        messageId: Int? = null,
        view: View? = null,
        positiveTextId: Int,
        negativeTextId: Int,
        onPositiveClick: (DialogInterface) -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(context, R.style.DialogStyle)
            .setTitle(titleResId)
            .setPositiveButton(positiveTextId) { dialog, _ ->
                onPositiveClick(dialog)
            }
            .setNegativeButton(negativeTextId) { dialog, _ ->
                dialog.dismiss()
            }

        if(view != null) {
            builder.setView(view)
        }

        if(messageId != null){
            builder.setMessage(messageId)
        }

        builder.show()
    }

    fun showPopupMenu(
        context: Context,
        anchor: View,
        menuResId: Int,
        onMenuItemClick: (itemId: Int) -> Boolean
    ) {
        val popupMenu = PopupMenu(ContextThemeWrapper(context, R.style.PopupMenuStyle), anchor)
        popupMenu.menuInflater.inflate(menuResId, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            onMenuItemClick(item.itemId)
        }

        popupMenu.show()
    }

    fun obtainEditTextContainer(context: Context, hint: String, editText: EditText): View{
        editText.hint = hint

        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 0)
        editText.layoutParams = params
        container.addView(editText)

        return container
    }
}