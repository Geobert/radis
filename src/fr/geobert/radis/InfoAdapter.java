package fr.geobert.radis;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.AsciiUtils;

public class InfoAdapter extends CursorAdapter {
	private String mColName = null;
	private Uri mTableUri = null;
	private Activity mCtx;
	private String mCurrentConstraint;

	public InfoAdapter(Activity context, Uri tableUri, String colName) {
		super(context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mColName = colName;
		mTableUri = tableUri;
		mCtx = context;
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(mColName));
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String text = convertToString(cursor).toString();
		((TextView) view).setText(text);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final View view = inflater.inflate(
				android.R.layout.simple_dropdown_item_1line, parent, false);

		return view;
	}
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		mCurrentConstraint = constraint != null ? AsciiUtils.convertNonAscii(constraint.toString()) : null;
		if (getFilterQueryProvider() != null) {
			return getFilterQueryProvider().runQuery(constraint);
		}
		Cursor c = InfoTables.fetchMatchingInfo(mCtx, mTableUri, mColName,
				mCurrentConstraint);
		return c;
	}
}