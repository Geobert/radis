package fr.geobert.radis.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DbContentProvider extends ContentProvider {
    private static final String AUTHORITY = "fr.geobert.radis.db";
    private static final String ACCOUNTS_PATH = "accounts";
    private static final String OPERATIONS_PATH = "operations";
    private static final String OPERATIONS_JOINED_PATH = "operations_joined";
    private static final String SCHEDULED_OPS_PATH = "scheduled_ops";
    private static final String SCHEDULED_JOINED_OPS_PATH = "scheduled_joined_ops";
    private static final String THIRD_PARTIES_PATH = "third_parties";
    private static final String TAGS_PATH = "tags";
    private static final String MODES_PATH = "modes";
    private static final String PREFS_PATH = "preferences";
    private static final String STATS_PATH = "statistics";
    private static final String TAG = "DbContentProvider";

    public static final Uri ACCOUNT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + ACCOUNTS_PATH);
    public static final Uri OPERATION_URI = Uri.parse("content://" + AUTHORITY
            + "/" + OPERATIONS_PATH);
    public static final Uri OPERATION_JOINED_URI = Uri.parse("content://"
            + AUTHORITY + "/" + OPERATIONS_JOINED_PATH);
    public static final Uri SCHEDULED_OP_URI = Uri.parse("content://"
            + AUTHORITY + "/" + SCHEDULED_OPS_PATH);
    public static final Uri SCHEDULED_JOINED_OP_URI = Uri.parse("content://"
            + AUTHORITY + "/" + SCHEDULED_JOINED_OPS_PATH);
    public static final Uri THIRD_PARTY_URI = Uri.parse("content://"
            + AUTHORITY + "/" + THIRD_PARTIES_PATH);
    public static final Uri TAGS_URI = Uri.parse("content://" + AUTHORITY + "/"
            + TAGS_PATH);
    public static final Uri MODES_URI = Uri.parse("content://" + AUTHORITY
            + "/" + MODES_PATH);
    public static final Uri PREFS_URI = Uri.parse("content://" + AUTHORITY
            + "/" + PREFS_PATH);
    public static final Uri STATS_URI = Uri.parse("content://" + AUTHORITY
            + "/" + STATS_PATH);

    private static final int ACCOUNT = 10;
    private static final int OPERATION = 20;
    private static final int OPERATION_JOINED = 21;
    private static final int SCHEDULED_OP = 30;
    private static final int SCHEDULED_JOINED_OP = 31;
    private static final int THIRD_PARTY = 40;
    private static final int TAGS = 50;
    private static final int MODES = 60;
    private static final int PREFS = 70;
    private static final int STATS = 80;
    private static final int ACCOUNT_ID = 15;
    private static final int OPERATION_ID = 25;
    private static final int OPERATION_JOINED_ID = 26;
    private static final int SCHEDULED_OP_ID = 35;
    private static final int SCHEDULED_JOINED_OP_ID = 36;
    private static final int THIRD_PARTY_ID = 45;
    private static final int TAGS_ID = 55;
    private static final int MODES_ID = 65;
    private static final int STATS_ID = 85;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH, ACCOUNT);
        sURIMatcher.addURI(AUTHORITY, OPERATIONS_PATH, OPERATION);
        sURIMatcher.addURI(AUTHORITY, OPERATIONS_JOINED_PATH, OPERATION_JOINED);
        sURIMatcher.addURI(AUTHORITY, SCHEDULED_OPS_PATH, SCHEDULED_OP);
        sURIMatcher.addURI(AUTHORITY, SCHEDULED_JOINED_OPS_PATH, SCHEDULED_JOINED_OP);
        sURIMatcher.addURI(AUTHORITY, THIRD_PARTIES_PATH, THIRD_PARTY);
        sURIMatcher.addURI(AUTHORITY, TAGS_PATH, TAGS);
        sURIMatcher.addURI(AUTHORITY, MODES_PATH, MODES);
        sURIMatcher.addURI(AUTHORITY, PREFS_PATH, PREFS);
        sURIMatcher.addURI(AUTHORITY, STATS_PATH, STATS);

        sURIMatcher.addURI(AUTHORITY, ACCOUNTS_PATH + "/#", ACCOUNT_ID);
        sURIMatcher.addURI(AUTHORITY, OPERATIONS_PATH + "/#", OPERATION_ID);
        sURIMatcher.addURI(AUTHORITY, OPERATIONS_JOINED_PATH + "/#", OPERATION_JOINED_ID);
        sURIMatcher.addURI(AUTHORITY, SCHEDULED_OPS_PATH + "/#", SCHEDULED_OP_ID);
        sURIMatcher.addURI(AUTHORITY, SCHEDULED_JOINED_OPS_PATH + "/#", SCHEDULED_JOINED_OP_ID);
        sURIMatcher.addURI(AUTHORITY, THIRD_PARTIES_PATH + "/#", THIRD_PARTY_ID);
        sURIMatcher.addURI(AUTHORITY, TAGS_PATH + "/#", TAGS_ID);
        sURIMatcher.addURI(AUTHORITY, MODES_PATH + "/#", MODES_ID);
        sURIMatcher.addURI(AUTHORITY, STATS_PATH + "/#", STATS_ID);
    }

    private static DbHelper mDbHelper = null;

    public static void reinit(Context ctx) {
        close();
        mDbHelper = new DbHelper(ctx);
    }

    public static void close() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate DbContentProvider in context : " + getContext().getClass().toString());
        mDbHelper = new DbHelper(getContext());
        return false;
    }

    public void deleteDatabase(Context ctx) {
        Log.d(TAG, "deleteDatabase from ContentProvider");
        mDbHelper.close();
        ctx.deleteDatabase(DbHelper.DATABASE_NAME);
        mDbHelper = new DbHelper(ctx);
    }

    private String switchToTable(Uri uri) {
        int uriType = sURIMatcher.match(uri);
//		Log.d(TAG, "begin switch to table : " + uri + "/#" + uriType);
        String table;
        switch (uriType) {
            case ACCOUNT:
            case ACCOUNT_ID:
                table = AccountTable.DATABASE_ACCOUNT_TABLE;
                break;
            case OPERATION:
            case OPERATION_ID:
                table = OperationTable.DATABASE_OPERATIONS_TABLE;
                break;
            case OPERATION_JOINED:
            case OPERATION_JOINED_ID:
                table = OperationTable.DATABASE_OP_TABLE_JOINTURE;
                break;
            case SCHEDULED_OP:
            case SCHEDULED_OP_ID:
                table = ScheduledOperationTable.DATABASE_SCHEDULED_TABLE;
                break;
            case SCHEDULED_JOINED_OP:
            case SCHEDULED_JOINED_OP_ID:
                table = ScheduledOperationTable.DATABASE_SCHEDULED_TABLE_JOINTURE;
                break;
            case THIRD_PARTY:
            case THIRD_PARTY_ID:
                table = InfoTables.DATABASE_THIRD_PARTIES_TABLE;
                break;
            case MODES:
            case MODES_ID:
                table = InfoTables.DATABASE_MODES_TABLE;
                break;
            case TAGS:
            case TAGS_ID:
                table = InfoTables.DATABASE_TAGS_TABLE;
                break;
            case PREFS:
                table = PreferenceTable.DATABASE_PREFS_TABLE;
                break;
            case STATS:
            case STATS_ID:
                table = StatisticTable.STAT_TABLE();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return table;
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] projection,
                                     String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case ACCOUNT_ID:
            case OPERATION_ID:
            case SCHEDULED_OP_ID:
            case THIRD_PARTY_ID:
            case MODES_ID:
            case TAGS_ID:
            case STATS_ID:
                queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                break;
            case OPERATION_JOINED_ID:
                queryBuilder.appendWhere("ops._id=" + uri.getLastPathSegment());
                break;
            case SCHEDULED_JOINED_OP_ID:
                queryBuilder.appendWhere("sch._id=" + uri.getLastPathSegment());
                break;
            default:
                break;
        }
        String table = switchToTable(uri);
        queryBuilder.setTables(table);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public synchronized int delete(Uri uri, String selection,
                                   String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        String table = switchToTable(uri);
        String id = null;
        switch (uriType) {
            case ACCOUNT_ID:
            case OPERATION_ID:
            case OPERATION_JOINED_ID:
            case SCHEDULED_OP_ID:
            case SCHEDULED_JOINED_OP_ID:
            case THIRD_PARTY_ID:
            case MODES_ID:
            case TAGS_ID:
            case STATS_ID:
                id = uri.getLastPathSegment();
                break;
            default:
                break;
        }

        if (id != null) {
            if (selection == null || selection.trim().length() == 0) {
                rowsDeleted = db.delete(table, "_id=?", new String[]{id});
            } else {
                if (selectionArgs != null) {
                    List<String> args = new ArrayList<String>(selectionArgs.length + 1);
                    args.add(id);
                    Collections.addAll(args, selectionArgs);
                    selectionArgs = args.toArray(new String[args.size()]);
                } else {
                    selectionArgs = new String[]{id};
                }
                rowsDeleted = db.delete(table, "_id=? and " + selection, selectionArgs);
            }
        } else {
            rowsDeleted = db.delete(table, selection, selectionArgs);
        }
        return rowsDeleted;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id;
        String table = switchToTable(uri);
        String baseUrl;
        switch (uriType) {
            case ACCOUNT:
                baseUrl = ACCOUNTS_PATH;
                break;
            case OPERATION:
                baseUrl = OPERATIONS_PATH;
                break;
            case OPERATION_JOINED:
                baseUrl = OPERATIONS_JOINED_PATH;
                break;
            case SCHEDULED_OP:
                baseUrl = SCHEDULED_OPS_PATH;
                break;
            case SCHEDULED_JOINED_OP:
                baseUrl = SCHEDULED_JOINED_OPS_PATH;
                break;
            case THIRD_PARTY:
                baseUrl = THIRD_PARTIES_PATH;
                break;
            case MODES:
                baseUrl = MODES_PATH;
                break;
            case TAGS:
                baseUrl = TAGS_PATH;
                break;
            case PREFS:
                baseUrl = PREFS_PATH;
                break;
            case STATS:
                baseUrl = STATS_PATH;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        id = db.insert(table, null, values);
        if (id > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.parse(baseUrl + "/" + id);
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int rowsUpdated = 0;
        String table = switchToTable(uri);
        String id = null;
        switch (uriType) {
            case ACCOUNT_ID:
            case OPERATION_ID:
            case OPERATION_JOINED_ID:
            case SCHEDULED_OP_ID:
            case SCHEDULED_JOINED_OP_ID:
            case THIRD_PARTY_ID:
            case MODES_ID:
            case TAGS_ID:
            case STATS_ID:
                id = uri.getLastPathSegment();
                break;
            default:
                break;
        }

        if (id != null) {
            if (selection == null || selection.trim().length() == 0) {
                rowsUpdated = db.update(table, values, "_id=?", new String[]{id});
            } else {
                if (selectionArgs != null) {
                    List<String> args = new ArrayList<String>(selectionArgs.length + 1);
                    args.add(id);
                    Collections.addAll(args, selectionArgs);
                    selectionArgs = args.toArray(new String[args.size()]);
                } else {
                    selectionArgs = new String[]{id};
                }
                rowsUpdated = db.update(table, values, "_id=? and " + selection, selectionArgs);
            }
        } else {
            rowsUpdated = db.update(table, values, selection, selectionArgs);
        }
        return rowsUpdated;
    }

}
