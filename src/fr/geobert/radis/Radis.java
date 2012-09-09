package fr.geobert.radis;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dG9hd2l6YkFaNjFqSVVwcjJMWGlzRmc6MQ", 
//		customReportContent = { APP_VERSION_CODE, APP_VERSION_NAME, PHONE_MODEL, BRAND, ANDROID_VERSION, STACK_TRACE, USER_COMMENT, BUILD, CUSTOM_DATA },
		mode = ReportingInteractionMode.NOTIFICATION, resNotifTickerText = R.string.crash_notif_ticker_text, resNotifTitle = R.string.crash_notif_title, resNotifText = R.string.crash_notif_text, resDialogTitle = R.string.crash_dialog_title, resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, resDialogOkToast = R.string.crash_dialog_ok_toast, resDialogText = R.string.crash_dialog_text)
public class Radis extends Application {

	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}	
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		//CommonDbAdapter.getInstance(getApplicationContext()).close();
	}
}
