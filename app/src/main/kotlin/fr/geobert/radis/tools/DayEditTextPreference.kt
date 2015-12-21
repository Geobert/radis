package fr.geobert.radis.tools

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.preference.EditTextPreference
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet

class DayEditTextPreference : EditTextPreference {
    constructor(ctx: Context, attrs: AttributeSet,
                defStyle: Int) : super(ctx, attrs, defStyle) {
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
    }

    private inner class EditTextWatcher : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int,
                                   count: Int) {
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, before: Int,
                                       count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            onEditTextChanged()
        }
    }

    private var m_watcher = EditTextWatcher()

    /**
     * Return true in order to enable positive button or false to disable it.
     */
    protected fun onCheckValue(value: String): Boolean {
        var res: Boolean
        try {
            val d = Integer.parseInt(value)
            res = d > 0 && d <= 31
        } catch (e: Exception) {
            res = false
        }

        return res
    }

    protected fun onEditTextChanged() {
        val enable = onCheckValue(editText.text.toString())
        val dlg = dialog
        if (dlg is AlertDialog) {
            val btn = dlg.getButton(DialogInterface.BUTTON_POSITIVE)
            btn.isEnabled = enable
        }
    }

    override fun showDialog(state: Bundle?) {
        super.showDialog(state)

        editText.removeTextChangedListener(m_watcher)
        editText.addTextChangedListener(m_watcher)
        onEditTextChanged()
    }
}
