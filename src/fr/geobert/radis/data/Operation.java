package fr.geobert.radis.data;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.db.OperationTable;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

public class Operation implements Parcelable {
	protected GregorianCalendar mDate;
	public String mThirdParty;
	public String mTag;
	public String mMode;
	public String mNotes;
	public long mSum;
	public long mScheduledId;
	public long mRowId;
	public String mTransSrcAccName;

	// if these value are != 0, it is a transfert operation between 2 accounts
	// mTransferAccountId is the other account
	public long mTransferAccountId;
	public long mAccountId;

	public Operation(Operation op) {
		mDate = new GregorianCalendar();
		mDate.setTimeInMillis(op.getDate());
		Tools.clearTimeOfCalendar(mDate);
		mThirdParty = op.mThirdParty;
		mTag = op.mTag;
		mMode = op.mMode;
		mNotes = op.mNotes;
		mSum = op.mSum;
		mScheduledId = op.mScheduledId;
		mTransferAccountId = op.mTransferAccountId;
		mTransSrcAccName = op.mTransSrcAccName;
		mAccountId = op.mAccountId;
	}

	public Operation(Cursor op) {
		mThirdParty = op.getString(op
				.getColumnIndexOrThrow(InfoTables.KEY_THIRD_PARTY_NAME));
		if (null == mThirdParty) {
			mThirdParty = "";
		}
		mMode = op.getString(op
				.getColumnIndexOrThrow(InfoTables.KEY_MODE_NAME));
		if (null == mMode) {
			mMode = "";
		}
		mTag = op.getString(op
				.getColumnIndexOrThrow(InfoTables.KEY_TAG_NAME));
		if (null == mTag) {
			mTag = "";
		}
		mSum = op.getLong(op.getColumnIndexOrThrow(OperationTable.KEY_OP_SUM));
		mDate = new GregorianCalendar();
		mDate.setTimeInMillis(op.getLong(op
				.getColumnIndexOrThrow(OperationTable.KEY_OP_DATE)));
		Tools.clearTimeOfCalendar(mDate);
		mNotes = op.getString(op
				.getColumnIndexOrThrow(OperationTable.KEY_OP_NOTES));
		final int idx = op.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID);
		if (idx >= 0) {
			mScheduledId = op.getLong(idx);
		} else {
			mScheduledId = 0;
		}
		final int transIdx = op
				.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID);
		if (transIdx >= 0) {
			mTransferAccountId = op.getLong(transIdx);
			mTransSrcAccName = op.getString(op
					.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME));
		}

		final int accIdx = op.getColumnIndex(OperationTable.KEY_OP_ACCOUNT_ID);
		if (accIdx >= 0) {
			mAccountId = op.getLong(accIdx);
		}
	}

	public Operation() {
		mDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(mDate);
		mSum = 0L;
		mThirdParty = "";
		mMode = "";
		mTag = "";
		mNotes = "";
		mScheduledId = 0;
		mTransferAccountId = 0;
		mAccountId = 0;
	}

	public Operation(Parcel parcel) {
		mDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(mDate);
		readFromParcel(parcel);
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
		return Formater.getFullDateFormater().format(mDate.getTime());
	}

	public String getShortDateStr() {
		return Formater.getShortDateFormater(null).format(mDate.getTime());
	}

	public long getDate() {
		return mDate.getTimeInMillis();
	}

	public void setDate(long date) {
		mDate.setTimeInMillis(date);
		Tools.clearTimeOfCalendar(mDate);
	}

	public void addDay(int nbDays) {
		mDate.add(Calendar.DAY_OF_MONTH, nbDays);
	}

	public void addMonth(int nbMonths) {
		mDate.add(Calendar.MONTH, nbMonths);
	}

	public void addYear(int nbYears) {
		mDate.add(Calendar.YEAR, nbYears);
	}

	public String getSumStr() {
		return Formater.getSumFormater().format(mSum / 100.0d);
	}

	public void setSumStr(String sumStr) {
		sumStr = sumStr.replace('+', ' ').trim();
		double d;
		try {
			d = Formater.getSumFormater().parse(sumStr).doubleValue();
		} catch (ParseException e) {
			d = 0d;
		}
		mSum = Math.round(d * 100);
	}

	public void setDateStr(String dateStr) throws ParseException {
		mDate.setTime(Formater.getFullDateFormater().parse(dateStr));

	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(getDay());
		dest.writeInt(getMonth());
		dest.writeInt(getYear());
		dest.writeString(mThirdParty);
		dest.writeString(mTag);
		dest.writeString(mMode);
		dest.writeString(mNotes);
		dest.writeLong(mSum);
		dest.writeLong(mScheduledId);
		dest.writeLong(mTransferAccountId);
	}

	protected void readFromParcel(Parcel in) {
		setDay(in.readInt());
		setMonth(in.readInt());
		setYear(in.readInt());

		mThirdParty = in.readString();
		if (null == mThirdParty) {
			mThirdParty = "";
		}

		mTag = in.readString();
		if (null == mTag) {
			mTag = "";
		}

		mMode = in.readString();
		mNotes = in.readString();
		if (null == mMode) {
			mMode = "";
		}

		mSum = in.readLong();
		mScheduledId = in.readLong();
		mTransferAccountId = in.readLong();
	}

	public static final Parcelable.Creator<Operation> CREATOR = new Parcelable.Creator<Operation>() {
		public Operation createFromParcel(Parcel in) {
			return new Operation(in);
		}

		public Operation[] newArray(int size) {
			return new Operation[size];
		}
	};

	public boolean equals(Operation op) {
		if (null == op) {
			return false;
		}
		return mDate.equals(op.mDate) && equalsButDate(op);
	}

	public boolean equalsButDate(Operation op) {
		if (null == op) {
			return false;
		}
		return mThirdParty.equals(op.mThirdParty) && mTag.equals(op.mTag)
				&& mMode.equals(op.mMode) && mNotes.equals(op.mNotes)
				&& mSum == op.mSum && mScheduledId == op.mScheduledId
				&& mTransferAccountId == op.mTransferAccountId;
	}
}
