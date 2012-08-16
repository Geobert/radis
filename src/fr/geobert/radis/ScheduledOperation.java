package fr.geobert.radis;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import fr.geobert.radis.db.ScheduledOperationTable;
import fr.geobert.radis.tools.Tools;

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

	public ScheduledOperation(Operation op, final long accountId) {
		super(op);
		mAccountId = accountId;
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
	}

	public ScheduledOperation(Cursor op, final long accountId) {
		super(op);
		mAccountId = accountId;
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
	}

	public ScheduledOperation(Cursor op) {
		super(op);
		mAccountId = op.getLong(op
				.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID));
		mEndDate = new GregorianCalendar();
		mEndDate.setTimeInMillis(op.getLong(op
				.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_END_DATE)));
		Tools.clearTimeOfCalendar(mEndDate);
		mPeriodicity = op.getInt(op
				.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY));
		mPeriodicityUnit = op
				.getInt(op
						.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT));
	}

	public ScheduledOperation() {
		super();
		mAccountId = 0;
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
		mPeriodicity = 0;
		mPeriodicityUnit = 1; // default periodicity to MONTHLY, happens more
								// often
	}

	public ScheduledOperation(Parcel parcel) {
		super(parcel);
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

	@Override
	protected void readFromParcel(Parcel in) {
		Log.d("Radis", "schedule operation readFromParcel");
		super.readFromParcel(in);
		mPeriodicity = in.readInt();
		mPeriodicityUnit = in.readInt();
		mAccountId = in.readLong();
		mEndDate = new GregorianCalendar();
		mEndDate.clear();
		setEndDay(in.readInt());
		setEndMonth(in.readInt());
		setEndYear(in.readInt());
	}

	public static final Parcelable.Creator<ScheduledOperation> CREATOR = new Parcelable.Creator<ScheduledOperation>() {
		public ScheduledOperation createFromParcel(Parcel in) {
			return new ScheduledOperation(in);
		}

		public ScheduledOperation[] newArray(int size) {
			return new ScheduledOperation[size];
		}
	};

	public boolean isObsolete() {
		return (getEndDate() > 0) && (getEndDate() <= getDate());
	}

	@Override
	public boolean equals(Operation op) {
		if (null == op) {
			return false;
		}
		ScheduledOperation schOp = (ScheduledOperation) op;
		return super.equals(op) && mAccountId == schOp.mAccountId
				&& mEndDate.equals(schOp.mEndDate)
				&& mPeriodicity == schOp.mPeriodicity
				&& mPeriodicityUnit == schOp.mPeriodicityUnit;
	}

	public boolean periodicityEquals(ScheduledOperation schOp) {
		return mPeriodicity == schOp.mPeriodicity
				&& mPeriodicityUnit == schOp.mPeriodicityUnit;
	}

	public static void deleteAllOccurences(
			final long schOpId) {
//		Cursor schOp = dbHelper.fetchOneScheduledOp(schOpId);
//		final long accountId = schOp.getLong(schOp
//				.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID));
//		dbHelper.deleteAllOccurrences(accountId, schOpId);
//		dbHelper.consolidateSums(accountId, null);
//		if (null != schOp) {
//			schOp.close();
//		}
	}

	public static void updateAllOccurences(
			final ScheduledOperation op, final long prevSum, final long rowId) {
		// final long accountId = op.mAccountId;
		// dbHelper.updateAllOccurrences(accountId, rowId, op);
		// dbHelper.consolidateSums(accountId, null);
	}

	static public void addPeriodicityToDate(ScheduledOperation op) {
		switch (op.mPeriodicityUnit) {
		case ScheduledOperation.WEEKLY_PERIOD:
			op.addDay(7);
			break;
		case ScheduledOperation.MONTHLY_PERIOD:
			op.addMonth(1);
			break;
		case ScheduledOperation.YEARLY_PERIOD:
			op.addYear(1);
			break;
		case ScheduledOperation.CUSTOM_DAILY_PERIOD:
			op.addDay(op.mPeriodicity);
			break;
		case ScheduledOperation.CUSTOM_WEEKLY_PERIOD:
			op.addDay(7 * op.mPeriodicity);
			break;
		case ScheduledOperation.CUSTOM_MONTHLY_PERIOD:
			op.addMonth(op.mPeriodicity);
			break;
		case ScheduledOperation.CUSTOM_YEARLY_PERIOD:
			op.addYear(op.mPeriodicity);
			break;
		}
	}

}
