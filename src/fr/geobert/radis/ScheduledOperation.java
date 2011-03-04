package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;

public class ScheduledOperation extends Operation {
	public static int ONCE_PERIOD = 0;
	public static int DAILY_PERIOD = 1;
	public static int MONTHLY_PERIOD = 2;
	public static int YEARLY_PERIOD = 3;
	public static int CUSTOM_DAILY_PERIOD = 4;
	public static int CUSTOM_MONTHLY_PERIOD = 5;
	public static int CUSTOM_YEARLY_PERIOD = 6;
	
	
	public int mPeriodicity;
	public int mPeriodicityUnit;
	private GregorianCalendar mEndDate;
	public long mAccountId;
	
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
	
	public static String getUnitStr(final Activity context, final int unit, final int periodicity) {
		String s = context.getResources().getStringArray(R.array.periodicity_labels)[unit];
		if (unit < 4) {
			return s;
		} else if (unit <= 6) {
			return String.format(s, periodicity);
		}
		return null;
	}
}
