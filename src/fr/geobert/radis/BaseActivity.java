package fr.geobert.radis;

import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.DBPrefsManager;
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
	
	@Override
	protected void onResume() {
		super.onResume();
		DBPrefsManager.getInstance(this).fillCache(this);
		InfoTables.fillCachesSync(this);
	}
}
