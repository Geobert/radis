package fr.geobert.radis.tools

import android.text.Editable
import android.widget.EditText
import fr.geobert.radis.ui.CheckingOpDashboard

public class TargetSumWatcher(localeComma: Char, w: EditText, val checkingDashboard: CheckingOpDashboard) : CorrectCommaWatcher(localeComma, w) {
    override fun afterTextChanged(s: Editable) {
        correctComma(s)
        checkingDashboard.updateDisplay()
    }
}
