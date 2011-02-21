package fr.geobert.radis;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.ListView;

public class SwipeDetector extends SimpleOnGestureListener {
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private ListSwipeAction mLToR;
	private ListSwipeAction mRToL;
	private ListView mListView;

	public SwipeDetector(ListView l, ListSwipeAction lToR, ListSwipeAction rToL) {
		mLToR = lToR;
		mRToL = rToL;
		mListView = l;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		try {
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;
			ListView l = mListView;
			int adapterIndex = l.pointToPosition((int) e1.getX(),(int) e1.getY());
			long rowId = l.pointToRowId((int) e1.getX(),(int) e1.getY());
			
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) { // <--
				if (null != mRToL) {
					mRToL.mPosition = adapterIndex;
					mRToL.mRowId = rowId;
					mRToL.run();
				}
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) { // -->
				if (null != mLToR) {
					mLToR.mPosition = adapterIndex;
					mLToR.mRowId = rowId;
					mLToR.run();
				}
			}
		} catch (Exception e) {
			// nothing
		}
		return false;

	}
}
