package fr.geobert.radis;

import android.app.Activity;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ViewFlipper;

public class ScheduledOperationEditor extends Activity {
	private Long mRowId;
	private OperationsDbAdapter mDbHelper;
	private ViewFlipper mViewFlipper;
	private ViewFlipper mHeaderFlipper;
	private boolean mOnBasics = true;
	private GestureOverlayView mGestureOverlay;
	private GestureLibrary mGesturelib = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Bundle extras = getIntent().getExtras();
		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState.getSerializable(Tools.EXTRAS_OP_ID);
		if (mRowId == null) {
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_OP_ID) : null;
			if (mRowId == -1) {
				mRowId = null;
			}
		}
		mDbHelper = new OperationsDbAdapter(this, 0);
		mDbHelper.open();
		setContentView(R.layout.scheduled_operation_edit);

		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		mHeaderFlipper = (ViewFlipper) findViewById(R.id.header_flipper);

		mGesturelib = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if (!mGesturelib.load()) {
			Log.w("GestureActivity", "could not load gesture library");

			finish();
		}

		mGestureOverlay = (GestureOverlayView) findViewById(R.id.gesture_view);
		// mGestureOverlay.setGestureVisible(false);
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
