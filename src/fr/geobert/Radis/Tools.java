package fr.geobert.Radis;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;

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

	
	private static final CharSequence[] items = {"Trash database"};
	private static CommonDbAdapter mDb;
	public static Dialog getDebugDialog(Context context, CommonDbAdapter dB) {
		mDb = dB;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
		    	}
		    }
		});

		builder.setTitle("Debug menu");
		
		return builder.create();
	}
}
