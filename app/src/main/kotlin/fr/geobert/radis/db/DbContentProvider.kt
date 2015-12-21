package fr.geobert.radis.db

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import java.util.*

class DbContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        mDbHelper = DbHelper.getInstance(context)
        return false
    }

    fun deleteDatabase(ctx: Context): Boolean {
        Log.d(TAG, "deleteDatabase from ContentProvider")
        DbHelper.delete()
        val res = ctx.deleteDatabase(DbHelper.DATABASE_NAME)
        mDbHelper = DbHelper.getInstance(ctx)
        return res
    }

    private fun switchToTable(uri: Uri): String {
        val uriType = sURIMatcher.match(uri)
        //		Log.d(TAG, "begin switch to table : " + uri + "/#" + uriType);
        val table: String
        when (uriType) {
            ACCOUNT, ACCOUNT_ID -> table = AccountTable.DATABASE_ACCOUNT_TABLE
            OPERATION, OPERATION_ID -> table = OperationTable.DATABASE_OPERATIONS_TABLE
            OPERATION_JOINED, OPERATION_JOINED_ID -> table = OperationTable.DATABASE_OP_TABLE_JOINTURE
            SCHEDULED_OP, SCHEDULED_OP_ID -> table = ScheduledOperationTable.DATABASE_SCHEDULED_TABLE
            SCHEDULED_JOINED_OP, SCHEDULED_JOINED_OP_ID -> table = ScheduledOperationTable.DATABASE_SCHEDULED_TABLE_JOINTURE
            THIRD_PARTY, THIRD_PARTY_ID -> table = InfoTables.DATABASE_THIRD_PARTIES_TABLE
            MODES, MODES_ID -> table = InfoTables.DATABASE_MODES_TABLE
            TAGS, TAGS_ID -> table = InfoTables.DATABASE_TAGS_TABLE
            PREFS, PREFS_ACCOUNT -> table = PreferenceTable.DATABASE_PREFS_TABLE
            STATS, STATS_ID -> table = StatisticTable.STAT_TABLE
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        return table
    }

    @Synchronized
    override fun query(uri: Uri, projection: Array<String>,
                       selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
        val queryBuilder = SQLiteQueryBuilder()
        val uriType = sURIMatcher.match(uri)
        when (uriType) {
            ACCOUNT_ID, OPERATION_ID, SCHEDULED_OP_ID, THIRD_PARTY_ID, MODES_ID, TAGS_ID, STATS_ID -> queryBuilder.appendWhere("_id=" + uri.lastPathSegment)
            OPERATION_JOINED_ID -> queryBuilder.appendWhere("ops._id=" + uri.lastPathSegment)
            SCHEDULED_JOINED_OP_ID -> queryBuilder.appendWhere("sch._id=" + uri.lastPathSegment)
            PREFS_ACCOUNT -> queryBuilder.appendWhere(PreferenceTable.KEY_PREFS_ACCOUNT + "=" + uri.lastPathSegment)
            else -> {
            }
        }
        val table = switchToTable(uri)
        queryBuilder.tables = table
        val db = mDbHelper!!.writableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    @Synchronized override fun delete(uri: Uri, selection: String?, a: Array<String>?): Int {
        var selectionArgs = a
        val uriType = sURIMatcher.match(uri)
        val db = mDbHelper!!.writableDatabase
        val rowsDeleted: Int
        val table = switchToTable(uri)
        var id: String? = null
        when (uriType) {
            ACCOUNT_ID, OPERATION_ID, OPERATION_JOINED_ID, SCHEDULED_OP_ID, SCHEDULED_JOINED_OP_ID, THIRD_PARTY_ID,
            MODES_ID, TAGS_ID, STATS_ID -> // no need for PREFS_ACCOUNT, trigger takes care of it
                id = uri.lastPathSegment
            else -> {
            }
        }

        if (id != null) {
            if (selection == null || selection.trim().length == 0) {
                rowsDeleted = db.delete(table, "_id=?", arrayOf(id))
            } else {
                if (selectionArgs != null) {
                    val args = ArrayList<String>(selectionArgs.size + 1)
                    args.add(id)
                    Collections.addAll(args, *selectionArgs)
                    selectionArgs = args.toArray<String>(arrayOfNulls<String>(args.size))
                } else {
                    selectionArgs = arrayOf(id)
                }
                rowsDeleted = db.delete(table, "_id=? and " + selection, selectionArgs)
            }
        } else {
            rowsDeleted = db.delete(table, selection, selectionArgs)
        }
        return rowsDeleted
    }

    override fun getType(arg0: Uri): String? {
        return null
    }

    @Synchronized override fun insert(uri: Uri, values: ContentValues): Uri {
        val uriType = sURIMatcher.match(uri)
        val db = mDbHelper!!.writableDatabase
        val id: Long
        val table = switchToTable(uri)
        val baseUrl: String
        when (uriType) {
            ACCOUNT -> baseUrl = ACCOUNTS_PATH
            OPERATION -> baseUrl = OPERATIONS_PATH
            OPERATION_JOINED -> baseUrl = OPERATIONS_JOINED_PATH
            SCHEDULED_OP -> baseUrl = SCHEDULED_OPS_PATH
            SCHEDULED_JOINED_OP -> baseUrl = SCHEDULED_JOINED_OPS_PATH
            THIRD_PARTY -> baseUrl = THIRD_PARTIES_PATH
            MODES -> baseUrl = MODES_PATH
            TAGS -> baseUrl = TAGS_PATH
            PREFS -> baseUrl = PREFS_PATH
            STATS -> baseUrl = STATS_PATH
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        id = db.insert(table, null, values)
        if (id > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return Uri.parse(baseUrl + "/" + id)
    }

    @Synchronized override fun update(uri: Uri, values: ContentValues, selection: String?, a: Array<String>?): Int {
        var selectionArgs = a
        val uriType = sURIMatcher.match(uri)
        val db = mDbHelper!!.writableDatabase
        var rowsUpdated: Int
        val table = switchToTable(uri)
        var id: String? = null
        var idKey = "_id"
        when (uriType) {
            PREFS_ACCOUNT -> {
                idKey = PreferenceTable.KEY_PREFS_ACCOUNT
                id = uri.lastPathSegment
            }
            ACCOUNT_ID, OPERATION_ID, OPERATION_JOINED_ID, SCHEDULED_OP_ID, SCHEDULED_JOINED_OP_ID, THIRD_PARTY_ID,
            MODES_ID, TAGS_ID, STATS_ID -> id = uri.lastPathSegment
            else -> {
            }
        }

        if (id != null) {
            if (selection == null || selection.trim().length == 0) {
                rowsUpdated = db.update(table, values, idKey + "=?", arrayOf(id))
            } else {
                if (selectionArgs != null) {
                    val args = ArrayList<String>(selectionArgs.size + 1)
                    args.add(id)
                    Collections.addAll(args, *selectionArgs)
                    selectionArgs = args.toArray<String>(arrayOfNulls<String>(args.size))
                } else {
                    selectionArgs = arrayOf(id)
                }

                rowsUpdated = db.update(table, values, idKey + "=? and " + selection, selectionArgs)
            }
        } else {
            rowsUpdated = db.update(table, values, selection, selectionArgs)
        }
        return rowsUpdated
    }

    companion object {
        private val AUTHORITY = "fr.geobert.radis.db"
        private val ACCOUNTS_PATH = "accounts"
        private val OPERATIONS_PATH = "operations"
        private val OPERATIONS_JOINED_PATH = "operations_joined"
        private val SCHEDULED_OPS_PATH = "scheduled_ops"
        private val SCHEDULED_JOINED_OPS_PATH = "scheduled_joined_ops"
        private val THIRD_PARTIES_PATH = "third_parties"
        private val TAGS_PATH = "tags"
        private val MODES_PATH = "modes"
        private val PREFS_PATH = "preferences"
        private val STATS_PATH = "statistics"
        private val TAG = "DbContentProvider"

        private val BASE_URI = "content://" + AUTHORITY

        val ACCOUNT_URI = Uri.parse(BASE_URI + "/" + ACCOUNTS_PATH)
        val OPERATION_URI = Uri.parse(BASE_URI + "/" + OPERATIONS_PATH)
        val OPERATION_JOINED_URI = Uri.parse(BASE_URI + "/" + OPERATIONS_JOINED_PATH)
        val SCHEDULED_OP_URI = Uri.parse(BASE_URI + "/" + SCHEDULED_OPS_PATH)
        val SCHEDULED_JOINED_OP_URI = Uri.parse(BASE_URI + "/" + SCHEDULED_JOINED_OPS_PATH)
        val THIRD_PARTY_URI = Uri.parse(BASE_URI + "/" + THIRD_PARTIES_PATH)
        val TAGS_URI = Uri.parse(BASE_URI + "/" + TAGS_PATH)
        val MODES_URI = Uri.parse(BASE_URI + "/" + MODES_PATH)
        val PREFS_URI = Uri.parse(BASE_URI + "/" + PREFS_PATH)
        val STATS_URI = Uri.parse(BASE_URI + "/" + STATS_PATH)

        private val ACCOUNT = 10
        private val OPERATION = 20
        private val OPERATION_JOINED = 21
        private val SCHEDULED_OP = 30
        private val SCHEDULED_JOINED_OP = 31
        private val THIRD_PARTY = 40
        private val TAGS = 50
        private val MODES = 60
        private val PREFS = 70
        private val STATS = 80
        private val ACCOUNT_ID = 15
        private val OPERATION_ID = 25
        private val OPERATION_JOINED_ID = 26
        private val SCHEDULED_OP_ID = 35
        private val SCHEDULED_JOINED_OP_ID = 36
        private val THIRD_PARTY_ID = 45
        private val TAGS_ID = 55
        private val MODES_ID = 65
        private val STATS_ID = 85
        private val PREFS_ACCOUNT = 75
        private val sURIMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH, ACCOUNT)
            sURIMatcher.addURI(AUTHORITY, OPERATIONS_PATH, OPERATION)
            sURIMatcher.addURI(AUTHORITY, OPERATIONS_JOINED_PATH, OPERATION_JOINED)
            sURIMatcher.addURI(AUTHORITY, SCHEDULED_OPS_PATH, SCHEDULED_OP)
            sURIMatcher.addURI(AUTHORITY, SCHEDULED_JOINED_OPS_PATH, SCHEDULED_JOINED_OP)
            sURIMatcher.addURI(AUTHORITY, THIRD_PARTIES_PATH, THIRD_PARTY)
            sURIMatcher.addURI(AUTHORITY, TAGS_PATH, TAGS)
            sURIMatcher.addURI(AUTHORITY, MODES_PATH, MODES)
            sURIMatcher.addURI(AUTHORITY, PREFS_PATH, PREFS)
            sURIMatcher.addURI(AUTHORITY, STATS_PATH, STATS)

            sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH + "/#", ACCOUNT_ID)
            sURIMatcher.addURI(AUTHORITY, OPERATIONS_PATH + "/#", OPERATION_ID)
            sURIMatcher.addURI(AUTHORITY, OPERATIONS_JOINED_PATH + "/#", OPERATION_JOINED_ID)
            sURIMatcher.addURI(AUTHORITY, SCHEDULED_OPS_PATH + "/#", SCHEDULED_OP_ID)
            sURIMatcher.addURI(AUTHORITY, SCHEDULED_JOINED_OPS_PATH + "/#", SCHEDULED_JOINED_OP_ID)
            sURIMatcher.addURI(AUTHORITY, THIRD_PARTIES_PATH + "/#", THIRD_PARTY_ID)
            sURIMatcher.addURI(AUTHORITY, TAGS_PATH + "/#", TAGS_ID)
            sURIMatcher.addURI(AUTHORITY, MODES_PATH + "/#", MODES_ID)
            sURIMatcher.addURI(AUTHORITY, STATS_PATH + "/#", STATS_ID)
            sURIMatcher.addURI(AUTHORITY, PREFS_PATH + "/#", PREFS_ACCOUNT)
        }

        private var mDbHelper: DbHelper? = null

        fun reinit(ctx: Context) {
            mDbHelper = DbHelper.getInstance(ctx)
        }


        fun close() {
            DbHelper.delete()
        }
    }

}
