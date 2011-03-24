package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import fr.geobert.radis.db.OperationsDbAdapter;
import fr.geobert.radis.tools.Tools;

import android.app.Activity;
import android.database.Cursor;
import android.os.Parcel;

public class ScheduledOperation extends Operation {
	public final static int WEEKLY_PERIOD = 0;
	public final static int MONTHLY_PERIOD = 1;
	public final static int YEARLY_PERIOD = 2;
	public final static int CUSTOM_DAILY_PERIOD = 3;
	public final static int CUSTOM_WEEKLY_PERIOD = 4;
	public final static int CUSTOM_MONTHLY_PERIOD = 5;
	public final static int CUSTOM_YEARLY_PERIOD = 6;

	public int mPeriodicity;
	public int mPeriodicityUnit;
	public GregorianCalendar mEndDate;
	public long mAccountId;

	public ScheduledOperation(Cursor op) {
		super(op);
		mAccountId = op.getLong(op
				.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_ACCOUNT_ID));
		mEndDate = new GregorianCalendar();
		mEndDate.setTimeInMillis(op.getLong(op
				.getColumnIndex(OperationsDbAdapter.KEY_SCHEDULED_END_DATE)));
		Tools.clearTimeOfCalendar(mEndDate);
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
		if (unit < CUSTOM_DAILY_PERIOD) {
			return s;
		} else if (unit <= CUSTOM_YEARLY_PERIOD) {
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

	public boolean isObsolete() {
		return isObsolete(new GregorianCalendar().getTimeInMillis());
	}

	public boolean isObsolete(final long dateInMillis) {
		return (getEndDate() > 0) && (getEndDate() <= dateInMillis);
	}

	@Override
	public boolean equals(Operation op) {
		ScheduledOperation schOp = (ScheduledOperation) op;
		return super.equals(op) && mAccountId == schOp.mAccountId
				&& mEndDate.equals(schOp.mEndDate)
				&& mPeriodicity == schOp.mPeriodicity
				&& mPeriodicityUnit == schOp.mPeriodicityUnit;
	}
}
