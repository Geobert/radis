package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import fr.geobert.radis.tools.PrefsManager
import java.util.HashMap
import kotlin.platform.platformStatic

public class PreferenceTable {

    public fun deletePref(db: SQLiteDatabase, key: String) {
        db.delete(DATABASE_PREFS_TABLE, KEY_PREFS_NAME + "='" + key + "'", null)
    }

    companion object {
        public val DATABASE_PREFS_TABLE: String = "preferences"
        public val KEY_PREFS_ROWID: String = "_id"
        public val KEY_PREFS_NAME: String = "pref_name"
        public val KEY_PREFS_VALUE: String = "pref_value"

        protected val DATABASE_PREFS_CREATE: String = "create table $DATABASE_PREFS_TABLE($KEY_PREFS_ROWID integer primary key autoincrement," +
                "$KEY_PREFS_NAME text not null, $KEY_PREFS_VALUE text not null);"
        public val PREFS_COLS: Array<String> = array(KEY_PREFS_NAME, KEY_PREFS_VALUE)
        //private val CREATE_ACCOUNT_COL = "ALTER TABLE $DATABASE_PREFS_TABLE ADD COLUMN $KEY_PREFS_ACCOUNT integer not null DEFAULT 0"
        //        private val CREATE_TRIGGER_ACCOUNT_DELETED = "CREATE TRIGGER IF NOT EXISTS on_account_deleted AFTER DELETE ON ${AccountTable.DATABASE_ACCOUNT_TABLE} " +
        //                "BEGIN DELETE FROM $DATABASE_PREFS_TABLE WHERE $KEY_PREFS_ACCOUNT = old._id ; END"

        platformStatic fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_PREFS_CREATE)
        }

        fun setPref(db: SQLiteDatabase, key: String, value: String) {
            val values = ContentValues()
            values.put(KEY_PREFS_VALUE, value)
            if (null == getPref(db, key)) {
                values.put(KEY_PREFS_NAME, key)
                // insert
                db.insert(DATABASE_PREFS_TABLE, null, values)
            } else {
                // update
                db.update(DATABASE_PREFS_TABLE, values, KEY_PREFS_NAME + "='" + key + "'", null)
            }
        }

        fun getPref(db: SQLiteDatabase, key: String): String? {
            var res: String? = null
            val c = db.query(DATABASE_PREFS_TABLE, array(KEY_PREFS_VALUE), KEY_PREFS_NAME + "='" + key + "'", null, null, null, null)
            if (null != c) {
                if (c.moveToFirst()) {
                    res = c.getString(0)
                }
                c.close()
            }
            return res
        }

        fun getAllPrefs(db: SQLiteDatabase): HashMap<String, String> {
            val res = HashMap<String, String>()
            val c = db.query(DATABASE_PREFS_TABLE, array(KEY_PREFS_NAME, KEY_PREFS_VALUE), null, null, null, null, null)
            if (null != c) {
                if (c.moveToFirst()) {
                    do {
                        res.put(c.getString(0), c.getString(1))
                    } while (c.moveToNext())
                }
                c.close()
            }
            return res
        }

        // UPGRADE

        platformStatic fun upgradeFromV10(ctx: Context, db: SQLiteDatabase) {
            db.execSQL(DATABASE_PREFS_CREATE)
            // convert all preferences to DB
            val prefs = PrefsManager.getInstance(ctx)
            val allPrefs = prefs.getRawData()
            if (null != allPrefs) {
                for (elt in allPrefs.entrySet()) {
                    val values = ContentValues()
                    values.put(KEY_PREFS_VALUE, elt.getValue())
                    values.put(KEY_PREFS_NAME, elt.getKey())
                    db.insert(DATABASE_PREFS_TABLE, null, values)
                }
            }
        }

        //        platformStatic fun upgradeFrom18(ctx: Context, db: SQLiteDatabase) {
        //            db.execSQL(CREATE_ACCOUNT_COL)
        //            db.execSQL(CREATE_TRIGGER_ACCOUNT_DELETED)
        //            // duplicate current options for each accounts
        //            val c = AccountTable.fetchAllAccounts(ctx)
        //            if (c.moveToFirst()) {
        //                do {
        //                    val id = c.getLong(0)
        //                    val values = ContentValues()
        //                    val p = DBPrefsManager.getInstance(ctx)
        //                    values.put(KEY_PREFS_ACCOUNT, id)
        //                    values.put(ConfigFragment.KEY_HIDE_OPS_QUICK_ADD,
        //                            p.getBoolean(ConfigFragment.KEY_HIDE_OPS_QUICK_ADD, false))
        //                    values.put(ConfigFragment.KEY_INSERTION_DATE, p.getInt(ConfigFragment.KEY_INSERTION_DATE,
        //                            ConfigFragment.DEFAULT_INSERTION_DATE.toInt()))
        //                    values.put(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD,
        //                            p.getBoolean(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD, false))
        //                    values.put(ConfigFragment.KEY_USE_WEIGHTED_INFOS,
        //                            p.getBoolean(ConfigFragment.KEY_USE_WEIGHTED_INFOS, true))
        //                    values.put(ConfigFragment.KEY_LAST_INSERTION_DATE,
        //                            p.getLong(ConfigFragment.KEY_LAST_INSERTION_DATE, 0))
        //                    db.insert(DATABASE_PREFS_TABLE, null, values)
        //                } while (c.moveToNext())
        //                c.close()
        //            }
        //        }
    }
}
