package fr.geobert.radis.tools;

import java.text.ParseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import fr.geobert.radis.R;
import fr.geobert.radis.db.CommonDbAdapter;

public class ProjectionDateController {
	private Spinner mProjectionMode;
	public EditText mProjectionDate;
	private Activity mActivity;
	private CommonDbAdapter mDbHelper;
	private long mAccountId;
	private int mOrigProjMode;
	private String mOrigProjDate;
	private int mCurPos;

	protected static ProjectionDateController mInstance;

	public ProjectionDateController(Activity activity) {
		mActivity = activity;
		mProjectionDate = (EditText) activity
				.findViewById(R.id.projection_date_value);
		mProjectionMode = (Spinner) activity
				.findViewById(R.id.projection_date_spinner);
		initViews();
	}

	public ProjectionDateController(View layout, Activity activity) {
		mActivity = activity;
		mProjectionDate = (EditText) layout
				.findViewById(R.id.projection_date_value);
		mProjectionMode = (Spinner) layout
				.findViewById(R.id.projection_date_spinner);
		initViews();
	}

	public void initViews() {
		ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(mActivity,
				R.array.projection_modes, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mProjectionMode.setAdapter(adapter);
		mProjectionMode.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				mProjectionDate.setEnabled(pos > 0);
				if (pos != mCurPos) {
					mProjectionDate.setText("");
				}
				ProjectionDateController.this.setHint(pos);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
	}

	protected void setHint(int pos) {
		CharSequence hint = "";
		switch (pos) {
		case 0:
			hint = "";
			break;
		case 1:
			hint = mActivity.getString(R.string.projection_day_of_month);
			break;
		case 2:
			hint = mActivity.getString(R.string.projection_full_date);
			break;
		}
		mProjectionDate.setHint(hint);
	}

	public void setDbHelper(CommonDbAdapter dbHelper) {
		mDbHelper = dbHelper;
	}

	public void populateFields(Cursor account) {
		mAccountId = account.getLong(account
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_ROWID));
		int pos = account.getInt(account
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_PROJECTION_MODE));
		mCurPos = pos;
		setHint(pos);
		mOrigProjMode = pos;
		mProjectionMode.setSelection(pos);
		mOrigProjDate = account.getString(account
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_PROJECTION_DATE));
		mProjectionDate.setEnabled(pos > 0);
		mProjectionDate.setText(mOrigProjDate);
	}

	public int getMode() {
		return mProjectionMode.getSelectedItemPosition();
	}

	public String getDate() {
		return mProjectionDate.getText().toString();
	}

	public boolean hasChanged() {
		return mOrigProjMode != mProjectionMode.getSelectedItemPosition()
				|| (mOrigProjMode != 0 && !mOrigProjDate.equals(mProjectionDate
						.getText().toString()));
	}

	public static AlertDialog getDialog(Activity activity,
			CommonDbAdapter dbHelper) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		LayoutInflater inflater = (LayoutInflater) activity.getLayoutInflater();
		View layout = inflater.inflate(R.layout.projection_date_dialog, null);
		builder.setPositiveButton(activity.getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						mInstance.saveProjectionDate();
					}
				}).setNegativeButton(activity.getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				}).setTitle(R.string.projection_date);
		builder.setView(layout);
		AlertDialog dialog = builder.create();
		mInstance = new ProjectionDateController(layout, activity);
		mInstance.setDbHelper(dbHelper);

		return dialog;
	}

	public static void onPrepareDialog(Cursor account) {
		mInstance.populateFields(account);
	}

	protected void saveProjectionDate() {
		try {
			mDbHelper.updateAccountProjectionDate(mAccountId, mInstance);
			if (mActivity instanceof UpdateDisplayInterface) {
				((UpdateDisplayInterface) mActivity).updateDisplay(null);
			}
		} catch (ParseException e) {
			Tools.popError(mActivity,
					mActivity.getString(R.string.bad_format_for_date), null);
			e.printStackTrace();
		} catch (NumberFormatException e) {
			Tools.popError(mActivity,
					mActivity.getString(R.string.bad_format_for_date), null);
			e.printStackTrace();
		}
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("projectionMode",
				mProjectionMode.getSelectedItemPosition());
		outState.putString("projectionDate", mProjectionDate.getText()
				.toString());
		outState.putInt("origProjMode", mOrigProjMode);
		outState.putString("origProjDate", mOrigProjDate);
	}

	public void onRestoreInstanceState(Bundle state) {
		mProjectionMode.setSelection(state.getInt("projectionMode"));
		mProjectionDate.setText(state.getString("projectionDate"));
		mOrigProjDate = state.getString("origProjDate");
		mOrigProjMode = state.getInt("origProjMode");
	}
}
