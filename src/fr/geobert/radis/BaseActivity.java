package fr.geobert.radis;

import android.app.ProgressDialog;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.tools.DBPrefsManager;

public class BaseActivity extends SherlockFragmentActivity {
    private static final String TAG = "BaseActivity";
    protected ProgressDialog mProgress;
    private int mProgressCount = 0;
    protected AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountManager = new AccountManager();
    }

    public AccountManager getAccountManager() {
        return mAccountManager;
    }

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
    }

}
