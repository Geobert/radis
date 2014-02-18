package fr.geobert.radis.tools;

import android.text.Editable;
import android.widget.EditText;

public class TargetSumWatcher extends CorrectCommaWatcher {
    private final UpdateDisplayInterface updateItf;

    public TargetSumWatcher(char localeComma, EditText w, UpdateDisplayInterface update) {
        super(localeComma, w);
        this.updateItf = update;
    }

    @Override
    public void afterTextChanged(Editable s) {
        correctComma(s);
        updateItf.updateDisplay(null);
    }
}
