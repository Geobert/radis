package fr.geobert.radis

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import fr.geobert.radis.data.AccountManager
import fr.geobert.radis.tools.UpdateDisplayInterface
import kotlin.properties.Delegates
import android.support.v7.widget.Toolbar

public abstract class BaseFragment : Fragment(), UpdateDisplayInterface, Toolbar.OnMenuItemClickListener {
    protected val mActivity: MainActivity by Delegates.lazy { getActivity() as MainActivity }
    protected val mAccountManager: AccountManager by Delegates.lazy { mActivity.mAccountManager }

    //    public abstract fun onRestoreInstanceState(savedInstanceState: Bundle)

    public abstract fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?)

    public abstract fun onAccountChanged(itemId: Long): Boolean

    public fun getName(): String {
        return this.javaClass.getName()
    }

    protected fun setIcon(id: Int) {
        mActivity.mToolbar.setNavigationIcon(id)
    }

    protected fun setMenu(id: Int) {
        mActivity.mToolbar.getMenu().clear()
        mActivity.mToolbar.inflateMenu(id)
        mActivity.mToolbar.setOnMenuItemClickListener(this)
    }
}
