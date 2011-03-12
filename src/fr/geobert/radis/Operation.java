package fr.geobert.radis;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Operation implements Parcelable {
	protected GregorianCalendar mDate;
	public String mThirdParty;
	public String mTag;
	public String mMode;
	public String mNotes;
	public double mSum;

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
		mNotes = op.getString(op
				.getColumnIndexOrThrow(OperationsDbAdapter.KEY_OP_NOTES));
	}

	public Operation() {
		mDate = new GregorianCalendar();
		mSum = 0.0d;
		mThirdParty = "";
		mMode = "";
		mTag = "";
		mNotes = "";
	}
	
	public Operation(Parcel parcel) {
		mDate = new GregorianCalendar();
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
		return Formater.DATE_FORMAT.format(mDate.getTime());
	}

	public String getShortDateStr() {
		return Formater.SHORT_DATE_FORMAT.format(mDate.getTime());
	}

	public long getDate() {
		return mDate.getTimeInMillis();
	}

	public void setDate(long date) {
		mDate.setTimeInMillis(date);
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
		return Formater.SUM_FORMAT.format(mSum);
	}

	public void setSumStr(String sumStr) throws ParseException {
		mSum = Formater.SUM_FORMAT.parse(sumStr).doubleValue();
	}

	public void setDateStr(String dateStr) throws ParseException {
		mDate.setTime(Formater.DATE_FORMAT.parse(dateStr));

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
		dest.writeDouble(mSum);
		
	}
	
	private void readFromParcel(Parcel in) {
        setDay(in.readInt());
        setMonth(in.readInt());
        setYear(in.readInt());
        mThirdParty = in.readString();
        mTag = in.readString();
        mMode = in.readString();
        mNotes = in.readString();
        mSum = in.readDouble();
    }
	
	public static final Parcelable.Creator<Operation> CREATOR =
    	new Parcelable.Creator<Operation>() {
            public Operation createFromParcel(Parcel in) {
                return new Operation(in);
            }
 
            public Operation[] newArray(int size) {
                return new Operation[size];
            }
        };
}
