package fr.geobert.radis;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.AsciiUtils;

public class InfoAdapter extends CursorAdapter {
	private String mColName = null;
	private String mTableName = null;
	private String mCurrentConstraint;
	private Activity mCtx;
	private CommonDbAdapter mDbHelper;

	// private String boldFormat = "<u><b>$1</b></u>";

	public InfoAdapter(Activity context, CommonDbAdapter dBHelper,
			String tableName, String colName) {
		super(context, null);
		mColName = colName;
		mTableName = tableName;
		mCtx = context;
		mDbHelper = dBHelper;
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(mColName));
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		mCurrentConstraint = constraint != null ? AsciiUtils.convertNonAscii(constraint.toString()) : null;
		if (getFilterQueryProvider() != null) {
			return getFilterQueryProvider().runQuery(constraint);
		}
		Cursor c = mDbHelper.fetchMatchingInfo(mTableName, mColName,
				mCurrentConstraint);
		mCtx.startManagingCursor(c);
		return c;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String text = convertToString(cursor).toString();
		// if (mCurrentConstraint != null) {
		// text = text.replaceAll("(?i)(" + mCurrentConstraint + ")",
		// boldFormat);
		// }
		((TextView) view).setText(text);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(
				android.R.layout.simple_dropdown_item_1line, parent, false);

		return view;
	}
}