package fr.geobert.radis;

import android.app.Application;
import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.BUILD;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.ENVIRONMENT;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.IS_SILENT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.SETTINGS_SYSTEM;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_COMMENT;

//formKey = "dG9hd2l6YkFaNjFqSVVwcjJMWGlzRmc6MQ",
@ReportsCrashes(formKey = "",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        customReportContent = {REPORT_ID, APP_VERSION_CODE, APP_VERSION_NAME, PHONE_MODEL, BRAND, ANDROID_VERSION, STACK_TRACE, USER_COMMENT, CUSTOM_DATA, BUILD, IS_SILENT, ENVIRONMENT, INSTALLATION_ID, SETTINGS_SYSTEM},
        mode = ReportingInteractionMode.NOTIFICATION, resNotifTickerText = R.string.crash_notif_ticker_text, resNotifTitle = R.string.crash_notif_title, resNotifText = R.string.crash_notif_text, resDialogTitle = R.string.crash_dialog_title, resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, resDialogOkToast = R.string.crash_dialog_ok_toast, resDialogText = R.string.crash_dialog_text)
public class Radis extends Application {

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        super.onCreate();

        // dynamic config of acra to avoid user/password in code
        // you'll need to add these in a string resource file that you should not push anywhere public
        ACRAConfiguration acraCfg = ACRA.getConfig();
        acraCfg.setFormUri(getString(R.string.acra_report_url));
        acraCfg.setFormUriBasicAuthLogin(getString(R.string.acra_user));
        acraCfg.setFormUriBasicAuthPassword(getString(R.string.acra_pwd));

        ACRA.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //CommonDbAdapter.getInstance(getApplicationContext()).close();
    }
}
