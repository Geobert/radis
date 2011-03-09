package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.database.Cursor;
import android.os.Parcel;

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
	public boolean mHasEnd;
	public GregorianCalendar mEndDate;
	public long mAccountId;

	public ScheduledOperation(Cursor op) {
		super(op);
		mAccountId = op.getLong(op
				.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_ACCOUNT_ID));
		mEndDate = new GregorianCalendar();
		mEndDate.setTimeInMillis(op.getLong(op
				.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_END_DATE)));
		mPeriodicity = op.getInt(op
				.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_PERIODICITY));
		mPeriodicityUnit = op
				.getInt(op
						.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_PERIODICITY_UNIT));
	}

	public ScheduledOperation() {
		super();
		mAccountId = 0;
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
		mPeriodicity = 0;
		mPeriodicityUnit = 0;
	}

	public ScheduledOperation(Parcel parcel) {
		super(parcel);
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
		readFromParcel(parcel);
	}

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

	public static String getUnitStr(final Activity context, final int unit,
			final int periodicity) {
		String s = context.getResources().getStringArray(
				R.array.periodicity_labels)[unit];
		if (unit < 4) {
			return s;
		} else if (unit <= 6) {
			return String.format(s, periodicity);
		}
		return null;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeInt(mPeriodicity);
		dest.writeInt(mPeriodicityUnit);
		dest.writeLong(mAccountId);
		dest.writeInt(getEndDay());
		dest.writeInt(getEndMonth());
		dest.writeInt(getEndYear());
	}

	private void readFromParcel(Parcel in) {
		mPeriodicity = in.readInt();
		mPeriodicityUnit = in.readInt();
		mAccountId = in.readLong();
		setEndDay(in.readInt());
		setEndMonth(in.readInt());
		setEndYear(in.readInt());
	}
}
