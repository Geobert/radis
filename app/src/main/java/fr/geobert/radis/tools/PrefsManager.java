package fr.geobert.radis.tools;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;

// only for conversion from db version 10
@Deprecated
public class PrefsManager {
    public final static String SHARED_PREF_NAME = "radis_prefs";
    private static PrefsManager mInstance;
    private HashMap<String, String> mPrefs;
//    private Context mCurrentCtx;

    @SuppressWarnings("unchecked")
    private PrefsManager(Context ctx) {
        FileInputStream fis;
        try {
            fis = ctx.openFileInput(SHARED_PREF_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mPrefs = (HashMap<String, String>) ois.readObject();
        } catch (FileNotFoundException e) {
            mPrefs = new HashMap<>();
        } catch (StreamCorruptedException e) {
            Log.e("RADIS", "StreamCorruptedException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("RADIS", "IOException: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("RADIS", "ClassNotFoundException: " + e.getMessage());
        }
    }

    public static PrefsManager getInstance(Context ctx) {
        if (null == mInstance) {
            mInstance = new PrefsManager(ctx);
        }
//        mInstance.mCurrentCtx = ctx;
        return mInstance;
    }

    public HashMap<String, String> getRawData() {
        return mPrefs;
    }

//    public boolean commit() {
//        try {
//            FileOutputStream fos = mCurrentCtx.openFileOutput(SHARED_PREF_NAME,
//                    Context.MODE_PRIVATE);
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            oos.writeObject(mPrefs);
//            return true;
//        } catch (FileNotFoundException e) {
//            return false;
//        } catch (IOException e) {
//            return false;
//        }
//    }

//    public void put(final String key, final Object value) {
//        mPrefs.put(key, value.toString());
//    }

//    public Boolean getBoolean(final String key, final boolean defValue) {
//        String v = mPrefs.get(key);
//        try {
//            Boolean b = Boolean.valueOf(v);
//            return b != null ? b : defValue;
//        } catch (Exception e) {
//            return defValue;
//        }
//    }

}
