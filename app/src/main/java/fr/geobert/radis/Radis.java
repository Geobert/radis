package fr.geobert.radis;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class Radis extends Application {

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        super.onCreate();

        boolean isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        if (!isDebuggable) {
            Fabric.with(this, new Crashlytics());
        }
    }
}
