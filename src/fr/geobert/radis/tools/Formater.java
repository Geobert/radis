package fr.geobert.radis.tools;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import fr.geobert.radis.R;

public class Formater {
	private static SimpleDateFormat DATE_FORMAT = null;
	private static SimpleDateFormat SHORT_DATE_FORMAT = null;
	private static DecimalFormat SUM_FORMAT = null;
	
	public static SimpleDateFormat getFullDateFormater(final Context c) {
		if (Formater.DATE_FORMAT == null) {
			Formater.DATE_FORMAT = new SimpleDateFormat(c.getResources().getString(R.string.long_date_format));
		}
		return Formater.DATE_FORMAT;
	}
	
	public static SimpleDateFormat getShortDateFormater(final Context c) {
		if (Formater.SHORT_DATE_FORMAT == null) {
			Formater.SHORT_DATE_FORMAT = new SimpleDateFormat(c.getResources().getString(R.string.short_date_format));
		}
		return Formater.SHORT_DATE_FORMAT;
	}
	
	public static DecimalFormat getSumFormater() {
		if (Formater.SUM_FORMAT == null) {
			Formater.SUM_FORMAT = new DecimalFormat();
			Formater.SUM_FORMAT.setMaximumFractionDigits(2);
			Formater.SUM_FORMAT.setMinimumFractionDigits(2);
		}
		return Formater.SUM_FORMAT;
	}
}
