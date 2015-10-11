package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.support.v4.content.CursorLoader
import android.util.Log
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.convertNonAscii
import fr.geobert.radis.ui.ConfigFragment
import java.util.*

// this class manage simple info tables which are _id / name schema
public object InfoTables {
    val DATABASE_THIRD_PARTIES_TABLE = "third_parties"
    val DATABASE_TAGS_TABLE = "tags"
    val DATABASE_MODES_TABLE = "modes"

    public val KEY_THIRD_PARTY_ROWID: String = "_id"
    public val KEY_THIRD_PARTY_NAME: String = "third_party_name"
    public val KEY_THIRD_PARTY_NORMALIZED_NAME: String = "third_party_norm_name"
    public val KEY_WEIGHT: String = "weight"
    public val KEY_TAG_ROWID: String = "_id"
    public val KEY_TAG_NAME: String = "tag_name"
    public val KEY_TAG_NORMALIZED_NAME: String = "tag_norm_name"
    public val KEY_MODE_ROWID: String = "_id"
    public val KEY_MODE_NAME: String = "mode_name"
    public val KEY_MODE_NORMALIZED_NAME: String = "mode_norm_name"

    private val DATABASE_THIRD_PARTIES_CREATE = "create table $DATABASE_THIRD_PARTIES_TABLE($KEY_THIRD_PARTY_ROWID integer primary key autoincrement, $KEY_THIRD_PARTY_NAME text not null, $KEY_THIRD_PARTY_NORMALIZED_NAME text not null, $KEY_WEIGHT integer);"

    private val DATABASE_TAGS_CREATE = "create table $DATABASE_TAGS_TABLE($KEY_TAG_ROWID integer primary key autoincrement, $KEY_TAG_NAME text not null, $KEY_TAG_NORMALIZED_NAME text not null, $KEY_WEIGHT integer);"

    private val DATABASE_MODES_CREATE = "create table $DATABASE_MODES_TABLE($KEY_MODE_ROWID integer primary key autoincrement, $KEY_MODE_NAME text not null, $KEY_MODE_NORMALIZED_NAME text not null, $KEY_WEIGHT integer);"

    protected val ADD_NORMALIZED_MODE: String = "ALTER TABLE $DATABASE_MODES_TABLE ADD COLUMN $KEY_MODE_NORMALIZED_NAME text not null DEFAULT ''"

    protected val ADD_NORMALIZED_TAG: String = "ALTER TABLE $DATABASE_TAGS_TABLE ADD COLUMN $KEY_TAG_NORMALIZED_NAME text not null DEFAULT ''"

    protected val ADD_NORMALIZED_THIRD_PARTY: String = "ALTER TABLE $DATABASE_THIRD_PARTIES_TABLE ADD COLUMN $KEY_THIRD_PARTY_NORMALIZED_NAME text not null DEFAULT ''"

    protected val ADD_WEIGHT_COL: String = "ALTER TABLE %s ADD COLUMN $KEY_WEIGHT integer DEFAULT 0"

    @SuppressWarnings("serial")
    private val mColNameNormName = object : HashMap<String, String>() {
        init {
            put(InfoTables.KEY_THIRD_PARTY_NAME,
                    InfoTables.KEY_THIRD_PARTY_NORMALIZED_NAME)
            put(InfoTables.KEY_TAG_NAME, InfoTables.KEY_TAG_NORMALIZED_NAME)
            put(InfoTables.KEY_MODE_NAME, InfoTables.KEY_MODE_NORMALIZED_NAME)
        }
    }

    @SuppressWarnings("serial")
    private val mInfoColMap = object : HashMap<String, String>() {
        init {
            put(DbContentProvider.THIRD_PARTY_URI.toString(),
                    KEY_THIRD_PARTY_NAME)
            put(DbContentProvider.TAGS_URI.toString(), KEY_TAG_NAME)
            put(DbContentProvider.MODES_URI.toString(), KEY_MODE_NAME)
        }
    }

    @SuppressWarnings("serial")
    private val mInfoUriMap = object : HashMap<Uri, String>() {
        init {
            put(DbContentProvider.THIRD_PARTY_URI, DATABASE_THIRD_PARTIES_TABLE)
            put(DbContentProvider.TAGS_URI, DATABASE_TAGS_TABLE)
            put(DbContentProvider.MODES_URI, DATABASE_MODES_TABLE)
        }
    }

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DATABASE_THIRD_PARTIES_CREATE)
        db.execSQL(DATABASE_TAGS_CREATE)
        db.execSQL(DATABASE_MODES_CREATE)
    }

    private fun getInfoByKey(ctx: Context, key: String, table: Uri, col: String): Cursor? {
        val k = convertNonAscii(key).trim().toLowerCase()
        if (k.length() == 0) {
            return null
        }
        return ctx.contentResolver.query(table, arrayOf("_id", KEY_WEIGHT),
                mColNameNormName.get(col) + "= ?", arrayOf(k), null)
    }

    private fun createKeyId(ctx: Context, key: String, table: Uri, col: String): Long {
        val k = convertNonAscii(key).trim().toLowerCase()
        val initialValues = ContentValues()
        initialValues.put(col, key)
        initialValues.put(mColNameNormName.get(col), k)
        initialValues.put(KEY_WEIGHT, 0)
        val res = ctx.contentResolver.insert(table, initialValues)
        val id = java.lang.Long.parseLong(res.lastPathSegment)
        if (id == -1L) {
            throw SQLException("Database insertion error : $k in $table")
        }
        return id
    }

    public fun getKeyIdIfExistsOrCreate(ctx: Context, key: String, table: Uri): Long {
        var col: String

        if (table == DbContentProvider.THIRD_PARTY_URI) {
            col = KEY_THIRD_PARTY_NAME
        } else if (table == DbContentProvider.TAGS_URI) {
            col = KEY_TAG_NAME
        } else {
            // DbContentProvider.MODES_URI
            col = KEY_MODE_NAME
        }
        val inf = getInfoByKey(ctx, key, table, col)
        var res: Long = -1
        if (inf == null || inf.count == 0) {
            createKeyId(ctx, key, table, col)
        } else {
            if (inf.moveToFirst()) {
                res = inf.getLong(0)
            }
            inf.close()
        }
        return res
    }

    public fun updateInfo(ctx: Context, table: Uri, rowId: Long, value: String?, weight: Long?): Boolean {
        val args = ContentValues()
        if (value != null) {
            args.put(mInfoColMap.get(table.toString()), value)
            args.put(mColNameNormName.get(mInfoColMap.get(table.toString())),
                    convertNonAscii(value).trim().toLowerCase())
        }
        if (weight != null) {
            args.put(KEY_WEIGHT, weight)
        }
        val res = ctx.contentResolver.update(Uri.parse("$table/$rowId"), args, null, null)
        return res > 0
    }

    public fun deleteInfo(ctx: Context, table: Uri, rowId: Long): Boolean {
        val res = ctx.contentResolver.delete(
                Uri.parse("$table/$rowId"), null, null) > 0
        return res
    }

    public fun fetchMatchingInfo(ctx: Context, table: Uri, colName: String, constraint: String?,
                                 isQuickAdd: Boolean, account: AccountConfig): Cursor {
        val where: String?
        val params: Array<String>?
        if (null != constraint) {
            where = mColNameNormName.get(colName) + " LIKE ?"
            params = arrayOf(constraint.trim() + "%")
        } else {
            where = null
            params = null
        }
        var ordering = colName + " asc"
        val useWeight = if (account.overrideUseWeighedInfo)
            account.useWeighedInfo
        else
            DBPrefsManager.getInstance(ctx).getBoolean(ConfigFragment.KEY_USE_WEIGHTED_INFOS, true)
        if (useWeight) {
            val order: String
            val ascOrder = if (account.overrideInvertQuickAddComp)
                account.invertQuickAddComp
            else
                DBPrefsManager.getInstance(ctx).getBoolean(ConfigFragment.KEY_INVERT_COMPLETION_IN_QUICK_ADD, true)
            if (isQuickAdd && ascOrder) {
                order = " asc, "
            } else {
                order = " desc, "
            }
            ordering = KEY_WEIGHT + order + ordering
        }
        val c = ctx.contentResolver.query(table,
                arrayOf("_id", colName, KEY_WEIGHT), where, params, ordering)
        c?.moveToFirst()
        return c
    }

    public fun getMatchingInfoLoader(ctx: Context, table: Uri, colName: String, constraint: String?): CursorLoader {
        val where: String?
        val params: Array<String>?
        if (null != constraint) {
            where = mColNameNormName.get(colName) + " LIKE ?"
            params = arrayOf(constraint.trim() + "%")
        } else {
            where = null
            params = null
        }
        var ordering = colName + " asc"
        if (DBPrefsManager.getInstance(ctx).getBoolean(ConfigFragment.KEY_USE_WEIGHTED_INFOS, true)) {
            ordering = KEY_WEIGHT + " desc, " + ordering
        }
        val loader = CursorLoader(ctx, table, arrayOf("_id", colName, KEY_WEIGHT), where, params,
                ordering)

        return loader
    }

    fun putKeyIdInThirdParties(ctx: Context, key: String, initialValues: ContentValues, isUpdate: Boolean) {
        putKeyId(ctx, key, DbContentProvider.THIRD_PARTY_URI, KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_THIRD_PARTY,
                initialValues, isUpdate)
    }

    fun putKeyIdInTags(ctx: Context, key: String, initialValues: ContentValues, isUpdate: Boolean) {
        putKeyId(ctx, key, DbContentProvider.TAGS_URI, KEY_TAG_NAME, OperationTable.KEY_OP_TAG,
                initialValues, isUpdate)
    }

    fun putKeyIdInModes(ctx: Context, key: String, initialValues: ContentValues, isUpdate: Boolean) {
        putKeyId(ctx, key, DbContentProvider.MODES_URI, KEY_MODE_NAME, OperationTable.KEY_OP_MODE,
                initialValues, isUpdate)
    }

    private val weightProj = arrayOf("_id", KEY_WEIGHT)

    private fun fetchMaxWeight(ctx: Context, keyTableName: Uri): Cursor? {
        val c = ctx.contentResolver.query(keyTableName, weightProj,
                KEY_WEIGHT + " = (SELECT max(i2." + KEY_WEIGHT + ") FROM " + mInfoUriMap.get(keyTableName) + " i2) ",
                null, null)
        c?.moveToFirst()
        return c
    }

    private fun putKeyId(ctx: Context, key: String, keyTableName: Uri, keyTableCol: String, opTableCol: String,
                         initialValues: ContentValues, isUpdate: Boolean) {
        val inf = getInfoByKey(ctx, key, keyTableName, keyTableCol)
        var justCreated = false
        var id: Long = -1
        var weight: Long = -1
        if (inf == null || !inf.moveToFirst()) {
            if (key.length() > 0) {
                id = createKeyId(ctx, key, keyTableName, keyTableCol)
                justCreated = true
            }
            inf?.close()
        } else {
            if (inf.moveToFirst()) {
                weight = inf.getLong(1)
                id = inf.getLong(0)
            }
            inf.close()
        }

        if (id != -1L) {
            if (DBPrefsManager.getInstance(ctx).getBoolean(ConfigFragment.KEY_USE_WEIGHTED_INFOS, true) && !isUpdate &&
                    !justCreated) {
                // update of weight
                val maxWeightInf = fetchMaxWeight(ctx, keyTableName)
                if (maxWeightInf != null) {
                    if (maxWeightInf.count == 0 || maxWeightInf.getLong(0) != id) {
                        updateInfo(ctx, keyTableName, id, null, weight + 1)
                    }
                    maxWeightInf.close()
                }
            }
            initialValues.put(opTableCol, id)
        } else {
            initialValues.putNull(opTableCol)
        }
    }

    // UPGRADE FUNCTIONS
    fun upgradeFromV8(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var c: Cursor? = db.query(DATABASE_THIRD_PARTIES_TABLE, arrayOf(KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NAME),
                null, null, null, null, null)
        if (c != null && c.moveToFirst()) {
            do {
                val v = ContentValues()
                v.put(KEY_THIRD_PARTY_NORMALIZED_NAME,
                        convertNonAscii(c.getString(c.getColumnIndex(KEY_THIRD_PARTY_NAME))).toLowerCase())
                db.update(DATABASE_THIRD_PARTIES_TABLE, v,
                        KEY_THIRD_PARTY_ROWID + "=" + java.lang.Long.toString(c.getLong(c.getColumnIndex(KEY_THIRD_PARTY_ROWID))),
                        null)
            } while (c.moveToNext())
            c.close()
        }
        c = db.query(DATABASE_TAGS_TABLE, arrayOf(KEY_TAG_ROWID, KEY_TAG_NAME), null, null, null, null, null)
        if (c != null && c.moveToFirst()) {
            do {
                val v = ContentValues()
                v.put(KEY_TAG_NORMALIZED_NAME,
                        convertNonAscii(c.getString(c.getColumnIndex(KEY_TAG_NAME))).toLowerCase())
                db.update(DATABASE_TAGS_TABLE, v,
                        KEY_TAG_ROWID + "=" + java.lang.Long.toString(c.getLong(c.getColumnIndex(KEY_TAG_ROWID))), null)
            } while (c.moveToNext())
            c.close()
        }

        c = db.query(DATABASE_MODES_TABLE, arrayOf(KEY_MODE_ROWID, KEY_MODE_NAME), null, null, null, null, null)
        if (c != null && c.moveToFirst()) {
            do {
                val v = ContentValues()
                v.put(KEY_MODE_NORMALIZED_NAME,
                        convertNonAscii(c.getString(c.getColumnIndex(KEY_MODE_NAME))).toLowerCase())
                db.update(DATABASE_MODES_TABLE, v,
                        KEY_MODE_ROWID + "=" + java.lang.Long.toString(c.getLong(c.getColumnIndex(KEY_MODE_ROWID))), null)
            } while (c.moveToNext())
            c.close()
        }
    }

    fun upgradeFromV7(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(ADD_NORMALIZED_THIRD_PARTY)
        db.execSQL(ADD_NORMALIZED_TAG)
        db.execSQL(ADD_NORMALIZED_MODE)
    }


    fun updateATableFromV13(db: SQLiteDatabase, table: String, tableCreate: String, rowKey: String, nameKey: String, normKey: String, strangerKey: String) {
        val oldTable = table + "_old"
        db.execSQL("ALTER TABLE $table RENAME TO $oldTable")
        db.execSQL(tableCreate)
        var itor: Cursor? = db.query(oldTable, arrayOf(rowKey, normKey, nameKey), null, null, null, null, null)
        while (itor != null && itor.moveToFirst()) {
            val normName = itor.getString(1)
            val initialVal = ContentValues()
            initialVal.put(nameKey, itor.getString(2))
            initialVal.put(normKey, normName)
            val newId = db.insert(table, null, initialVal)
            Log.d("InfoTables", "upgradeFromV13 : newId $newId for $normName")
            val doubles = db.query(oldTable, arrayOf(rowKey),
                    normKey + " LIKE ?", arrayOf(normName), null, null, null)
            if (doubles != null && doubles.moveToFirst()) {
                // collect ids of doubles
                val listOfId = Vector<Long>(doubles.count)
                do {
                    listOfId.add(doubles.getLong(0))
                } while (doubles.moveToNext())
                doubles.close()
                for (id in listOfId) {
                    // replace id by refId in operations and sched_op tables
                    val v = ContentValues()
                    v.put(strangerKey, newId)
                    val i = db.update(OperationTable.DATABASE_OPERATIONS_TABLE, v,
                            strangerKey + "=?", arrayOf(id!!.toString()))

                    db.update(ScheduledOperationTable.DATABASE_SCHEDULED_TABLE, v,
                            strangerKey + "=?", arrayOf(id.toString()))
                    val nb = db.delete(oldTable, rowKey + "=?", arrayOf(id.toString()))
                    Log.d("InfoTables", "upgradeFromV13 : nb deleted from $oldTable : $nb")
                }
            }
            itor.close()
            itor = db.query(oldTable, arrayOf(rowKey, normKey, nameKey), null, null, null, null, null)
        }
        db.execSQL("DROP TABLE " + oldTable)
    }

    // find duplicate of infos and update operations
    fun upgradeFromV13(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TRIGGER on_delete_third_party")
        db.execSQL("DROP TRIGGER on_delete_mode")
        db.execSQL("DROP TRIGGER on_delete_tag")
        updateATableFromV13(db, DATABASE_THIRD_PARTIES_TABLE, DATABASE_THIRD_PARTIES_CREATE, KEY_THIRD_PARTY_ROWID,
                KEY_THIRD_PARTY_NAME, KEY_THIRD_PARTY_NORMALIZED_NAME, OperationTable.KEY_OP_THIRD_PARTY)
        updateATableFromV13(db, DATABASE_MODES_TABLE, DATABASE_MODES_CREATE, KEY_MODE_ROWID,
                KEY_MODE_NAME, KEY_MODE_NORMALIZED_NAME, OperationTable.KEY_OP_MODE)
        updateATableFromV13(db, DATABASE_TAGS_TABLE, DATABASE_TAGS_CREATE, KEY_TAG_ROWID,
                KEY_TAG_NAME, KEY_TAG_NORMALIZED_NAME, OperationTable.KEY_OP_TAG)
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_THIRD_PARTY_CREATE)
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_MODE_CREATE)
        db.execSQL(OperationTable.TRIGGER_ON_DELETE_TAG_CREATE)
    }

    public fun upgradeFromV15(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.delete(DATABASE_TAGS_TABLE, KEY_TAG_NAME + "=''", null)
        db.delete(DATABASE_MODES_TABLE, KEY_MODE_NAME + "=''", null)
    }

    public fun upgradeFromV16(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL(ADD_WEIGHT_COL.format(DATABASE_THIRD_PARTIES_TABLE))
        } catch (e: SQLiteException) {
        }

        try {
            db.execSQL(ADD_WEIGHT_COL.format(DATABASE_TAGS_TABLE))
        } catch (e: SQLiteException) {
        }

        try {
            db.execSQL(ADD_WEIGHT_COL.format(DATABASE_MODES_TABLE))
        } catch (e: SQLiteException) {
        }

    }
}
