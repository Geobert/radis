package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class ScheduledOperation extends Operation {
	public static int ONCE_PERIOD = 0;
	public static int DAILY_PERIOD = 1;
	public static int MONTHLY_PERIOD = 2;
	public static int YEARLY_PERIOD = 3;
	
	public int mPeriodicity;
	public int mPeriodicityUnit;
	private GregorianCalendar mEndDate;
	
	public int getEndMonth() {
		return mEndDate.get(Calendar.MONTH);
	}

	public void setEndMonth(int month) {
		this.mEndDate.set(Calendar.MONTH, month);
	}

	public int getEndDay() {
		return mEndDate.get(Calendar.DAY_OF_MONTH);
	}

	public void setEndDay(int day) {
		this.mEndDate.set(Calendar.DAY_OF_MONTH, day);
	}

	public int getEndYear() {
		return mEndDate.get(Calendar.YEAR);
	}

	public void setEndYear(int year) {
		this.mEndDate.set(Calendar.YEAR, year);
	}
	
	public long getEndDate() {
		return mEndDate.getTimeInMillis();
	}

}
