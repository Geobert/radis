package fr.geobert.radis;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class Formater {
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"dd/MM/yyyy");
	public static SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat(
			"dd/MM");
	public static DecimalFormat SUM_FORMAT;
	
	public static void init() {
		Formater.SUM_FORMAT = new DecimalFormat();
		Formater.SUM_FORMAT.setMaximumFractionDigits(2);
		Formater.SUM_FORMAT.setMinimumFractionDigits(2);
	}
	
	public static boolean isInit() {
		return SUM_FORMAT == null;
	}
}
