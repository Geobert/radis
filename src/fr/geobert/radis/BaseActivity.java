package fr.geobert.radis;

import android.app.ProgressDialog;
import android.support.v4.app.FragmentActivity;

public class BaseActivity extends FragmentActivity {
	protected ProgressDialog mProgress;
	
	protected void showProgress() {
		if (mProgress == null) {
			mProgress = ProgressDialog.show(this, "", getString(R.string.loading));
		} else {
			mProgress.show();
		}
	}
	
	protected void hideProgress() {
		if (mProgress != null && mProgress.isShowing()) {
			mProgress.dismiss();
		}
	}
}
