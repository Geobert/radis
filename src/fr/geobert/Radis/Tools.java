package fr.geobert.Radis;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Tools {
	private static Tools instance = null;
	private Context mCtx;

	// these are here because database force to use "_id" to be able to use
	// SimpleCursorAdaptater, so KEY_ACCOUNT_ROWID == KEY_OP_ROWID and make bug
	// when used in extras
	public static String EXTRAS_OP_ID = "op_id";
	public static String EXTRAS_ACCOUNT_ID = "account_id";

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
}
