package fr.geobert.radis.tools;

import android.content.Context;
import fr.geobert.radis.R;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class Formater {
    private static DateFormat DATE_FORMAT = null;
    private static SimpleDateFormat SHORT_DATE_FORMAT = null;
    private static DecimalFormat SUM_FORMAT = null;

    @NotNull
    public static DateFormat getFullDateFormater() {
        if (Formater.DATE_FORMAT == null) {
            Formater.DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
        }
        return Formater.DATE_FORMAT;
    }

    @NotNull
    public static SimpleDateFormat getShortDateFormater(final Context c) {
        if (Formater.SHORT_DATE_FORMAT == null) {
            Formater.SHORT_DATE_FORMAT = new SimpleDateFormat(c.getResources().getString(R.string.short_date_format));
        }
        return Formater.SHORT_DATE_FORMAT;
    }

    @NotNull
    public static DecimalFormat getSumFormater() {
        if (Formater.SUM_FORMAT == null) {
            Formater.SUM_FORMAT = new DecimalFormat();
            Formater.SUM_FORMAT.setMaximumFractionDigits(2);
            Formater.SUM_FORMAT.setMinimumFractionDigits(2);
        }
        return Formater.SUM_FORMAT;
    }
}
