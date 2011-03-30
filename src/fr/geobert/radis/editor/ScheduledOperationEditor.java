package fr.geobert.radis.editor;

import java.text.ParseException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.ViewSwipeDetector;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.db.OperationsDbAdapter;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.tools.Tools;

public class ScheduledOperationEditor extends CommonOpEditor {
	private ViewFlipper mViewFlipper;
	private boolean mOnBasics = true;
	private GestureOverlayView mGestureOverlay;
	private GestureLibrary mGesturelib = null;
	private DatePicker mEndDatePicker;
	private Spinner mAccountSpinner;
	private Spinner mPeriodicitySpinner;
	private EditText mCustomPeriodicityVal;
	private Spinner mCustomPeriodicityUnit;
	private ScheduledOperation mCurrentSchOp;
	private View mCustomPeriodicityCont;
	private CheckBox mEndDateCheck;
	private ScheduledOperation mOriginalSchOp;
	private long mOpIdSource;

	protected static final int ASK_UPDATE_OCCURENCES_DIALOG_ID = 10;

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
		initWidgetReferences();
		initWidgetBehavior();
		initGesture();
		
		Bundle extras = getIntent().getExtras();
		mOpIdSource = extras.getLong("operationId");
	}

	private void initWidgetReferences() {
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		mAccountSpinner = (Spinner) findViewById(R.id.account_choice);
		mPeriodicitySpinner = (Spinner) findViewById(R.id.periodicity_choice);
		mCustomPeriodicityCont = findViewById(R.id.custom_periodicity);
		mCustomPeriodicityVal = (EditText) findViewById(R.id.custom_periodicity_value);
		mCustomPeriodicityUnit = (Spinner) findViewById(R.id.custom_periodicity_choice);
		mEndDatePicker = (DatePicker) findViewById(R.id.edit_end_date);
		mEndDateCheck = (CheckBox) findViewById(R.id.end_date_check);
	}

	private void initWidgetBehavior() {
		mCustomPeriodicityVal
				.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							((EditText) v).selectAll();
						}
					}
				});

		mEndDatePicker.setEnabled(false);

		mEndDateCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEndDatePicker.setEnabled(isChecked);
				mEndDatePicker.clearFocus();
			}
		});
		mEndDateCheck.setChecked(false);

		Button but = (Button) findViewById(R.id.GoToSchedulingBtn);
		but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mOnBasics) {
					mOnBasics = false;
					flip(mViewFlipper, false);
				}
			}
		});
		but = (Button) findViewById(R.id.GoToBasicsBtn);
		but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!mOnBasics) {
					mOnBasics = true;
					flip(mViewFlipper, true);
				}
			}
		});
	}

	private void initGesture() {
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
						}
					}
				}, new Runnable() {
					@Override
					public void run() {
						if (mOnBasics) {
							mOnBasics = false;
							flip(mViewFlipper, false);
						}
					}
				}));
	}

	@Override
	protected void fetchOrCreateCurrentOp() {
		if (mRowId != 0) {
			Cursor opCursor = mDbHelper.fetchOneScheduledOp(mRowId);
			startManagingCursor(opCursor);
			mCurrentSchOp = new ScheduledOperation(opCursor);
			mOriginalSchOp = new ScheduledOperation(opCursor);
		} else {
			if (mOpIdSource > 0) {
				Bundle extras = getIntent().getExtras();
				final long accountId = extras.getLong(Tools.EXTRAS_ACCOUNT_ID);
				Cursor op = mDbHelper.fetchOneOp(mOpIdSource, accountId);
				mCurrentSchOp = new ScheduledOperation(op, accountId);
				mOriginalSchOp = new ScheduledOperation(op, accountId);
				op.close();
			} else {
				mCurrentSchOp = new ScheduledOperation();
			}
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

	private void startInsertionServiceAndExit() {
		RadisService.acquireStaticLock(this);
		this.startService(new Intent(this, RadisService.class));
		Intent res = new Intent();
		if (mOpIdSource > 0) {
			res.putExtra("schOperationId", mRowId);
			res.putExtra("opIdSource", mOpIdSource);
		}	
		setResult(RESULT_OK, res);
		finish();
	}

	@Override
	protected void saveOpAndExit() throws ParseException {
		ScheduledOperation op = mCurrentSchOp;
		if (mRowId == 0) {
			if ((mOpIdSource > 0) && (op.getDate() == mOriginalSchOp.getDate())) {
				// do not insert another occurence with same date
				ScheduledOperation.addPeriodicityToDate(op);
			}
			long id = mDbHelper.createScheduledOp(op);
			if (id > 0) {
				mRowId = id;
			}
			startInsertionServiceAndExit();
		} else {
			if (!op.equals(mOriginalSchOp)) {
				mDbHelper.updateScheduledOp(mRowId, op, false);
				showDialog(ASK_UPDATE_OCCURENCES_DIALOG_ID);
			} else { // nothing to update
				Intent res = new Intent();
				setResult(RESULT_OK, res);
				finish();
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ASK_UPDATE_OCCURENCES_DIALOG_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.ask_update_occurences)
					.setCancelable(false)
					.setPositiveButton(R.string.update,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									ScheduledOperationEditor
											.updateAllOccurences(mDbHelper,
													mCurrentSchOp,
													mPreviousSum, mRowId);
									startInsertionServiceAndExit();
								}
							})
					.setNeutralButton(R.string.disconnect,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mDbHelper.disconnectAllOccurrences(
											mCurrentSchOp.mAccountId, mRowId);
									startInsertionServiceAndExit();
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			return builder.create();
		default:
			return super.onCreateDialog(id);
		}

	}

	public static void updateAllOccurences(OperationsDbAdapter dbHelper,
			final ScheduledOperation op, final double prevSum, final long rowId) {
		final long accountId = op.mAccountId;
		int nbUpdated = dbHelper.updateAllOccurrences(accountId, rowId, op);
		double sumToAdd = nbUpdated * (op.mSum - prevSum);
		Cursor accountCursor = dbHelper.fetchAccount(accountId);
		double curSum = accountCursor.getDouble(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
		dbHelper.updateOpSum(accountId, curSum + sumToAdd);
		Cursor opCur = dbHelper.fetchNLastOps(1, accountId);
		if (null != opCur) {
			opCur.moveToFirst();
		}
		final long date = opCur.getLong(opCur
				.getColumnIndex(OperationsDbAdapter.KEY_OP_DATE));
		final long curDate = accountCursor.getLong(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE));
		if (date > curDate) {
			dbHelper.updateCurrentSum(accountId, date);
		} else {
			dbHelper.updateCurrentSum(accountId, 0);
		}
		accountCursor.close();
		opCur.close();
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
		outState.putParcelable("originalOp", mOriginalSchOp);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		mCurrentSchOp = savedInstanceState.getParcelable("currentOp");
		mOnBasics = savedInstanceState.getBoolean("isOnBasics");
		mOriginalSchOp = savedInstanceState.getParcelable("originalOp");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (!mOnBasics) {
			mViewFlipper.showNext();
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
