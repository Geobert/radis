package fr.geobert.radis;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class Tools {
	private static Tools instance = null;
	private Context mCtx;

	// these are here because database force to use "_id" to be able to use
	// SimpleCursorAdaptater, so KEY_ACCOUNT_ROWID == KEY_OP_ROWID and make bug
	// when used in extras
	public static String EXTRAS_OP_ID = "op_id";
	public static String EXTRAS_ACCOUNT_ID = "account_id";
	public static boolean DEBUG_MODE = true;
	public static final int DEBUG_DIALOG = 9876;

	public static void checkDebugMode(Context ctx) {
		// See if we're a debug or a release build
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			int flags = packageInfo.applicationInfo.flags;
			DEBUG_MODE = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}

	}

	public void popError(String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(mCtx).create();
		alertDialog.setTitle("Erreur");
		alertDialog.setMessage(msg);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		alertDialog.show();
	}

	public static Tools getInstance(Context ctx) {
		if (null == instance) {
			instance = new Tools();
		}
		instance.mCtx = ctx;
		return instance;
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
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(R.string.delete_confirmation)
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
		}
		return false;
	}

	private static CommonDbAdapter mDb;
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

	private static Dialog createFailAndRestartDialog(Activity ctx, int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		StringBuilder msg = new StringBuilder();
		msg.append(ctx.getString(id)).append('\n')
				.append(ctx.getString(R.string.will_restart));
		builder.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int id) {
								Tools.restartApp();
							}
						});
		return builder.create();
	}
	
	public static Dialog onDefaultCreateDialog(Activity ctx, int id,
			CommonDbAdapter db) {
		mDb = db;
		mActivity = ctx;
		switch (id) {
		case Tools.DEBUG_DIALOG:
			return Tools.getDebugDialog(ctx, db);
		case R.id.restore:
			return Tools.getAdvancedDialog(ctx, id,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Activity ctx = mActivity;
							if (mDb.backupDatabase()) {
								StringBuilder msg = new StringBuilder();
								msg.append(
										ctx.getString(R.string.restore_success))
										.append('\n')
										.append(ctx
												.getString(R.string.restarting));
								Toast t = Toast.makeText(ctx, msg,
										Toast.LENGTH_LONG);
								t.show();
								new Handler().postDelayed(new Runnable() {
									public void run() {
										Tools.restartApp();
									}
								}, 2000);
							} else {
								ctx.showDialog(R.string.restore_failed);
							}
						}
					});
		case R.id.backup:
			return Tools.getAdvancedDialog(ctx, id,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Activity ctx = mActivity;
							if (mDb.backupDatabase()) {
								StringBuilder msg = new StringBuilder();
								msg.append(
										ctx.getString(R.string.backup_success))
										.append('\n')
										.append(ctx
												.getString(R.string.restarting));
								Toast t = Toast.makeText(ctx, msg,
										Toast.LENGTH_LONG);
								t.show();
								new Handler().postDelayed(new Runnable() {
									public void run() {
										Tools.restartApp();
									}
								}, 2000);
							} else {
								ctx.showDialog(R.string.backup_failed);
							}
						}
					});
		case R.string.backup_failed:
			return createFailAndRestartDialog(ctx, id);
		case R.string.restore_failed:
			return createFailAndRestartDialog(ctx, id);
		}
		return null;
	}

	// ------------------------------------------------------
	// DEBUG TOOLS
	// ------------------------------------------------------
	public static void restartApp() {
		AlarmManager mgr = (AlarmManager) AccountList.ACTIVITY
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000,
				AccountList.RESTART_INTENT);
		System.exit(2);
	}

	public static boolean onKeyLongPress(int keyCode, KeyEvent event,
			Activity curActivity) {
		if (keyCode == KeyEvent.KEYCODE_BACK && DEBUG_MODE) {
			curActivity.showDialog(Tools.DEBUG_DIALOG);
			return true;
		}
		return false;
	}

	public static Dialog getDebugDialog(Context context, CommonDbAdapter dB) {
		final CharSequence[] items = { "Trash DB", "Restart" };
		mDb = dB;
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
					mDb.trashDatabase();
					break;
				case 1:
					Tools.restartApp();
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
			gravity = Gravity.CENTER_VERTICAL|Gravity.RIGHT;
		} else {
			gravity = Gravity.CENTER_VERTICAL|Gravity.LEFT;
		}
		sumText.setGravity(gravity);
	}

	// private static void fillDatabase(CommonDbAdapter db) {
	// mDb = db;
	//
	// }
}
