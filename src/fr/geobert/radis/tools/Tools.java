package fr.geobert.radis.tools;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import fr.geobert.radis.AccountList;
import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.R;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.DbHelper;
import fr.geobert.radis.service.InstallRadisServiceReceiver;
import fr.geobert.radis.service.RadisService;

public class Tools {
	// these are here because database force to use "_id" to be able to use
	// SimpleCursorAdaptater, so KEY_ACCOUNT_ROWID == KEY_OP_ROWID and make bug
	// when used in Bundle's extras
	public final static String EXTRAS_OP_ID = "op_id";
	public final static String EXTRAS_ACCOUNT_ID = "account_id";

	// Intents actions
	public final static String INTENT_RADIS_STARTED = "fr.geobert.radis.STARTED";
	public final static String INTENT_OP_INSERTED = "fr.geobert.radis.OP_INSERTED";

	// debug mode stuff
	public static boolean DEBUG_MODE = true;
	public static final int DEBUG_DIALOG = 9876;
	
	public static int SCREEN_HEIGHT;

	public static void checkDebugMode(Activity ctx) {
		// See if we're a debug or a release build
		SCREEN_HEIGHT = ctx.getWindowManager().getDefaultDisplay().getHeight();
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			int flags = packageInfo.applicationInfo.flags;
			DEBUG_MODE = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
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
		alertDialog.setButton("OK", onClick);
		alertDialog.show();
	}

	public static void setTextWithoutComplete(AutoCompleteTextView v,
			String text) {
		InfoAdapter adapter = (InfoAdapter) v.getAdapter();
		v.setAdapter((InfoAdapter) null);
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
	
	public static boolean onDefaultMenuSelected(Activity ctx, int featureId,
			MenuItem item) {
		switch (item.getItemId()) {
		case R.id.restore:
			ctx.showDialog(R.id.restore);
			return true;
		case R.id.backup:
			ctx.showDialog(R.id.backup);
			return true;
		case R.id.go_to_preferences:
			Intent i = new Intent(ctx, RadisConfiguration.class);
			ctx.startActivity(i);
			return true;
		case R.id.process_scheduling:
			ctx.showDialog(R.id.process_scheduling);
			return true;
		}
		return false;
	}

	private static Activity mActivity;

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

	private interface BooleanResultNoParamFct {
		boolean run();
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
					ctx.showDialog(failureTextId);
				}
			}
		};
	}

	public static Dialog onDefaultCreateDialog(final Activity ctx, int id) {
		mActivity = ctx;
		switch (id) {
		case Tools.DEBUG_DIALOG:
			return Tools.getDebugDialog(ctx);
		case R.id.restore:
			return Tools.getAdvancedDialog(
					ctx,
					id,
					createRestoreOrBackupClickListener(
							new BooleanResultNoParamFct() {
								@Override
								public boolean run() {
									return DbHelper.restoreDatabase(ctx);
								}
							}, R.string.restore_success,
							R.string.restore_failed));
		case R.id.backup:
			return Tools
					.getAdvancedDialog(
							ctx,
							id,
							createRestoreOrBackupClickListener(
									new BooleanResultNoParamFct() {
										@Override
										public boolean run() {
											return DbHelper.backupDatabase();
										}
									}, R.string.backup_success,
									R.string.backup_failed));
		case R.string.backup_failed:
		case R.string.restore_failed:
			return createFailAndRestartDialog(ctx, id);
		case R.id.process_scheduling:
			return Tools.getAdvancedDialog(ctx, id,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							RadisService.acquireStaticLock(ctx);
							ctx.startService(new Intent(ctx, RadisService.class));
						}

					});
		}
		return null;
	}

	public static void clearTimeOfCalendar(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
	}

	// ------------------------------------------------------
	// DEBUG�TOOLS
	// ------------------------------------------------------
	public static void restartApp(Context ctx) {
		AccountList.restart(ctx);
	}

	public static boolean onKeyLongPress(int keyCode, KeyEvent event,
			Activity curActivity) {
		if (keyCode == KeyEvent.KEYCODE_BACK && DEBUG_MODE) {
			curActivity.showDialog(Tools.DEBUG_DIALOG);
			return true;
		}
		return false;
	}

	public static Dialog getDebugDialog(final Context context) {
		final CharSequence[] items = { "Trash DB", "Restart",
				"Install RadisService", "Trash Prefs"};
		//mDb = dB;
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
			//		mDb.trashDatabase();
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
		GregorianCalendar g = new GregorianCalendar();
		g.setTimeInMillis(date);
		return getDateStr(g);
	}
	
	public static String getDateStr(Calendar cal) {
		return String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)) + "/"
				+ String.format("%02d", cal.get(Calendar.MONTH) + 1) + "/"
				+ cal.get(Calendar.YEAR);
	}
}
