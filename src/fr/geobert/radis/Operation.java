package fr.geobert.radis;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.database.Cursor;

public class Operation {
	private GregorianCalendar mDate;
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	public static SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd/MM");
	public static DecimalFormat SUM_FORMAT;

	private String mThirdParty;
	private String mTag;
	private String mMode;
	private double mSum;

	public Operation(Cursor op) {
		mThirdParty = op
				.getString(op
						.getColumnIndexOrThrow(OperationsDbAdapter.KEY_THIRD_PARTY_NAME));
		mMode = op.getString(op
				.getColumnIndexOrThrow(OperationsDbAdapter.KEY_MODE_NAME));
		mTag = op.getString(op
				.getColumnIndexOrThrow(OperationsDbAdapter.KEY_TAG_NAME));
		mSum = op.getDouble(op
				.getColumnIndexOrThrow(OperationsDbAdapter.KEY_OP_SUM));
		mDate = new GregorianCalendar();
		mDate.setTimeInMillis(op.getLong(op
				.getColumnIndexOrThrow(OperationsDbAdapter.KEY_OP_DATE)));
	}

	public Operation() {
		mDate = new GregorianCalendar();
		mSum = 0.0d;
		mThirdParty = "";
		mMode = "";
		mTag = "";
	}

	public int getMonth() {
		return mDate.get(Calendar.MONTH);
	}

	public void setMonth(int month) {
		this.mDate.set(Calendar.MONTH, month);
	}

	public int getDay() {
		return mDate.get(Calendar.DAY_OF_MONTH);
	}

	public void setDay(int day) {
		this.mDate.set(Calendar.DAY_OF_MONTH, day);
	}

	public int getYear() {
		return mDate.get(Calendar.YEAR);
	}

	public void setYear(int year) {
		this.mDate.set(Calendar.YEAR, year);
	}

	public String getDateStr() {
		return DATE_FORMAT.format(mDate.getTime());
	}

	public String getShortDateStr() {
		return SHORT_DATE_FORMAT.format(mDate.getTime());
	}

	public long getDate() {
		return mDate.getTimeInMillis();
	}

	public void setDate(long date) {
		mDate.setTimeInMillis(date);
	}

	public String getThirdParty() {
		return mThirdParty;
	}

	public void setThirdParty(String thirdParty) {
		this.mThirdParty = thirdParty;
	}

	public String getTag() {
		return mTag;
	}

	public void setTag(String tag) {
		this.mTag = tag;
	}

	public String getMode() {
		return mMode;
	}

	public void setMode(String mode) {
		this.mMode = mode;
	}

	public double getSum() {
		return mSum;
	}

	public void setSum(double sum) {
		this.mSum = sum;
	}

	public String getSumStr() {
		return SUM_FORMAT.format(mSum);
	}

	public void setSumStr(String sumStr) throws ParseException {
		mSum = SUM_FORMAT.parse(sumStr).doubleValue();
	}

	public void setDateStr(String dateStr) throws ParseException {
		mDate.setTime(DATE_FORMAT.parse(dateStr));

	}
}
