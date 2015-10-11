package fr.geobert.radis

import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import fr.geobert.radis.data.AccountManager
import fr.geobert.radis.tools.DBPrefsManager

public abstract class BaseActivity : AppCompatActivity() {
    protected var mProgress: ProgressDialog? = null
    private var mProgressCount = 0
    public val mAccountManager: AccountManager by lazy(LazyThreadSafetyMode.NONE) { AccountManager(this) }
    public val mToolbar: Toolbar by lazy(LazyThreadSafetyMode.NONE) { findViewById(R.id.my_toolbar) as Toolbar }

    protected fun showProgress() {
        mProgressCount++
        if (mProgress == null) {
            mProgress = ProgressDialog.show(this, "", getString(R.string.please_wait))
        } else {
            mProgress!!.show()
        }
    }

    protected fun hideProgress() {
        mProgressCount--
        if (mProgress != null && mProgress!!.isShowing && mProgressCount <= 0) {
            mProgressCount = 0
            mProgress!!.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        DBPrefsManager.getInstance(this).fillCache(this)
    }

    public fun setIcon(id: Int) {
        mToolbar.setNavigationIcon(id)
    }

    public fun setIconOnClick(listener: View.OnClickListener) {
        mToolbar.setNavigationOnClickListener(listener)
    }

    public fun setMenu(id: Int) {
        mToolbar.inflateMenu(id)
    }

    override fun setTitle(title: CharSequence?) {
        mToolbar.title = title
    }

    override fun setTitle(titleId: Int) {
        mToolbar.setTitle(titleId)
    }

    override fun setTitleColor(textColor: Int) {
        mToolbar.setTitleTextColor(textColor)
    }
}
