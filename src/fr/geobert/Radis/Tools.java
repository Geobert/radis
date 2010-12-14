package fr.geobert.Radis;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Tools {
	private static Tools instance = null;
	private Context mCtx;
	
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
