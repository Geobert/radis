package fr.geobert.radis;

import java.util.Date;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import fr.geobert.radis.tools.Formater;

public class OpViewBinder implements SimpleCursorAdapter.ViewBinder {
	protected Resources mRes;
	private CharSequence mSumColName;
	protected CharSequence mDateColName;
	private int mArrowIconId;

	public OpViewBinder(Activity context, CharSequence sumColName, CharSequence dateColName, int arrowIconId) {
		mRes = context.getResources();
		mSumColName = sumColName;
		mDateColName = dateColName;
		mArrowIconId = arrowIconId;
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		String colName = cursor.getColumnName(columnIndex);
		if (colName.equals(mSumColName)) {
			TextView textView = ((TextView) view);
			ImageView i = (ImageView) ((LinearLayout) view.getParent()
					.getParent()).findViewById(mArrowIconId);
			long sum = cursor.getLong(columnIndex);
			if (sum >= 0.0) {
				textView.setTextColor(mRes.getColor(R.color.positiveSum));
				i.setImageResource(R.drawable.arrow_up16);
			} else {
				textView.setTextColor(mRes.getColor(R.color.text_color));
				i.setImageResource(R.drawable.arrow_down16);
			}
			String txt = Formater.getSumFormater().format(sum / 100.0d);
			textView.setText(txt);
			return true;
		} else if (colName.equals(mDateColName)) {
			Date date = new Date(cursor.getLong(columnIndex));
			((TextView) view).setText(Formater.getShortDateFormater(null)
					.format(date));
			return true;
		}
		return false;
	}

}
