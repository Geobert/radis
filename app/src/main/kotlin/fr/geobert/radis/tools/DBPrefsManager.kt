package fr.geobert.radis.tools

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.PreferenceTable
import java.util.*
import kotlin.properties.Delegates

public class DBPrefsManager : LoaderManager.LoaderCallbacks<Cursor> {
    private var mCurrentCtx: Context? = null
    private var mCache: HashMap<String, String>? = null
    private val FILL_CACHE = 100

    private var mCbk: () -> Unit by Delegates.notNull()

    // async
    public fun fillCache(ctx: FragmentActivity, cbk: () -> Unit) {
        if (mCache == null) {
            mCbk = cbk
            ctx.supportLoaderManager.initLoader<Cursor>(FILL_CACHE, Bundle(), this)
        } else {
            cbk()
        }
    }

    // sync
    public fun fillCache(ctx: Context) {
        if (mCache == null) {
            val data = ctx.contentResolver.query(DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS, null, null, null)
            mCache = HashMap<String, String>()
            if (data != null) {
                if (data.moveToFirst()) {
                    do {
                        mCache!!.put(data.getString(0), data.getString(1))
                    } while (data.moveToNext())
                }
                data.close()
            }
        }
    }

    public fun getString(key: String): String? {
        return mCache?.get(key)
    }

    public fun getString(key: String, defValue: String): String {
        return mCache?.get(key) ?: defValue
    }

    public fun getBoolean(key: String): Boolean? {
        val v = mCache?.get(key)
        try {
            val b = v?.toBoolean()
            return b
        } catch (e: Exception) {
            return null
        }

    }

    public fun getBoolean(key: String, defValue: Boolean): Boolean {
        try {
            return (mCache?.get(key)?.toBoolean()) ?: defValue
        } catch (e: Exception) {
            return defValue
        }

    }

    public fun getInt(key: String): Int? {
        val v = mCache?.get(key)
        if (v != null) {
            try {
                val b = Integer.valueOf(v)
                return b
            } catch (e: Exception) {
                return null
            }
        } else {
            return null
        }
    }

    public fun getInt(key: String, defValue: Int): Int {
        val v = mCache?.get(key)
        try {
            return (if (v != null) Integer.valueOf(v) else null) ?: defValue
        } catch (e: Exception) {
            return defValue
        }
    }

    public fun getLong(key: String): Long? {
        val v = mCache?.get(key)
        if (v != null) {
            try {
                return v.toLong()
            } catch (e: Exception) {
                return null
            }
        } else {
            return null
        }
    }

    public fun getLong(key: String, defValue: Long): Long {
        try {
            return (mCache?.get(key)?.toLong()) ?: defValue
        } catch (e: Exception) {
            return defValue
        }

    }

    public fun put(key: String, value: Any?) {
        if (!key.endsWith("_for_account") && !key.startsWith("override_")) {
            val cr = mCurrentCtx!!.contentResolver
            val values = ContentValues()
            values.put(PreferenceTable.KEY_PREFS_NAME, key)
            values.put(PreferenceTable.KEY_PREFS_VALUE, value.toString())
            values.put(PreferenceTable.KEY_PREFS_ACCOUNT, 0)
            values.put(PreferenceTable.KEY_PREFS_IS_ACTIVE, 1)

            if (mCache!!.get(key) == null) {
                cr.insert(DbContentProvider.PREFS_URI, values)
            } else {
                cr.update(DbContentProvider.PREFS_URI, values, PreferenceTable.KEY_PREFS_NAME + "=?", arrayOf(key))
            }
        }
        mCache!!.put(key, value.toString())
    }

    private fun deletePref(key: String) {
        mCurrentCtx!!.contentResolver.delete(DbContentProvider.PREFS_URI, PreferenceTable.KEY_PREFS_NAME + "=?", arrayOf(key))
    }

    private fun deleteAllPrefs() {
        val i = mCurrentCtx!!.contentResolver.delete(DbContentProvider.PREFS_URI, null, null)
    }

    public fun resetAll() {
        //clearAccountRelated()
        deleteAllPrefs()
        resetCache()
        val editor = PreferenceManager.getDefaultSharedPreferences (mCurrentCtx!!).edit() //mCurrentCtx!!.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.commit()
    }

    public fun resetCache() {
        mCache?.clear()
        mCache = null
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? = when (id) {
        FILL_CACHE -> CursorLoader(mCurrentCtx, DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS, null, null, null)
        else -> null
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            FILL_CACHE -> {
                mCache = HashMap<String, String>()
                if (data.moveToFirst()) {
                    do {
                        mCache!!.put(data.getString(0), data.getString(1))
                    } while (data.moveToNext())
                }
                mCbk()
            }
            else -> {
            }
        }

    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
        // nothing to do
    }

    companion object {
        public val SHARED_PREF_NAME: String = "radis_prefs"
        private var mInstance: DBPrefsManager? = null

        public fun getInstance(ctx: Context): DBPrefsManager {
            if (null == mInstance) {
                mInstance = DBPrefsManager()
                PreferenceManager.setDefaultValues(ctx, R.xml.preferences, false)
            }
            mInstance!!.mCurrentCtx = ctx
            return mInstance as DBPrefsManager
        }
    }
}
