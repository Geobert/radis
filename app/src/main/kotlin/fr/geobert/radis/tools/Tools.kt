package fr.geobert.radis.tools

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentProviderClient
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.DialogFragment
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.DbHelper
import fr.geobert.radis.service.InstallRadisServiceReceiver
import fr.geobert.radis.service.RadisService
import fr.geobert.radis.ui.OperationListFragment
import fr.geobert.radis.ui.adapter.InfoAdapter

import java.util.Calendar
import java.util.GregorianCalendar

public object Tools {
    // Intents actions
    public val INTENT_RADIS_STARTED: String = "fr.geobert.radis.STARTED"
    public val INTENT_REFRESH_NEEDED: String = "fr.geobert.radis.REFRESH_NEEDED"
    public val INTENT_REFRESH_STAT: String = "fr.geobert.radis.REFRESH_STAT"

    public val DEBUG_DIALOG: Int = 9876
    // debug mode stuff
    public var DEBUG_MODE: Boolean = true
    //    public static int SCREEN_HEIGHT;
    private var mActivity: Activity? = null

    public fun checkDebugMode(ctx: Activity) {
        // See if we're a debug or a release build
        try {
            val packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_CONFIGURATIONS)
            val flags = packageInfo.applicationInfo.flags
            DEBUG_MODE = (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e1: NameNotFoundException) {
            e1.printStackTrace()
        }


    }

    public fun setViewBg(v: View, drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= 16) {
            v.setBackground(drawable)
        } else {
            //noinspection deprecation
            v.setBackgroundDrawable(drawable)
        }

    }

    //    public fun createRestartClickListener(ctx: Context): DialogInterface.OnClickListener {
    //        return object : DialogInterface.OnClickListener {
    //            override fun onClick(dialog: DialogInterface, which: Int) {
    //                Tools.restartApp(ctx)
    //            }
    //        }
    //    }

    public fun popMessage(ctx: Activity, msg: String, titleStrId: Int, btnText: String, onClick: ((d: DialogInterface, i: Int) -> Unit)?) {
        val alertDialog = AlertDialog.Builder(ctx).create()
        alertDialog.setTitle(titleStrId)
        alertDialog.setMessage(msg)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, btnText, onClick)
        alertDialog.show()
    }

    public fun popError(ctx: Activity, msg: String, onClick: ((d: DialogInterface, i: Int) -> Unit)?) {
        Tools.popMessage(ctx, msg, R.string.error, ctx.getString(R.string.ok), onClick)
    }

    SuppressWarnings("ConstantConditions")
    public fun setTextWithoutComplete(v: AutoCompleteTextView, text: String) {
        val adapter = v.getAdapter() as InfoAdapter
        v.setAdapter(null)
        v.setText(text)
        v.setAdapter(adapter)
    }

    public fun createDeleteConfirmationDialog(ctx: Context, onClick: ((d: DialogInterface, i: Int) -> Unit)?): Dialog {
        return Tools.createDeleteConfirmationDialog(ctx, ctx.getString(R.string.delete_confirmation),
                ctx.getString(R.string.delete), onClick)
    }

    public fun createDeleteConfirmationDialog(ctx: Context, msg: String, title: String,
                                              onClick: ((d: DialogInterface, i: Int) -> Unit)?): Dialog {
        val builder = AlertDialog.Builder(ctx)
        builder.setMessage(msg).setTitle(title)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, onClick)
                .setNegativeButton(R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }

    public fun onDefaultOptionItemSelected(ctx: MainActivity, item: MenuItem): Boolean {
        mActivity = ctx
        when (item.getItemId()) {
            R.id.debug -> {
                Tools.showDebugDialog(ctx)
                return true
            }
        }

        return false
    }

    public class AdvancedDialog : DialogFragment() {
        private var mId: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = getArguments()
            this.mId = args.getInt("id")
            val ctx = getActivity()
            val listener: DialogInterface.OnClickListener? = when (mId) {
                MainActivity.SAVE_ACCOUNT -> createRestoreOrBackupClickListener(R.string.backup_success,
                        R.string.backup_failed, { -> DbHelper.backupDatabase() })
                MainActivity.RESTORE_ACCOUNT -> createRestoreOrBackupClickListener(R.string.restore_success,
                        R.string.restore_failed, { -> DbHelper.restoreDatabase(ctx) })
                MainActivity.PROCESS_SCH -> object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        RadisService.acquireStaticLock(ctx)
                        ctx.startService(Intent(ctx, javaClass<RadisService>()))
                    }
                }
                else -> {
                    null
                }
            }
            return Tools.getAdvancedDialog(getActivity(), mId, listener!!)
        }

        companion object {
            public fun newInstance(id: Int, ctx: Context): AdvancedDialog {
                mActivity = ctx as Activity
                val frag = AdvancedDialog()
                val args = Bundle()
                args.putInt("id", id)
                frag.setArguments(args)
                return frag
            }
        }
    }

    public fun getAdvancedDialog(ctx: Activity, id: Int, onClick: DialogInterface.OnClickListener): Dialog {
        var msgId = -1
        when (id) {
            MainActivity.RESTORE_ACCOUNT -> msgId = R.string.restore_confirm
            MainActivity.SAVE_ACCOUNT -> msgId = R.string.backup_confirm
            MainActivity.PROCESS_SCH -> msgId = R.string.process_scheduled_transactions
            else -> {
            }
        }
        val builder = AlertDialog.Builder(ctx)
        builder.setMessage(msgId).setCancelable(false).setPositiveButton(R.string.to_continue, onClick)
                .setNegativeButton(R.string.cancel, { d, i -> d.cancel() })
        return builder.create()
    }

    public class ErrorDialog : DialogFragment() {
        private var mId: Int = 0

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = getArguments()
            this.mId = args.getInt("id")
            return Tools.createFailAndRestartDialog(getActivity(), mId)
        }

        companion object {

            public fun newInstance(id: Int): ErrorDialog {
                val frag = ErrorDialog()
                val args = Bundle()
                args.putInt("id", id)
                frag.setArguments(args)
                return frag
            }
        }
    }

    private fun createFailAndRestartDialog(ctx: Activity, id: Int): Dialog {
        val builder = AlertDialog.Builder(ctx)
        val msg = StringBuilder()
        msg.append(ctx.getString(id)).append('\n').append(ctx.getString(R.string.will_restart))
        builder.setMessage(msg).setCancelable(false).setPositiveButton(R.string.ok, { d, i ->
            Tools.restartApp(ctx)
        })
        return builder.create()
    }

    private fun createRestoreOrBackupClickListener(successTextId: Int, failureTextId: Int,
                                                   action: () -> Boolean): DialogInterface.OnClickListener {
        return object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, id: Int) {
                val ctx = mActivity
                if (action() && ctx != null) {
                    val msg = StringBuilder()
                    msg.append(ctx.getString(successTextId)).append('\n').append(ctx.getString(R.string.restarting))
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    Handler().postDelayed({ Tools.restartApp(ctx) }, 2000)
                } else {
                    ErrorDialog.newInstance(failureTextId).show((ctx as BaseActivity).getSupportFragmentManager(), "")
                }
            }
        }
    }

    public fun createClearedCalendar(): GregorianCalendar {
        val cal = GregorianCalendar()
        clearTimeOfCalendar(cal)
        return cal
    }

    public fun clearTimeOfCalendar(c: Calendar) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }

    // ------------------------------------------------------
    // DEBUG TOOLS
    // ------------------------------------------------------
    public fun restartApp(ctx: Context) {
        OperationListFragment.restart(ctx)
    }

    public fun showDebugDialog(activity: Context) {
        DebugDialog.newInstance().show((activity as BaseActivity).getSupportFragmentManager(), "debug")
    }

    public class DebugDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = getActivity()
            val items = arrayOf<CharSequence>("Trash DB", "Restart", "Install RadisService", "Trash Prefs")
            val builder = AlertDialog.Builder(context)
            builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    dialog.cancel()
                }
            })
            builder.setItems(items, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, item: Int) {
                    when (item) {
                        0 -> {
                            val client = context.getContentResolver().acquireContentProviderClient("fr.geobert.radis.db")
                            val provider = client.getLocalContentProvider() as DbContentProvider
                            provider.deleteDatabase(context)
                            client.release()
                            Tools.restartApp(context)
                        }
                        1 -> Tools.restartApp(context)
                        2 -> {
                            val i = Intent(context, javaClass<InstallRadisServiceReceiver>())
                            i.setAction(Tools.INTENT_RADIS_STARTED)
                            context.sendBroadcast(i)
                        }
                        3 -> DBPrefsManager.getInstance(context).resetAll()
                    }
                }
            })

            builder.setTitle("Debug menu")
            return builder.create()
        }

        companion object {
            public fun newInstance(): DebugDialog {
                val frag = DebugDialog()
                return frag
            }
        }
    }

    public fun setSumTextGravity(sumText: EditText) {
        val gravity: Int
        if (sumText.length() > 0) {
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        } else {
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        sumText.setGravity(gravity)
    }

    public fun getDateStr(date: Long): String {
        return date.formatDate()
    }

    public fun getDateStr(cal: Calendar): String {
        return getDateStr(cal.getTimeInMillis())
    }

    public fun createTooltip(stringId: Int): View.OnLongClickListener {
        return object : View.OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                val ctx = v.getContext()
                val t = Toast.makeText(ctx, ctx.getString(stringId), Toast.LENGTH_SHORT)
                val screenPos = IntArray(2)
                val displayFrame = Rect()
                val screenWidth = ctx.getResources().getDisplayMetrics().widthPixels
                v.getWindowVisibleDisplayFrame(displayFrame)
                v.getLocationInWindow(screenPos)
                t.setGravity(Gravity.RIGHT or Gravity.TOP, screenWidth - screenPos[0],
                        screenPos[1] - v.getHeight() - v.getHeight() / 2)
                t.show()
                return true
            }
        }
    }

    public fun hideKeyboard(ctx: Activity) {
        val inputManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(ctx.getCurrentFocus()!!.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS)
    }


    public fun showKeyboard(ctx: Activity) {
        val inputManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(ctx.getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT)
    }
}

