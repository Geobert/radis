package fr.geobert.radis;

import java.text.ParseException;

import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
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
		mCustomPeriodicityVal = (EditText) findViewById(R.id.custom_periodicity_value);
		mCustomPeriodicityUnit = (Spinner) findViewById(R.id.custom_periodicity_choice);
		mCustomPeriodicityText = (TextView) findViewById(R.id.custom_periodicity_text);
		mEndDatePicker = (DatePicker) findViewById(R.id.edit_end_date);
		mEndDatePicker.setEnabled(false);
		CheckBox endDateCheck = (CheckBox) findViewById(R.id.end_date_check);
		endDateCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mEndDatePicker.setEnabled(isChecked);
				mEndDatePicker.clearFocus();
			}
		});
		endDateCheck.setChecked(false);

		// Gestures init
		mGesturelib = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if (!mGesturelib.load()) {
			Log.w("GestureActivity", "could not load gesture library");
			finish();
		}
		mGestureOverlay = (GestureOverlayView) findViewById(R.id.gesture_view);
		if (!Tools.DEBUG_MODE) {
			mGestureOverlay.setGestureVisible(false);
		}
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
	protected void populateFields() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveOpAndSetActivityResult() throws ParseException {
		// TODO Auto-generated method stub

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
