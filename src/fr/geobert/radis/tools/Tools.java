package fr.geobert.radis.tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import fr.geobert.radis.BaseActivity;
import fr.geobert.radis.R;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.DbHelper;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.RadisService;
import fr.geobert.radis.ui.OperationListActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Tools {
    // Intents actions
    public final static String INTENT_RADIS_STARTED = "fr.geobert.radis.STARTED";
    public final static String INTENT_OP_INSERTED = "fr.geobert.radis.OP_INSERTED";
    public static final int DEBUG_DIALOG = 9876;
    // debug mode stuff
    public static boolean DEBUG_MODE = true;
//    public static int SCREEN_HEIGHT;
    private static Activity mActivity;

    public static void checkDebugMode(Activity ctx) {
        // See if we're a debug or a release build
//        if (Build.VERSION.SDK_INT >= 13) {
//            Point p = new Point();
//            ctx.getWindowManager().getDefaultDisplay().getSize(p);
//            SCREEN_HEIGHT = p.y;
//        } else {
//            //noinspection deprecation
//            SCREEN_HEIGHT = ctx.getWindowManager().getDefaultDisplay().getHeight();
//        }
        try {
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                    ctx.getPackageName(), PackageManager.GET_CONFIGURATIONS);
            int flags = packageInfo.applicationInfo.flags;
            DEBUG_MODE = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }

    }

    public static void setViewBg(View v, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 16) {
            v.setBackground(drawable);
        } else {
            //noinspection deprecation
            v.setBackgroundDrawable(drawable);
        }

    }

    public static DialogInterface.OnClickListener createRestartClickListener(final Context ctx) {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Tools.restartApp(ctx);
            }
        };
    }

    public static void popError(Activity ctx, String msg,
                                DialogInterface.OnClickListener onClick) {
        AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
        alertDialog.setTitle("Erreur");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", onClick);
        alertDialog.show();
    }

    public static void setTextWithoutComplete(AutoCompleteTextView v,
                                              String text) {
        InfoAdapter adapter = (InfoAdapter) v.getAdapter();
        v.setAdapter(null);
        v.setText(text);
        v.setAdapter(adapter);
    }

    public static Dialog createDeleteConfirmationDialog(Context ctx,
                                                        DialogInterface.OnClickListener onClick) {
        return Tools.createDeleteConfirmationDialog(ctx, onClick, R.string.delete_confirmation);
    }

    public static Dialog createDeleteConfirmationDialog(Context ctx,
                                                        DialogInterface.OnClickListener onClick, final int msgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(msgId)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, onClick)
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    public static boolean onDefaultOptionItemSelected(FragmentActivity ctx, MenuItem item) {
        mActivity = ctx;
        switch (item.getItemId()) {
            case R.id.restore:
                AdvancedDialog.newInstance(R.id.restore).show(ctx.getSupportFragmentManager(), "restore");
                return true;
            case R.id.backup:
                AdvancedDialog.newInstance(R.id.backup).show(ctx.getSupportFragmentManager(), "backup");
                return true;
            case R.id.go_to_preferences:
                Intent i = new Intent(ctx, RadisConfiguration.class);
                ctx.startActivity(i);
                return true;
            case R.id.process_scheduling:
                AdvancedDialog.newInstance(R.id.process_scheduling).show(ctx.getSupportFragmentManager(),
                        "process_scheduling");
                return true;
            case R.id.debug:
                Tools.showDebugDialog(ctx);
                return true;
        }

        return false;
    }

    protected static class AdvancedDialog extends DialogFragment {
        private int mId;

        public static AdvancedDialog newInstance(final int id) {
            AdvancedDialog frag = new AdvancedDialog();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            this.mId = args.getInt("id");
            DialogInterface.OnClickListener listener;
            final Activity ctx = getActivity();
            switch (mId) {
                case R.id.backup:
                    listener = createRestoreOrBackupClickListener(
                            new BooleanResultNoParamFct() {
                                @Override
                                public boolean run() {
                                    return DbHelper.backupDatabase();
                                }
                            }, R.string.backup_success,
                            R.string.backup_failed);
                    break;
                case R.id.restore:
                    listener = createRestoreOrBackupClickListener(
                            new BooleanResultNoParamFct() {
                                @Override
                                public boolean run() {
                                    return DbHelper.restoreDatabase(ctx);
                                }
                            }, R.string.restore_success,
                            R.string.restore_failed);
                    break;
                case R.id.process_scheduling:
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RadisService.acquireStaticLock(ctx);
                            ctx.startService(new Intent(ctx, RadisService.class));
                        }

                    };
                    break;
                default:
                    listener = null;
            }
            return Tools.getAdvancedDialog(getActivity(), mId, listener);
        }
    }

    public static Dialog getAdvancedDialog(Activity ctx, int id,
                                           DialogInterface.OnClickListener onClick) {
        int msgId = -1;
        switch (id) {
            case R.id.restore:
                msgId = R.string.restore_confirm;
                break;
            case R.id.backup:
                msgId = R.string.backup_confirm;
                break;
            case R.id.process_scheduling:
                msgId = R.string.process_scheduled_transactions;
                break;
            default:
                break;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setMessage(msgId)
                .setCancelable(false)
                .setPositiveButton(R.string.to_continue, onClick)
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    protected static class ErrorDialog extends DialogFragment {
        private int mId;

        public static ErrorDialog newInstance(final int id) {
            ErrorDialog frag = new ErrorDialog();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            this.mId = args.getInt("id");
            return Tools.createFailAndRestartDialog(getActivity(), mId);
        }
    }

    private static Dialog createFailAndRestartDialog(final Activity ctx, int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        StringBuilder msg = new StringBuilder();
        msg.append(ctx.getString(id)).append('\n')
                .append(ctx.getString(R.string.will_restart));
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Tools.restartApp(ctx);
                            }
                        });
        return builder.create();
    }

    private static DialogInterface.OnClickListener createRestoreOrBackupClickListener(
            final BooleanResultNoParamFct action, final int successTextId,
            final int failureTextId) {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final Activity ctx = mActivity;
                if (action.run()) {
                    StringBuilder msg = new StringBuilder();
                    msg.append(ctx.getString(successTextId)).append('\n')
                            .append(ctx.getString(R.string.restarting));
                    Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
                    t.show();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            Tools.restartApp(ctx);
                        }
                    }, 2000);
                } else {
                    ErrorDialog.newInstance(failureTextId).show(((BaseActivity) ctx).getSupportFragmentManager(), "");
                }
            }
        };
    }

    public static GregorianCalendar createClearedCalendar() {
        GregorianCalendar cal = new GregorianCalendar();
        clearTimeOfCalendar(cal);
        return cal;
    }

    public static void clearTimeOfCalendar(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    // ------------------------------------------------------
    // DEBUG TOOLS
    // ------------------------------------------------------
    public static void restartApp(Context ctx) {
        OperationListActivity.restart(ctx);
    }

    public static void showDebugDialog(Context activity) {
        DebugDialog.newInstance().show(((BaseActivity) activity).getSupportFragmentManager(), "debug");
    }

    protected static class DebugDialog extends DialogFragment {
        public static DebugDialog newInstance() {
            DebugDialog frag = new DebugDialog();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity context = getActivity();
            final CharSequence[] items = {"Trash DB", "Restart",
                    "Install RadisService", "Trash Prefs"};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (item) {
                        case 0:
                            ContentProviderClient client = context.getContentResolver()
                                    .acquireContentProviderClient("fr.geobert.radis.db");
                            DbContentProvider provider = (DbContentProvider) client
                                    .getLocalContentProvider();
                            provider.deleteDatabase(context);
                            client.release();
                            Tools.restartApp(context);
                            break;
                        case 1:
                            Tools.restartApp(context);
                            break;
                        case 2:
                            Intent i = new Intent(context,
                                    InstallRadisServiceReceiver.class);
                            i.setAction(Tools.INTENT_RADIS_STARTED);
                            context.sendBroadcast(i);
                            break;
                        case 3:
                            DBPrefsManager.getInstance(context).resetAll();
                            break;
                    }
                }
            });

            builder.setTitle("Debug menu");
            return builder.create();
        }
    }

    public static void setSumTextGravity(EditText sumText) {
        int gravity;
        if (sumText.length() > 0) {
            gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        } else {
            gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        }
        sumText.setGravity(gravity);
    }

    public static String getDateStr(long date) {
//        GregorianCalendar g = new GregorianCalendar();
//        g.setTimeInMillis(date);
//        return getDateStr(g);
        return Formater.getFullDateFormater().format(new Date(date));
    }

    public static String getDateStr(Calendar cal) {
        return getDateStr(cal.getTimeInMillis());
    }

    private interface BooleanResultNoParamFct {
        boolean run();
    }
}
