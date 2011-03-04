package fr.geobert.radis;

import java.util.ArrayList;

import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;

public class ViewSwipeDetector implements
		GestureOverlayView.OnGesturePerformedListener {
	private Runnable mLToR;
	private Runnable mRToL;
	private GestureLibrary mGesturelib;

	public ViewSwipeDetector(GestureLibrary gesturelib, Runnable lToR,
			Runnable rToL) {
		mLToR = lToR;
		mRToL = rToL;
		mGesturelib = gesturelib;
	}

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = mGesturelib.recognize(gesture);
		if (predictions.size() > 0 && predictions.get(0).score > 5.0) {
			Prediction prediction = predictions.get(0);
			if (prediction.name.equals("LeftToRight")) {
				if (null != mLToR) {
					mLToR.run();
				}
			} else if (prediction.name.equals("RightToLeft")) {
				if (null != mRToL) {
					mRToL.run();
				}
			}
		}
	}
}
