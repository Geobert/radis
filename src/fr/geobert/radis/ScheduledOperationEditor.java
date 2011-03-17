package fr.geobert.radis;

import java.text.ParseException;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.database.Cursor;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class ScheduledOperationEditor extends CommonOpEditor {
	private ViewFlipper mViewFlipper;
	private ViewFlipper mHeaderFlipper;
	private boolean mOnBasics = true;
	private GestureOverlayView mGestureOverlay;
	private GestureLibrary mGesturelib = null;
	private DatePicker mEndDatePicker;
	private Spinner mAccountSpinner;
	private Spinner mPeriodicitySpinner;
	private EditText mCustomPeriodicityVal;
	private Spinner mCustomPeriodicityUnit;
	private TextView mCustomPeriodicityText;
	private ScheduledOperation mCurrentSchOp;
	private View mCustomPeriodicityCont;
	private CheckBox mEndDateCheck;

	@Override
	protected void setView() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.scheduled_operation_edit);
	}

	@Override
	protected void initDbHelper() {
		mDbHelper = new OperationsDbAdapter(this, 0);
		mDbHelper.open();
	}

	// to be called after setContentView
	@Override
	protected void init(Bundle savedInstanceState) {
		super.init(savedInstanceState);
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		mHeaderFlipper = (ViewFlipper) findViewById(R.id.header_flipper);
		mAccountSpinner = (Spinner) findViewById(R.id.account_choice);
		mPeriodicitySpinner = (Spinner) findViewById(R.id.periodicity_choice);
		mCustomPeriodicityCont = findViewById(R.id.custom_periodicity);
		mCustomPeriodicityVal = (EditText) findViewById(R.id.custom_periodicity_value);
		mCustomPeriodicityVal
				.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							((EditText) v).selectAll();
						}
					}
				});
		mCustomPeriodicityUnit = (Spinner) findViewById(R.id.custom_periodicity_choice);
		mCustomPeriodicityText = (TextView) findViewById(R.id.custom_periodicity_text);
		mEndDatePicker = (DatePicker) findViewById(R.id.edit_end_date);
		mEndDatePicker.setEnabled(false);
		mEndDateCheck = (CheckBox) findViewById(R.id.end_date_check);
		mEndDateCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEndDatePicker.setEnabled(isChecked);
				mEndDatePicker.clearFocus();
			}
		});
		mEndDateCheck.setChecked(false);

		// Gestures init
		mGesturelib = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if (!mGesturelib.load()) {
			Log.w("GestureActivity", "could not load gesture library");
			finish();
		}
		mGestureOverlay = (GestureOverlayView) findViewById(R.id.gesture_view);
		mGestureOverlay.setGestureVisible(false);
		mGestureOverlay.addOnGesturePerformedListener(new ViewSwipeDetector(
				mGesturelib, new Runnable() {
					@Override
					public void run() {
						if (!mOnBasics) {
							mOnBasics = true;
							flip(mViewFlipper, true);
							flip(mHeaderFlipper, true);
						}
					}
				}, new Runnable() {
					@Override
					public void run() {
						if (mOnBasics) {
							mOnBasics = false;
							flip(mViewFlipper, false);
							flip(mHeaderFlipper, false);
						}
					}
				}));
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId != null) {
			Cursor opCursor = mDbHelper.fetchOneScheduledOp(mRowId);
			startManagingCursor(opCursor);
			mCurrentSchOp = new ScheduledOperation(opCursor);
		} else {
			mCurrentSchOp = new ScheduledOperation();
		}
	}

	@Override
	protected void populateFields() {
		ScheduledOperation op = mCurrentSchOp;
		mCurrentOp = op;
		populateCommonFields(op);
		mCustomPeriodicityVal.setText(mCurrentSchOp.mPeriodicity == 0 ? ""
				: Integer.toString(mCurrentSchOp.mPeriodicity));
		populateAccountSpinner();
		poputatePeriodicitySpinner();
		populateCustomPeriodicitySpinner();
		if (mCurrentSchOp.getEndDate() > 0) {
			mEndDateCheck.setChecked(true);
			mEndDatePicker.setEnabled(true);
			mEndDatePicker.updateDate(op.getEndYear(), op.getEndMonth(),
					op.getEndDay());
		} else {
			mEndDateCheck.setChecked(false);
			mEndDatePicker.setEnabled(false);
		}
	}

	private void populateCustomPeriodicitySpinner() {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.periodicity_custom_choices,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCustomPeriodicityUnit.setAdapter(adapter);

		int unit = mCurrentSchOp.mPeriodicityUnit;
		if (unit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) {
			mCustomPeriodicityUnit.setSelection(unit
					- ScheduledOperation.CUSTOM_DAILY_PERIOD);
		}
	}

	private void poputatePeriodicitySpinner() {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.periodicity_choices,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPeriodicitySpinner.setAdapter(adapter);
		mPeriodicitySpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						setCustomPeriodicityVisibility(pos == (parent
								.getAdapter().getCount() - 1));
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
		int unit = mCurrentSchOp.mPeriodicityUnit;
		if (unit < ScheduledOperation.CUSTOM_DAILY_PERIOD) {
			mPeriodicitySpinner.setSelection(unit);
		}
	}

	private void populateAccountSpinner() {
		Cursor c = mDbHelper.fetchAllAccounts();
		startManagingCursor(c);
		if (c.isFirst()) {
			String[] from = new String[] {
					OperationsDbAdapter.KEY_ACCOUNT_NAME,
					OperationsDbAdapter.KEY_ACCOUNT_ROWID,
					OperationsDbAdapter.KEY_ACCOUNT_CURRENCY };

			int[] to = new int[] { android.R.id.text1 };
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
					android.R.layout.simple_spinner_item, c, from, to);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mAccountSpinner.setAdapter(adapter);
			if (mCurrentSchOp.mAccountId != 0) {
				int pos = -1;
				boolean found = false;
				do {
					pos = pos + 1;
					found = adapter.getItemId(pos) == mCurrentSchOp.mAccountId;
				} while (!found && pos < adapter.getCount());
				if (found) {
					mAccountSpinner.setSelection(pos);
				}
			}
		}
	}

	private void setCustomPeriodicityVisibility(final boolean isVisible) {
		mCustomPeriodicityCont.setVisibility(isVisible ? View.VISIBLE
				: View.GONE);
	}

	@Override
	protected void saveOpAndSetActivityResult() throws ParseException {
		ScheduledOperation op = mCurrentSchOp;
		if (mRowId == null) {
			long id = mDbHelper.createScheduledOp(op);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateScheduledOp(mRowId, op);
		}
		Intent res = new Intent();
		setResult(RESULT_OK, res);
	}

	@Override
	protected void fillOperationWithInputs(Operation operation)
			throws ParseException {
		super.fillOperationWithInputs(operation);
		ScheduledOperation op = (ScheduledOperation) operation;
		op.mAccountId = mAccountSpinner.getSelectedItemId();
		final boolean isCustom = mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner
				.getAdapter().getCount() - 1);
		if (!isCustom) {
			op.mPeriodicity = 1;
			op.mPeriodicityUnit = mPeriodicitySpinner.getSelectedItemPosition();
		} else {
			op.mPeriodicity = Integer.parseInt(mCustomPeriodicityVal.getText()
					.toString());
			op.mPeriodicityUnit = mCustomPeriodicityUnit
					.getSelectedItemPosition()
					+ ScheduledOperation.CUSTOM_DAILY_PERIOD;
		}
		if (mEndDatePicker.isEnabled()) {
			DatePicker dp = mEndDatePicker;
			dp.clearChildFocus(getCurrentFocus());
			op.setEndDay(dp.getDayOfMonth());
			op.setEndMonth(dp.getMonth());
			op.setEndYear(dp.getYear());

		} else {
			op.mEndDate.clear();
		}
	}

	@Override
	protected boolean isFormValid(StringBuilder errMsg) throws ParseException {
		boolean res = super.isFormValid(errMsg);
		ScheduledOperation op = mCurrentSchOp;
		fillOperationWithInputs(op);
		final boolean hasEnd = op.getEndDate() > 0;
		if (hasEnd && (op.getDate() > op.getEndDate())) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append(getString(R.string.end_date_incorrect));
			res = false;
		}
		if ((op.mPeriodicityUnit >= ScheduledOperation.CUSTOM_DAILY_PERIOD)
				&& op.mPeriodicity <= 0) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append(getString(R.string.periodicity_must_be_greater_0));
			res = false;
		}
		return res;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isOnBasics", mOnBasics);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		mCurrentSchOp = savedInstanceState.getParcelable("currentOp");
		mOnBasics = savedInstanceState.getBoolean("isOnBasics");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (!mOnBasics) {
			mViewFlipper.showNext();
			mHeaderFlipper.showNext();
		}
		super.onResume();
	}

	private void flip(ViewFlipper flipper, final boolean l2r) {
		if (l2r) {
			flipper.setInAnimation(ScheduledOperationEditor.this,
					R.anim.enter_from_left);
			flipper.setOutAnimation(ScheduledOperationEditor.this,
					R.anim.exit_by_right);
			flipper.showPrevious();
		} else {
			flipper.setInAnimation(ScheduledOperationEditor.this,
					R.anim.enter_from_right);
			flipper.setOutAnimation(ScheduledOperationEditor.this,
					R.anim.exit_by_left);
			flipper.showNext();
		}
	}

}
