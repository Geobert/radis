package fr.geobert.radis;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.tools.UpdateDisplayInterface;

public abstract class BaseFragment extends Fragment implements UpdateDisplayInterface {
    protected MainActivity mActivity;
    protected AccountManager mAccountManager;
//    public boolean doOnResumeWhenReady = false;

    protected void baseInit() {
        this.mActivity = (MainActivity) getActivity();
        this.mAccountManager = mActivity.getAccountManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        baseInit();
        return null;
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        if (doOnResumeWhenReady) {
//            doOnResumeWhenReady = false;
//            doOnResume();
//        }
//    }
//
//    public abstract void processOnResume();
//
//    public final void doOnResume() {
//        if (isAdded()) {
//            processOnResume();
//        } else {
//            doOnResumeWhenReady = true;
//        }
//    }

    public abstract void onRestoreInstanceState(Bundle savedInstanceState);

    public abstract boolean onAccountChanged(long itemId);

    public String getName() {
        return this.getClass().getName();
    }
}
