package fr.geobert.radis;

import android.app.ProgressDialog;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.DBPrefsManager;

public class BaseActivity extends SherlockFragmentActivity {
    private static final String TAG = "BaseActivity";
    protected ProgressDialog mProgress;
    private int mProgressCount = 0;

    protected void showProgress() {
        mProgressCount++;
        if (mProgress == null) {
            mProgress = ProgressDialog.show(this, "", getString(R.string.loading));
        } else {
            mProgress.show();
        }
    }

    protected void hideProgress() {
        mProgressCount--;
        if (mProgress != null && mProgress.isShowing() && mProgressCount <= 0) {
            mProgressCount = 0;
            mProgress.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DBPrefsManager.getInstance(this).fillCache(this);
        InfoTables.fillCachesSync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        InfoTables.clearCache();
    }
}
