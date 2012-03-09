package fr.geobert.radis.tools;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import android.content.Context;
import fr.geobert.radis.R;

public class Formater {
	public static SimpleDateFormat DATE_FORMAT;
	public static SimpleDateFormat SHORT_DATE_FORMAT;
	public static DecimalFormat SUM_FORMAT;
	
	public static void init(Context c) {
		Formater.SUM_FORMAT = new DecimalFormat();
		Formater.SUM_FORMAT.setMaximumFractionDigits(2);
		Formater.SUM_FORMAT.setMinimumFractionDigits(2);
		Formater.DATE_FORMAT = new SimpleDateFormat(c.getResources().getString(R.string.long_date_format));
		Formater.SHORT_DATE_FORMAT = new SimpleDateFormat(c.getResources().getString(R.string.short_date_format));
	}
	
	public static boolean isInit() {
		return SUM_FORMAT != null;
	}
}
