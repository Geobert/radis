package fr.geobert.radis.tools

import android.text.Editable
import android.text.TextWatcher
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import fr.geobert.radis.ui.QuickAddController

class QuickAddTextWatcher(private val mThirdParty: AutoCompleteTextView,
                          private val mAmount: EditText, private val mQuickAdd: ImageButton) : TextWatcher {

    override fun afterTextChanged(s: Editable) {
        val amount = mAmount
        QuickAddController.setQuickAddButEnabled(
                mQuickAdd,
                ((mThirdParty.length() != 0) && (amount.length() != 0) && !(amount.length() == 1 && amount.text.charAt(0) == '-')))
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int,
                                   after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

}
