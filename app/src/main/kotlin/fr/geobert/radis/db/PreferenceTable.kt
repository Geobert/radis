package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.support.v4.content.CursorLoader
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.tools.PrefsManager
import fr.geobert.radis.ui.ConfigFragment
import java.util.*

public object PreferenceTable {

    public fun deletePref(db: SQLiteDatabase, key: String) {
        db.delete(DATABASE_PREFS_TABLE, KEY_PREFS_NAME + "='" + key + "'", null)
    }

    public val DATABASE_PREFS_TABLE: String = "preferences"
    public val KEY_PREFS_ROWID: String = "_id"
    public val KEY_PREFS_NAME: String = "pref_name"
    public val KEY_PREFS_VALUE: String = "pref_value"
    public val KEY_PREFS_ACCOUNT: String = "account_id"
    public val KEY_PREFS_IS_ACTIVE: String = "active"

    protected val DATABASE_PREFS_CREATE: String = "create table $DATABASE_PREFS_TABLE($KEY_PREFS_ROWID integer primary key autoincrement,$KEY_PREFS_NAME text not null, $KEY_PREFS_VALUE text not null,$KEY_PREFS_ACCOUNT integer not null, $KEY_PREFS_IS_ACTIVE integer not null);"
    public val PREFS_COLS: Array<String> = arrayOf(KEY_PREFS_NAME, KEY_PREFS_VALUE)
    public val ACCOUNT_CONFIG_COLS: Array<String> = arrayOf(KEY_PREFS_NAME, KEY_PREFS_VALUE, KEY_PREFS_IS_ACTIVE)
    protected val ADD_ACCOUNT_COL: String = "ALTER TABLE $DATABASE_PREFS_TABLE ADD COLUMN $KEY_PREFS_ACCOUNT integer not null DEFAULT 0"
    protected val ADD_IS_ACTIVE_COL: String = "ALTER TABLE $DATABASE_PREFS_TABLE ADD COLUMN $KEY_PREFS_IS_ACTIVE integer not null DEFAULT 1"

    private val CREATE_TRIGGER_ACCOUNT_DELETED = "CREATE TRIGGER IF NOT EXISTS on_account_deleted AFTER DELETE ON ${AccountTable.DATABASE_ACCOUNT_TABLE} " +
            "BEGIN DELETE FROM $DATABASE_PREFS_TABLE WHERE $KEY_PREFS_ACCOUNT = old._id ; END"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DATABASE_PREFS_CREATE)
        db.execSQL(CREATE_TRIGGER_ACCOUNT_DELETED)
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
        val c = db.query(DATABASE_PREFS_TABLE, arrayOf(KEY_PREFS_VALUE), "$KEY_PREFS_NAME='$key' AND $KEY_PREFS_ACCOUNT = 0", null, null, null, null)
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
        val c = db.query(DATABASE_PREFS_TABLE, arrayOf(KEY_PREFS_NAME, KEY_PREFS_VALUE), "$KEY_PREFS_ACCOUNT = 0", null, null, null, null)
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

    public fun getAccountConfigLoader(ctx: Context, accountId: Long): CursorLoader {
        return CursorLoader(ctx, Uri.parse("${DbContentProvider.PREFS_URI}/$accountId"), ACCOUNT_CONFIG_COLS, null, null, null)
    }

    public fun createValuesOf(config: AccountConfig): List<ContentValues> {
        fun makeContentValues(k: String, v: Int, b: Boolean): ContentValues {
            val c = ContentValues()
            c.put(KEY_PREFS_NAME, k)
            c.put(KEY_PREFS_VALUE, v)
            c.put(KEY_PREFS_IS_ACTIVE, b)
            return c
        }

        fun makeContentValues(k: String, v: Boolean, b: Boolean): ContentValues {
            val c = ContentValues()
            c.put(KEY_PREFS_NAME, k)
            c.put(KEY_PREFS_VALUE, v)
            c.put(KEY_PREFS_IS_ACTIVE, b)
            return c
        }

        return listOf(
                makeContentValues(ConfigFragment.KEY_INSERTION_DATE, config.insertDate, config.overrideInsertDate),
                makeContentValues(ConfigFragment.KEY_HIDE_OPS_QUICK_ADD, config.hideQuickAdd, config.overrideHideQuickAdd),
                makeContentValues(ConfigFragment.KEY_USE_WEIGHTED_INFOS, config.useWeighedInfo, config.overrideUseWeighedInfo),
                makeContentValues(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD, config.invertQuickAddComp, config.overrideInvertQuickAddComp),
                makeContentValues(ConfigFragment.KEY_NB_MONTH_AHEAD, config.nbMonthsAhead, config.overrideNbMonthsAhead),
                makeContentValues(ConfigFragment.KEY_QUICKADD_ACTION, config.quickAddAction, config.overrideQuickAddAction)
        )

    }

    fun createAccountPrefs(ctx: Context, config: AccountConfig, accountId: Long) {
        val values = createValuesOf(config)
        values.forEach {
            it.put(KEY_PREFS_ACCOUNT, accountId)
            ctx.contentResolver.insert(DbContentProvider.PREFS_URI, it)
        }
    }

    fun updateAccountPrefs(ctx: Context, config: AccountConfig, id: Long) {
        val values = createValuesOf(config)
        values.forEach {
            val k = it.getAsString(KEY_PREFS_NAME)
            it.remove(KEY_PREFS_NAME)
            val u = ctx.contentResolver.update(Uri.parse("${DbContentProvider.PREFS_URI}/$id"), it, "$KEY_PREFS_NAME=?", arrayOf(k))
        }
    }

    fun fetchPrefForAccount(ctx: Context, accountId: Long): Cursor {
        return ctx.contentResolver.query(Uri.parse("${DbContentProvider.PREFS_URI}/$accountId"), ACCOUNT_CONFIG_COLS, null, null, null)
    }


    // UPGRADE

    fun upgradeFromV10(ctx: Context, db: SQLiteDatabase) {
        db.execSQL(DATABASE_PREFS_CREATE)
        // convert all preferences to DB
        val prefs = PrefsManager.getInstance(ctx)
        val allPrefs = prefs.rawData
        if (null != allPrefs) {
            for (elt in allPrefs.entries) {
                val values = ContentValues()
                values.put(KEY_PREFS_VALUE, elt.value)
                values.put(KEY_PREFS_NAME, elt.key)
                db.insert(DATABASE_PREFS_TABLE, null, values)
            }
        }
    }

    fun upgradeFromV18(db: SQLiteDatabase) {
        db.execSQL(ADD_ACCOUNT_COL)
        db.execSQL(ADD_IS_ACTIVE_COL)
        db.execSQL(CREATE_TRIGGER_ACCOUNT_DELETED)
    }

}
