package fr.geobert.Radis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

public class Operation {
	private Date mDate;
	private SimpleDateFormat mDateFormat;
	
	private String mThirdParty;
	private String mTag;
	private String mMode;
	private double mSum;
	
	private Context mCtx;
	
	public Operation(Context ctx) {
		mDateFormat = new SimpleDateFormat("dd/MM/yyyy");
		mCtx = ctx;
	}
	
	public int getMonth() {
		return mDate.getMonth();
	}
	public void setMonth(int month) {
		this.mDate.setMonth(month);
	}
	public int getDay() {
		return mDate.getDay();
	}
	public void setDay(int day) {
		this.mDate.setDate(day);
	}
	public int getYear() {
		return mDate.getYear();
	}
	public void setYear(int year) {
		this.mDate.setYear(year);
	}
	
	public String getDateStr() {
		return mDateFormat.format(mDate);
	}
	
	public long getDate() {
		return mDate.getTime();
	}
	
	public void setDate(long date) {
		mDate = new Date(date);
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
	
	
	
}
