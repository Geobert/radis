package fr.geobert.radis.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.acra.ErrorReporter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import fr.geobert.radis.Operation;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.tools.AsciiUtils;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.PrefsManager;
import fr.geobert.radis.tools.ProjectionDateController;
import fr.geobert.radis.tools.Tools;

public class CommonDbAdapter {
	private static final String TAG = "CommonDbAdapter";
	protected static final String DATABASE_NAME = "radisDb";
	protected static final int DATABASE_VERSION = 11;

	public static final String DATABASE_ACCOUNT_TABLE = "accounts";
	public static final String DATABASE_MODES_TABLE = "modes";
	public static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
	public static final String DATABASE_TAGS_TABLE = "tags";
	public static final String DATABASE_OPERATIONS_TABLE = "operations";
	public static final String DATABASE_SCHEDULED_TABLE = "scheduled_ops";
	public static final String DATABASE_PREFS_TABLE = "preferences";

	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ACCOUNT_DESC = "account_desc";
	public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
	public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum";
	public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
	public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
	public static final String KEY_ACCOUNT_ROWID = "_id";
	public static final String KEY_ACCOUNT_CUR_SUM_DATE = "account_current_sum_date";
	public static final String KEY_ACCOUNT_PROJECTION_MODE = "account_projection_mode";
	public static final String KEY_ACCOUNT_PROJECTION_DATE = "account_projection_date";

	private static final String DATABASE_ACCOUNT_CREATE_v7 = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME + " text not null, "
			+ KEY_ACCOUNT_DESC + " text not null, " + KEY_ACCOUNT_START_SUM + " integer not null, "
			+ KEY_ACCOUNT_OP_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM
			+ " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE + " integer not null, "
			+ KEY_ACCOUNT_CURRENCY + " text not null);";

	private static final String DATABASE_ACCOUNT_CREATE = "create table " + DATABASE_ACCOUNT_TABLE
			+ "(" + KEY_ACCOUNT_ROWID + " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, " + KEY_ACCOUNT_START_SUM
			+ " integer not null, " + KEY_ACCOUNT_OP_SUM + " integer not null, "
			+ KEY_ACCOUNT_CUR_SUM + " integer not null, " + KEY_ACCOUNT_CUR_SUM_DATE
			+ " integer not null, " + KEY_ACCOUNT_CURRENCY + " text not null, "
			+ KEY_ACCOUNT_PROJECTION_MODE + " integer not null, " + KEY_ACCOUNT_PROJECTION_DATE
			+ " string);";

	public static final String KEY_THIRD_PARTY_ROWID = "_id";
	public static final String KEY_THIRD_PARTY_NAME = "third_party_name";
	public static final String KEY_THIRD_PARTY_NORMALIZED_NAME = "third_party_norm_name";

	private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME + " text not null, "
			+ KEY_THIRD_PARTY_NORMALIZED_NAME + " text not null);";

	public static final String KEY_TAG_ROWID = "_id";
	public static final String KEY_TAG_NAME = "tag_name";
	public static final String KEY_TAG_NORMALIZED_NAME = "tag_norm_name";

	private static final String DATABASE_TAGS_CREATE = "create table " + DATABASE_TAGS_TABLE + "("
			+ KEY_TAG_ROWID + " integer primary key autoincrement, " + KEY_TAG_NAME
			+ " text not null, " + KEY_TAG_NORMALIZED_NAME + " text not null);";

	public static final String KEY_MODE_ROWID = "_id";
	public static final String KEY_MODE_NAME = "mode_name";
	public static final String KEY_MODE_NORMALIZED_NAME = "mode_norm_name";

	private static final String DATABASE_MODES_CREATE = "create table " + DATABASE_MODES_TABLE
			+ "(" + KEY_MODE_ROWID + " integer primary key autoincrement, " + KEY_MODE_NAME
			+ " text not null, " + KEY_MODE_NORMALIZED_NAME + " text not null);";

	public static final String KEY_OP_DATE = "date";
	public static final String KEY_OP_THIRD_PARTY = "third_party";
	public static final String KEY_OP_TAG = "tag";
	public static final String KEY_OP_MODE = "mode";
	public static final String KEY_OP_SUM = "sum";
	public static final String KEY_OP_SCHEDULED_ID = "scheduled_id";
	public static final String KEY_OP_ACCOUNT_ID = "account_id";
	public static final String KEY_OP_ROWID = "_id";
	public static final String KEY_OP_NOTES = "notes";

	public static final String KEY_SCHEDULED_END_DATE = "end_date";
	public static final String KEY_SCHEDULED_PERIODICITY = "periodicity";
	public static final String KEY_SCHEDULED_ACCOUNT_ID = "scheduled_account_id";
	public static final String KEY_SCHEDULED_ROWID = "_id";
	public static final String KEY_SCHEDULED_PERIODICITY_UNIT = "periodicity_units";

	protected static final String DATABASE_SCHEDULED_CREATE = "create table "
			+ DATABASE_SCHEDULED_TABLE + "(" + KEY_SCHEDULED_ROWID
			+ " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY + " integer, "
			+ KEY_OP_TAG + " integer, " + KEY_OP_SUM + " integer not null, "
			+ KEY_SCHEDULED_ACCOUNT_ID + " integer not null, " + KEY_OP_MODE + " integer, "
			+ KEY_OP_DATE + " integer not null, " + KEY_SCHEDULED_END_DATE + " integer, "
			+ KEY_SCHEDULED_PERIODICITY + " integer, " + KEY_SCHEDULED_PERIODICITY_UNIT
			+ " integer not null, " + KEY_OP_NOTES + " text, FOREIGN KEY (" + KEY_OP_THIRD_PARTY
			+ ") REFERENCES " + DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_TAG + ") REFERENCES " + DATABASE_TAGS_TABLE + "("
			+ KEY_TAG_ROWID + "), FOREIGN KEY (" + KEY_OP_MODE + ") REFERENCES "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID + "));";

	protected static final String DATABASE_OP_CREATE = "create table " + DATABASE_OPERATIONS_TABLE
			+ "(" + KEY_OP_ROWID + " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY
			+ " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM + " integer not null, "
			+ KEY_OP_ACCOUNT_ID + " integer not null, " + KEY_OP_MODE + " integer, " + KEY_OP_DATE
			+ " integer not null, " + KEY_OP_NOTES + " text, " + KEY_OP_SCHEDULED_ID
			+ " integer, FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY ("
			+ KEY_OP_TAG + ") REFERENCES " + DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_MODE + ") REFERENCES " + DATABASE_MODES_TABLE + "("
			+ KEY_MODE_ROWID + "), FOREIGN KEY (" + KEY_OP_SCHEDULED_ID + ") REFERENCES "
			+ DATABASE_SCHEDULED_TABLE + "(" + KEY_SCHEDULED_ROWID + "));";

	public static final String KEY_PREFS_ROWID = "_id";
	public static final String KEY_PREFS_NAME = "pref_name";
	public static final String KEY_PREFS_VALUE = "pref_value";
	protected static final String DATABASE_PREFS_CREATE = "create table " + DATABASE_PREFS_TABLE
			+ "(" + KEY_PREFS_ROWID + " integer primary key autoincrement, " + KEY_PREFS_NAME
			+ " text not null, " + KEY_PREFS_VALUE + " text not null);";

	// meta
	protected static final String INDEX_ON_ACCOUNT_ID_CREATE = "CREATE INDEX IF NOT EXISTS account_id_idx ON "
			+ DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ACCOUNT_ID + ")";

	protected static final String TRIGGER_ON_DELETE_THIRD_PARTY_CREATE = "CREATE TRIGGER on_delete_third_party AFTER DELETE ON "
			+ DATABASE_THIRD_PARTIES_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_THIRD_PARTY
			+ " = null WHERE "
			+ KEY_OP_THIRD_PARTY
			+ " = old."
			+ KEY_THIRD_PARTY_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_THIRD_PARTY
			+ " = null WHERE "
			+ KEY_OP_THIRD_PARTY
			+ " = old."
			+ KEY_THIRD_PARTY_ROWID + "; END";
	protected static final String TRIGGER_ON_DELETE_MODE_CREATE = "CREATE TRIGGER on_delete_mode AFTER DELETE ON "
			+ DATABASE_MODES_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_MODE
			+ " = null WHERE "
			+ KEY_OP_MODE
			+ " = old."
			+ KEY_MODE_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_MODE
			+ " = null WHERE "
			+ KEY_OP_MODE + " = old." + KEY_MODE_ROWID + "; END";
	protected static final String TRIGGER_ON_DELETE_TAG_CREATE = "CREATE TRIGGER on_delete_tag AFTER DELETE ON "
			+ DATABASE_TAGS_TABLE
			+ " BEGIN UPDATE "
			+ DATABASE_OPERATIONS_TABLE
			+ " SET "
			+ KEY_OP_TAG
			+ " = null WHERE "
			+ KEY_OP_TAG
			+ " = old."
			+ KEY_TAG_ROWID
			+ "; UPDATE "
			+ DATABASE_SCHEDULED_TABLE
			+ " SET "
			+ KEY_OP_TAG
			+ " = null WHERE "
			+ KEY_OP_TAG
			+ " = old." + KEY_TAG_ROWID + "; END";
	protected static final String ADD_NOTES_COLUNM = "ALTER TABLE " + DATABASE_OPERATIONS_TABLE
			+ " ADD COLUMN op_notes text";
	protected static final String ADD_CUR_DATE_COLUNM = "ALTER TABLE " + DATABASE_ACCOUNT_TABLE
			+ " ADD COLUMN " + KEY_ACCOUNT_CUR_SUM_DATE + " integer not null DEFAULT 0";
	protected static final String ADD_NORMALIZED_THIRD_PARTY = "ALTER TABLE "
			+ DATABASE_THIRD_PARTIES_TABLE + " ADD COLUMN " + KEY_THIRD_PARTY_NORMALIZED_NAME
			+ " text not null DEFAULT ''";
	protected static final String ADD_NORMALIZED_TAG = "ALTER TABLE " + DATABASE_TAGS_TABLE
			+ " ADD COLUMN " + KEY_TAG_NORMALIZED_NAME + " text not null DEFAULT ''";
	protected static final String ADD_NORMALIZED_MODE = "ALTER TABLE " + DATABASE_MODES_TABLE
			+ " ADD COLUMN " + KEY_MODE_NORMALIZED_NAME + " text not null DEFAULT ''";
	protected static final String ADD_PROJECTION_MODE_COLUNM = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN " + KEY_ACCOUNT_PROJECTION_MODE
			+ " integer not null DEFAULT 0";
	protected static final String ADD_PROJECTION_MODE_DATE = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN " + KEY_ACCOUNT_PROJECTION_DATE + " string";

	private LinkedHashMap<String, Long> mModesMap;
	private LinkedHashMap<String, Long> mTagsMap;
	private LinkedHashMap<String, Long> mThirdPartiesMap;

	private HashMap<String, Cursor> mInfoCursorMap;

	@SuppressWarnings("serial")
	private static final HashMap<String, String> mInfoColMap = new HashMap<String, String>() {
		{
			put(DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
			put(DATABASE_TAGS_TABLE, KEY_TAG_NAME);
			put(DATABASE_MODES_TABLE, KEY_MODE_NAME);
		}
	};

	private static final String OP_ORDERING = "ops." + KEY_OP_DATE + " desc, ops." + KEY_OP_ROWID
			+ " desc";

	private final String DATABASE_OP_TABLE_JOINTURE = DATABASE_OPERATIONS_TABLE
			+ " ops LEFT OUTER JOIN " + DATABASE_THIRD_PARTIES_TABLE + " tp ON ops."
			+ KEY_OP_THIRD_PARTY + " = tp." + KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE + " mode ON ops." + KEY_OP_MODE + " = mode." + KEY_MODE_ROWID
			+ " LEFT OUTER JOIN " + DATABASE_TAGS_TABLE + " tag ON ops." + KEY_OP_TAG + " = tag."
			+ KEY_TAG_ROWID;

	public static final String[] OP_COLS_QUERY = { "ops." + KEY_OP_ROWID,
			"tp." + KEY_THIRD_PARTY_NAME, "tag." + KEY_TAG_NAME, "mode." + KEY_MODE_NAME,
			"ops." + KEY_OP_SUM, "ops." + KEY_OP_DATE, "ops." + KEY_OP_ACCOUNT_ID,
			"ops." + KEY_OP_NOTES, "ops." + KEY_OP_SCHEDULED_ID };

	private static final String RESTRICT_TO_ACCOUNT = "ops." + KEY_OP_ACCOUNT_ID + " = %d";

	private static final String SCHEDULED_OP_ORDERING = "sch." + KEY_OP_DATE + " desc, sch."
			+ KEY_SCHEDULED_ROWID + " desc";

	private final String DATABASE_SCHEDULED_TABLE_JOINTURE = DATABASE_SCHEDULED_TABLE
			+ " sch LEFT OUTER JOIN " + DATABASE_THIRD_PARTIES_TABLE + " tp ON sch."
			+ KEY_OP_THIRD_PARTY + " = tp." + KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE + " mode ON sch." + KEY_OP_MODE + " = mode." + KEY_MODE_ROWID
			+ " LEFT OUTER JOIN " + DATABASE_TAGS_TABLE + " tag ON sch." + KEY_OP_TAG + " = tag."
			+ KEY_TAG_ROWID + " LEFT OUTER JOIN " + DATABASE_ACCOUNT_TABLE + " acc ON sch."
			+ KEY_SCHEDULED_ACCOUNT_ID + " = acc." + KEY_ACCOUNT_ROWID;

	public static final String[] SCHEDULED_OP_COLS_QUERY = { "sch." + KEY_SCHEDULED_ROWID,
			"tp." + KEY_THIRD_PARTY_NAME, "tag." + KEY_TAG_NAME, "mode." + KEY_MODE_NAME,
			"sch." + KEY_OP_SUM, "sch." + KEY_OP_DATE, "sch." + KEY_SCHEDULED_ACCOUNT_ID,
			"acc." + KEY_ACCOUNT_NAME, "sch." + KEY_OP_NOTES, "sch." + KEY_SCHEDULED_END_DATE,
			"sch." + KEY_SCHEDULED_PERIODICITY, "sch." + KEY_SCHEDULED_PERIODICITY_UNIT };

	@SuppressWarnings("serial")
	private static final HashMap<String, String> mColNameNormName = new HashMap<String, String>() {
		{
			put(CommonDbAdapter.KEY_THIRD_PARTY_NAME,
					CommonDbAdapter.KEY_THIRD_PARTY_NORMALIZED_NAME);
			put(CommonDbAdapter.KEY_TAG_NAME, CommonDbAdapter.KEY_TAG_NORMALIZED_NAME);
			put(CommonDbAdapter.KEY_MODE_NAME, CommonDbAdapter.KEY_MODE_NORMALIZED_NAME);
		}
	};

	protected DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;
	protected Context mCtx;
	protected int mProjectionMode;
	protected long mProjectionDate;

	private static CommonDbAdapter mInstance = null;

	public CommonDbAdapter() {
		mInfoCursorMap = new HashMap<String, Cursor>();
	}

	private void init(Context ctx) {
		this.mCtx = ctx;
	}

	public static CommonDbAdapter getInstance(Context ctx) {
		if (null == mInstance) {
			mInstance = new CommonDbAdapter();
		}
		mInstance.init(ctx);
		return mInstance;
	}

	protected class DatabaseHelper extends SQLiteOpenHelper {
		private Context mCtx;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_ACCOUNT_CREATE);
			db.execSQL(DATABASE_OP_CREATE);
			db.execSQL(DATABASE_THIRD_PARTIES_CREATE);
			db.execSQL(DATABASE_MODES_CREATE);
			db.execSQL(DATABASE_TAGS_CREATE);
			db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
			db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
			db.execSQL(DATABASE_SCHEDULED_CREATE);
			db.execSQL(DATABASE_PREFS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", converting to new database");
			switch (oldVersion) {
			case 1: {
				db.execSQL(DATABASE_OP_CREATE);
				Cursor allAccounts = db.query(DATABASE_ACCOUNT_TABLE, new String[] {}, null, null,
						null, null, null);
				if (null != allAccounts) {
					if (allAccounts.moveToFirst()) {
						do {
							int accountId = allAccounts.getInt(allAccounts
									.getColumnIndex(KEY_ACCOUNT_ROWID));
							String oldTableName = "ops_of_account_" + accountId;
							db.execSQL("INSERT INTO operations ("
									+ KEY_OP_ACCOUNT_ID
									+ ", "
									+ KEY_OP_THIRD_PARTY
									+ ", "
									+ KEY_OP_TAG
									+ ", "
									+ KEY_OP_SUM
									+ ", "
									+ KEY_OP_MODE
									+ ", "
									+ KEY_OP_DATE
									+ ", "
									+ KEY_OP_SCHEDULED_ID
									+ ") SELECT "
									+ accountId
									+ ", old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id FROM "
									+ oldTableName + " old;");
							db.execSQL("DROP TABLE " + oldTableName + ";");
						} while (allAccounts.moveToNext());
					}
					allAccounts.close();
					db.execSQL(INDEX_ON_ACCOUNT_ID_CREATE);
					db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
				}
			}
			case 2:
				db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
			case 3:
				db.execSQL(ADD_NOTES_COLUNM);
			case 4:
				db.execSQL(ADD_CUR_DATE_COLUNM);
			case 5:
				// must recreate operations table as ALTER does not support
				// ALTER COLUMN in SQLITE
				db.execSQL("DROP TRIGGER on_delete_third_party");
				db.execSQL("DROP TRIGGER on_delete_mode");
				db.execSQL("DROP TRIGGER on_delete_tag");

				db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
				db.execSQL(DATABASE_SCHEDULED_CREATE);
				db.execSQL(DATABASE_OP_CREATE);
				db.execSQL("INSERT INTO operations ("
						+ KEY_OP_ACCOUNT_ID
						+ ", "
						+ KEY_OP_THIRD_PARTY
						+ ", "
						+ KEY_OP_TAG
						+ ", "
						+ KEY_OP_SUM
						+ ", "
						+ KEY_OP_MODE
						+ ", "
						+ KEY_OP_DATE
						+ ", "
						+ KEY_OP_SCHEDULED_ID
						+ ", "
						+ KEY_OP_NOTES
						+ ") SELECT old.op_account_id, old.op_third_party, old.op_tag, old.op_sum, old.op_mode, old.op_date, old.op_scheduled_id, old.op_notes FROM operations_old old;");
				db.execSQL("DROP TABLE operations_old;");
				db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);
			case 6: {
				db.execSQL("DROP TRIGGER on_delete_third_party");
				db.execSQL("DROP TRIGGER on_delete_mode");
				db.execSQL("DROP TRIGGER on_delete_tag");

				db.execSQL("ALTER TABLE accounts RENAME TO accounts_old;");
				db.execSQL(DATABASE_ACCOUNT_CREATE_v7);
				Cursor c = db.query("accounts_old", new String[] { KEY_ACCOUNT_ROWID,
						KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY,
						KEY_ACCOUNT_CUR_SUM_DATE, KEY_ACCOUNT_DESC, KEY_ACCOUNT_OP_SUM,
						KEY_ACCOUNT_START_SUM }, null, null, null, null, null);
				if (null != c && c.moveToFirst()) {
					do {
						ContentValues initialValues = new ContentValues();
						initialValues.put(KEY_ACCOUNT_NAME,
								c.getString(c.getColumnIndex(KEY_ACCOUNT_NAME)));
						initialValues.put(KEY_ACCOUNT_DESC,
								c.getString(c.getColumnIndex(KEY_ACCOUNT_DESC)));
						double d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_START_SUM));
						long l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_START_SUM, l);
						d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_OP_SUM));
						l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_OP_SUM, l);
						d = c.getDouble(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM));
						l = Math.round(d * 100);
						initialValues.put(KEY_ACCOUNT_CUR_SUM, l);
						initialValues.put(KEY_ACCOUNT_CURRENCY,
								c.getString(c.getColumnIndex(KEY_ACCOUNT_CURRENCY)));
						initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE,
								c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE)));
						long id = db.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
						initialValues = new ContentValues();
						initialValues.put(KEY_OP_ACCOUNT_ID, id);
						db.update(DATABASE_OPERATIONS_TABLE, initialValues, KEY_OP_ACCOUNT_ID + "="
								+ c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)), null);

					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("ALTER TABLE scheduled_ops RENAME TO scheduled_ops_old;");
				db.execSQL(DATABASE_SCHEDULED_CREATE);
				c = db.query("scheduled_ops_old", new String[] { KEY_SCHEDULED_ROWID,
						KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_SCHEDULED_ACCOUNT_ID,
						KEY_OP_MODE, KEY_OP_DATE, KEY_SCHEDULED_END_DATE,
						KEY_SCHEDULED_PERIODICITY, KEY_SCHEDULED_PERIODICITY_UNIT, KEY_OP_NOTES },
						null, null, null, null, null);
				if (null != c && c.moveToFirst()) {
					do {
						ContentValues initialValues = new ContentValues();
						initialValues.put(KEY_OP_THIRD_PARTY,
								c.getInt(c.getColumnIndex(KEY_OP_THIRD_PARTY)));
						initialValues.put(KEY_OP_TAG, c.getInt(c.getColumnIndex(KEY_OP_TAG)));
						double d = c.getDouble(c.getColumnIndex(KEY_OP_SUM));
						long l = Math.round(d * 100);
						initialValues.put(KEY_OP_SUM, l);
						initialValues.put(KEY_SCHEDULED_ACCOUNT_ID,
								c.getInt(c.getColumnIndex(KEY_SCHEDULED_ACCOUNT_ID)));
						initialValues.put(KEY_OP_MODE, c.getInt(c.getColumnIndex(KEY_OP_MODE)));
						initialValues.put(KEY_OP_DATE, c.getLong(c.getColumnIndex(KEY_OP_DATE)));
						initialValues.put(KEY_SCHEDULED_END_DATE,
								c.getLong(c.getColumnIndex(KEY_SCHEDULED_END_DATE)));
						initialValues.put(KEY_SCHEDULED_PERIODICITY,
								c.getInt(c.getColumnIndex(KEY_SCHEDULED_PERIODICITY)));
						initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT,
								c.getInt(c.getColumnIndex(KEY_SCHEDULED_PERIODICITY_UNIT)));
						initialValues
								.put(KEY_OP_NOTES, c.getString(c.getColumnIndex(KEY_OP_NOTES)));
						long id = db.insert(DATABASE_SCHEDULED_TABLE, null, initialValues);
						initialValues = new ContentValues();
						initialValues.put(KEY_OP_SCHEDULED_ID, id);
						db.update(DATABASE_OPERATIONS_TABLE, initialValues, KEY_OP_SCHEDULED_ID
								+ "=" + c.getLong(c.getColumnIndex(KEY_SCHEDULED_ROWID)), null);

					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("ALTER TABLE operations RENAME TO operations_old;");
				db.execSQL(DATABASE_OP_CREATE);
				c = db.query("operations_old", new String[] { KEY_OP_ROWID, KEY_OP_ACCOUNT_ID,
						KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_OP_MODE, KEY_OP_DATE,
						KEY_OP_SCHEDULED_ID, KEY_OP_NOTES }, null, null, null, null, null);
				if (null != c && c.moveToFirst()) {
					do {
						ContentValues initialValues = new ContentValues();
						initialValues.put(KEY_OP_THIRD_PARTY,
								c.getInt(c.getColumnIndex(KEY_OP_THIRD_PARTY)));
						initialValues.put(KEY_OP_TAG, c.getInt(c.getColumnIndex(KEY_OP_TAG)));
						double d = c.getDouble(c.getColumnIndex(KEY_OP_SUM));
						long l = Math.round(d * 100);
						initialValues.put(KEY_OP_SUM, l);
						initialValues.put(KEY_OP_ACCOUNT_ID,
								c.getLong(c.getColumnIndex(KEY_OP_ACCOUNT_ID)));
						initialValues.put(KEY_OP_MODE, c.getInt(c.getColumnIndex(KEY_OP_MODE)));
						initialValues.put(KEY_OP_DATE, c.getLong(c.getColumnIndex(KEY_OP_DATE)));
						initialValues.put(KEY_OP_SCHEDULED_ID,
								c.getLong(c.getColumnIndex(KEY_OP_SCHEDULED_ID)));
						initialValues
								.put(KEY_OP_NOTES, c.getString(c.getColumnIndex(KEY_OP_NOTES)));
						db.insert(DATABASE_OPERATIONS_TABLE, null, initialValues);
					} while (c.moveToNext());
					c.close();
				}

				db.execSQL("DROP TABLE accounts_old;");
				db.execSQL("DROP TABLE operations_old;");
				db.execSQL("DROP TABLE scheduled_ops_old;");
				db.execSQL(TRIGGER_ON_DELETE_THIRD_PARTY_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_MODE_CREATE);
				db.execSQL(TRIGGER_ON_DELETE_TAG_CREATE);

			}
			case 7: {
				db.execSQL(ADD_NORMALIZED_THIRD_PARTY);
				db.execSQL(ADD_NORMALIZED_TAG);
				db.execSQL(ADD_NORMALIZED_MODE);
			}
			case 8: {
				Cursor c = db
						.query(DATABASE_THIRD_PARTIES_TABLE, new String[] { KEY_THIRD_PARTY_ROWID,
								KEY_THIRD_PARTY_NAME }, null, null, null, null, null);
				if (c != null && c.moveToFirst()) {
					do {
						ContentValues v = new ContentValues();
						v.put(KEY_THIRD_PARTY_NORMALIZED_NAME,
								AsciiUtils.convertNonAscii(
										c.getString(c.getColumnIndex(KEY_THIRD_PARTY_NAME)))
										.toLowerCase());
						db.update(
								DATABASE_THIRD_PARTIES_TABLE,
								v,
								KEY_THIRD_PARTY_ROWID
										+ "="
										+ Long.toString(c.getLong(c
												.getColumnIndex(KEY_THIRD_PARTY_ROWID))), null);
					} while (c.moveToNext());
					c.close();
				}
				c = db.query(DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID, KEY_TAG_NAME },
						null, null, null, null, null);
				if (c != null && c.moveToFirst()) {
					do {
						ContentValues v = new ContentValues();
						v.put(KEY_TAG_NORMALIZED_NAME,
								AsciiUtils.convertNonAscii(
										c.getString(c.getColumnIndex(KEY_TAG_NAME))).toLowerCase());
						db.update(
								DATABASE_TAGS_TABLE,
								v,
								KEY_TAG_ROWID + "="
										+ Long.toString(c.getLong(c.getColumnIndex(KEY_TAG_ROWID))),
								null);
					} while (c.moveToNext());
					c.close();
				}

				c = db.query(DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID, KEY_MODE_NAME },
						null, null, null, null, null);
				if (c != null && c.moveToFirst()) {
					do {
						ContentValues v = new ContentValues();
						v.put(KEY_MODE_NORMALIZED_NAME,
								AsciiUtils.convertNonAscii(
										c.getString(c.getColumnIndex(KEY_MODE_NAME))).toLowerCase());
						db.update(
								DATABASE_MODES_TABLE,
								v,
								KEY_MODE_ROWID
										+ "="
										+ Long.toString(c.getLong(c.getColumnIndex(KEY_MODE_ROWID))),
								null);
					} while (c.moveToNext());
					c.close();
				}
			}
			case 9: {
				db.execSQL(ADD_PROJECTION_MODE_COLUNM);
				db.execSQL(ADD_PROJECTION_MODE_DATE);
				Cursor c = db.query(DATABASE_ACCOUNT_TABLE, new String[] {}, null, null, null,
						null, null);
				if (null != c) {
					if (c.moveToFirst()) {
						do {
							consolidateSums(c.getLong(c.getColumnIndex(KEY_ACCOUNT_ROWID)), db);
						} while (c.moveToNext());
					}
					c.close();
				}
				c = db.query(DATABASE_OPERATIONS_TABLE, new String[] { KEY_OP_ROWID, KEY_OP_DATE },
						null, null, null, null, null);
				if (null != c) {
					if (c.moveToFirst()) {
						ContentValues values;
						do {
							values = new ContentValues();
							GregorianCalendar d = new GregorianCalendar();
							d.setTimeInMillis(c.getLong(c.getColumnIndex(KEY_OP_DATE)));
							Tools.clearTimeOfCalendar(d);
							values.put(KEY_OP_DATE, d.getTimeInMillis());
							db.update(DATABASE_OPERATIONS_TABLE, values,
									KEY_OP_ROWID + "=" + c.getLong(c.getColumnIndex(KEY_OP_ROWID)),
									null);
						} while (c.moveToNext());
					}
					c.close();
				}
			}
			case 10: {
				db.execSQL(DATABASE_PREFS_CREATE);
				PrefsManager prefs = PrefsManager.getInstance(mCtx);
				HashMap<String, String> allPrefs = prefs.getRawData();
				if (null != allPrefs) {
					for (Entry<String, String> elt : allPrefs.entrySet()) {
						ContentValues values = new ContentValues();
						values.put(KEY_PREFS_VALUE, elt.getValue());
						values.put(KEY_PREFS_NAME, elt.getKey());
						db.insert(DATABASE_PREFS_TABLE, null, values);
					}
				}
			}
			case 11: {
				Cursor c = db.query(DATABASE_OPERATIONS_TABLE, null, KEY_OP_ROWID
						+ "=(SELECT max(_id) FROM " + DATABASE_OPERATIONS_TABLE + ")", null, null,
						null, null);
				if (c != null) {
					long lastSum;
					long curSum;
					if (c.moveToFirst()) {
						lastSum = c.getLong(c.getColumnIndex(KEY_OP_SUM));
						curSum = lastSum;
						int del = db.delete(DATABASE_OPERATIONS_TABLE, KEY_OP_ROWID
								+ ">600",
								null);
//						Log.d("Radis", "before loop del :" + del);
						do {
							lastSum = curSum;
							int nbDel = db.delete(DATABASE_OPERATIONS_TABLE, KEY_OP_ROWID
									+ "=(SELECT max(_id) FROM " + DATABASE_OPERATIONS_TABLE + ")",
									null);
//							Log.d("Radis", "rowId = " + c.getLong(c.getColumnIndex(KEY_OP_ROWID)));
							c.close();
							c = db.query(DATABASE_OPERATIONS_TABLE, null, KEY_OP_ROWID
									+ "=(SELECT max(_id) FROM " + DATABASE_OPERATIONS_TABLE + ")",
									null, null, null, null);
							if (null != c) {
								if (c.moveToFirst()) {
									curSum = c.getLong(c.getColumnIndex(KEY_OP_SUM));
								} else {
									c.close();
									curSum = 0;
								}
							} else {
								curSum = 0;
							}
//							Log.d("Radis", "lastSum = " + lastSum + ", curSum = " + curSum
//									+ ", nbDel = " + nbDel );
						} while (curSum == lastSum);
					} else {
						c.close();
					}
				}
			}
			default:
				Cursor c = db.query(DATABASE_ACCOUNT_TABLE, new String[] { KEY_ACCOUNT_ROWID },
						null, null, null, null, null);
				if (null != c) {
					if (c.moveToFirst()) {
						do {
							CommonDbAdapter.this.consolidateSums(c.getLong(0), db);
						} while (c.moveToNext());
					}
					c.close();
				}
				break;
			}

		}
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public CommonDbAdapter open() throws SQLException {
		if (null == mDbHelper) {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getWritableDatabase();
			fillCaches();
		}
		return this;
	}

	public void close() {
		try {
			if (null != mDbHelper) {
				mDbHelper.close();
				mDbHelper = null;
				mDb.close();
				mDb = null;
			}
		} catch (Exception e) {
		}
	}

	private void fillCache(String table, String[] cols, Map<String, Long> map) {
		Cursor c = mDb.query(table, cols, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1).toLowerCase();
				Long value = c.getLong(0);
				map.put(key, value);
			} while (c.moveToNext());
		}
		c.close();
	}

	private void fillCaches() {
		mModesMap = new LinkedHashMap<String, Long>();
		mTagsMap = new LinkedHashMap<String, Long>();
		mThirdPartiesMap = new LinkedHashMap<String, Long>();

		fillCache(DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID, KEY_MODE_NORMALIZED_NAME },
				mModesMap);
		fillCache(DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID, KEY_TAG_NORMALIZED_NAME },
				mTagsMap);
		fillCache(DATABASE_THIRD_PARTIES_TABLE, new String[] { KEY_THIRD_PARTY_ROWID,
				KEY_THIRD_PARTY_NORMALIZED_NAME }, mThirdPartiesMap);
	}

	public long createAccount(String name, String desc, long start_sum, String currency,
			int projectionMode, String projectionDate) throws ParseException {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ACCOUNT_NAME, name);
		initialValues.put(KEY_ACCOUNT_DESC, desc);
		initialValues.put(KEY_ACCOUNT_START_SUM, start_sum);

		initialValues.put(KEY_ACCOUNT_CURRENCY, currency);
		initialValues.put(KEY_ACCOUNT_PROJECTION_MODE, projectionMode);
		initialValues.put(KEY_ACCOUNT_PROJECTION_DATE, projectionDate);

		setCurrentSumAndDate(0, initialValues, start_sum, projectionMode, projectionDate);
		return mDb.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
	}

	private long computeSumFromCursor(Cursor c) {
		long sum = 0L;
		if (c.moveToFirst()) {
			do {
				long s = c.getLong(c.getColumnIndex(CommonDbAdapter.KEY_OP_SUM));
				sum = sum + s;
			} while (c.moveToNext());
		}
		return sum;
	}

	// called on create and update account
	private void setCurrentSumAndDate(long accountId, ContentValues values, final long start_sum,
			final int projectionMode, final String projectionDate) throws ParseException {
		Log.d("Radis", "setCurrentSumAndDate start_sum = " + start_sum + " / projMode:"
				+ projectionMode);
		long date = 0;
		long opSum = 0;
		switch (projectionMode) {
		case 0: {
			Log.d("Radis", "setCurrentSumAndDate mAccountId = " + accountId);
			if (accountId > 0) {
				Cursor allOps = fetchAllOps(accountId);
				if (null != allOps) {
					Log.d("Radis", "setCurrentSumAndDate allOps not null : " + allOps.getCount());
					if (allOps.moveToFirst()) {
						Log.d("Radis", "setCurrentSumAndDate allOps moved to first");
						date = allOps.getLong(allOps.getColumnIndex(KEY_OP_DATE));
						opSum = computeSumFromCursor(allOps);
					}
					allOps.close();
				}
			}
		}
			break;
		case 1: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			if (projDate.get(Calendar.DAY_OF_MONTH) >= Integer.parseInt(projectionDate)) {
				projDate.roll(Calendar.MONTH, 1);
			}
			projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = fetchOpEarlierThan(projDate.getTimeInMillis(), 0, accountId);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
														// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = computeSumFromCursor(op);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		case 2: {
			GregorianCalendar projDate = new GregorianCalendar();
			Tools.clearTimeOfCalendar(projDate);
			projDate.setTime(Formater.DATE_FORMAT.parse(projectionDate));
			projDate.roll(Calendar.DAY_OF_MONTH, 1); // roll for query
			Cursor op = fetchOpEarlierThan(projDate.getTimeInMillis(), 0, accountId);
			projDate.roll(Calendar.DAY_OF_MONTH, -1); // restore date after
			// query
			if (null != op) {
				if (op.moveToFirst()) {
					opSum = computeSumFromCursor(op);
				}
				op.close();
			}
			date = projDate.getTimeInMillis();
		}
			break;
		default:
			break;
		}
		values.put(KEY_ACCOUNT_OP_SUM, opSum);
		values.put(KEY_ACCOUNT_CUR_SUM, start_sum + opSum);
		values.put(KEY_ACCOUNT_CUR_SUM_DATE, date);
	}

	public boolean deleteAccount(long rowId) {
		return mDb.delete(DATABASE_ACCOUNT_TABLE, KEY_ACCOUNT_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllAccounts() {
		Cursor c = mDb.query(DATABASE_ACCOUNT_TABLE,
				new String[] { KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM,
						KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE,
						KEY_ACCOUNT_PROJECTION_MODE, KEY_ACCOUNT_PROJECTION_DATE }, null, null,
				null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAccount(long accountId) throws SQLException {
		Cursor c = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] { KEY_ACCOUNT_ROWID,
				KEY_ACCOUNT_NAME, KEY_ACCOUNT_DESC, KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_CUR_SUM,
				KEY_ACCOUNT_OP_SUM, KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE,
				KEY_ACCOUNT_PROJECTION_MODE, KEY_ACCOUNT_PROJECTION_DATE }, KEY_ACCOUNT_ROWID + "="
				+ accountId, null, null, null, null, null);
		initProjectionDate(c);
		return c;

	}

	private void initProjectionDate(Cursor c) {
		if (c != null && c.moveToFirst()) {
			mProjectionMode = c.getInt(8);
			switch (mProjectionMode) {
			case 0:
				mProjectionDate = c.getLong(c.getColumnIndex(KEY_ACCOUNT_CUR_SUM_DATE));
				break;
			case 1: {
				GregorianCalendar projDate = new GregorianCalendar();
				Tools.clearTimeOfCalendar(projDate);
				projDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(c.getString(9)));
				GregorianCalendar today = new GregorianCalendar();
				Tools.clearTimeOfCalendar(today);
				if (projDate.compareTo(today) <= 0) {
					projDate.roll(Calendar.MONTH, 1);
				}
				mProjectionDate = projDate.getTimeInMillis();
			}
				break;
			case 2:
				try {
					Date projDate = Formater.DATE_FORMAT.parse(c.getString(9));
					projDate.setHours(0);
					projDate.setMinutes(0);
					projDate.setSeconds(0);
					mProjectionDate = projDate.getTime();
				} catch (ParseException e) {
					ErrorReporter.getInstance().handleSilentException(e);
					e.printStackTrace();
				}

				break;
			default:
				break;
			}
		}
	}

	public boolean updateAccountProjectionDate(long accountId,
			ProjectionDateController projectionController) throws ParseException {
		if (projectionController.hasChanged()) {
			updateAccountProjectionDate(accountId, projectionController.getMode(),
					projectionController.getDate());
		}
		return true;
	}

	private boolean updateAccountProjectionDate(long accountId, final int projMode,
			final String projDate) throws ParseException {
		Cursor account = fetchAccount(accountId);
		ContentValues args = new ContentValues();
		long start_sum = account.getLong(account.getColumnIndex(KEY_ACCOUNT_START_SUM));
		account.close();
		args.put(KEY_ACCOUNT_PROJECTION_MODE, projMode);
		args.put(KEY_ACCOUNT_PROJECTION_DATE, projDate);

		setCurrentSumAndDate(accountId, args, start_sum, projMode, projDate);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "=" + accountId, null) > 0;
	}

	public boolean updateAccountProjectionDate(long accountId) throws ParseException {
		Cursor c = fetchAccount(accountId);
		boolean res = true;
		if (null != c) {
			if (c.moveToFirst()) {
				res = updateAccountProjectionDate(accountId,
						c.getInt(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)),
						c.getString(c.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
			}
			c.close();
		}
		return res;
	}

	public boolean updateAccount(long accountId, String name, String desc, long start_sum,
			String currency, ProjectionDateController projectionController) throws ParseException {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		args.put(KEY_ACCOUNT_PROJECTION_MODE, projectionController.getMode());
		args.put(KEY_ACCOUNT_PROJECTION_DATE, projectionController.getDate());
		setCurrentSumAndDate(accountId, args, start_sum, projectionController.getMode(),
				projectionController.getDate());
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "=" + accountId, null) > 0;
	}

	public void updateProjection(long accountId, long sumToAdd, long opDate) {
		ContentValues args = new ContentValues();
		if (mProjectionMode == 0 && opDate > mProjectionDate || opDate == 0) {
			if (opDate == 0) {
				Cursor op = fetchLastOp(accountId);
				if (null != op) {
					if (op.moveToFirst()) {
						args.put(KEY_ACCOUNT_CUR_SUM_DATE,
								op.getLong(op.getColumnIndex(KEY_OP_DATE)));
					}
					op.close();
				}
			} else {
				args.put(KEY_ACCOUNT_CUR_SUM_DATE, opDate);
			}
		}
		Cursor accountCursor = fetchAccount(accountId);
		accountCursor.requery();
		if (accountCursor.moveToFirst()) {
			long opSum = accountCursor.getLong(accountCursor
					.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
			long startSum = accountCursor.getLong(accountCursor
					.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_START_SUM));
			args.put(KEY_ACCOUNT_OP_SUM, opSum + sumToAdd);
			args.put(KEY_ACCOUNT_CUR_SUM, startSum + opSum + sumToAdd);
			if (mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "=" + accountId, null) > 0) {
				if (mProjectionMode == 0) {
					mProjectionDate = opDate;
				}
			}
		}
		accountCursor.close();
	}

	public long getKeyIdOrCreate(String key, String table) throws SQLException {
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			return getKeyIdOrCreate(key, mThirdPartiesMap, table, KEY_THIRD_PARTY_NAME);
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			return getKeyIdOrCreate(key, mTagsMap, table, KEY_TAG_NAME);
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			return getKeyIdOrCreate(key, mModesMap, table, KEY_MODE_NAME);
		}
		return 0;
	}

	public long getKeyIdIfExists(String key, String table) {
		Long res = null;
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			res = mThirdPartiesMap.get(key.toLowerCase());
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			res = mTagsMap.get(key.toLowerCase());
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			res = mModesMap.get(key.toLowerCase());
		}
		if (null != res) {
			return res.longValue();
		}
		return -1;
	}

	private long getKeyIdOrCreate(String key, LinkedHashMap<String, Long> map, String table,
			String col) throws SQLException {
		String origKey = key.toString();
		key = AsciiUtils.convertNonAscii(key).trim().toLowerCase();
		if (key.length() == 0) {
			return -1;
		}
		Long i = map.get(key);
		if (null != i) {
			return i.longValue();
		} else {
			ContentValues initialValues = new ContentValues();
			initialValues.put(col, origKey);
			initialValues.put(mColNameNormName.get(col), key);
			long id = mDb.insert(table, null, initialValues);
			if (id != -1) {
				map.put(key, id);
			} else {
				throw new SQLException("Database insertion error : " + key + " in " + table);
			}
			return id;
		}
	}

	private void putKeyId(String key, String keyTableName, String keyTableCol, String opTableCol,
			LinkedHashMap<String, Long> keyMap, ContentValues initialValues) {
		long id = getKeyIdOrCreate(key, keyMap, keyTableName, keyTableCol);
		if (id != -1) {
			initialValues.put(opTableCol, id);
		} else {
			initialValues.putNull(opTableCol);
		}
	}

	// return boolean saying if we need an update of OP_SUM
	public boolean createOp(Operation op, final long accountId) {
		ContentValues initialValues = new ContentValues();
		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME, KEY_OP_THIRD_PARTY,
				mThirdPartiesMap, initialValues);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap, initialValues);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE, mModesMap, initialValues);

		initialValues.put(KEY_OP_SUM, op.mSum);
		initialValues.put(KEY_OP_DATE, op.getDate());
		initialValues.put(KEY_OP_ACCOUNT_ID, accountId);
		initialValues.put(KEY_OP_NOTES, op.mNotes);
		initialValues.put(KEY_OP_SCHEDULED_ID, op.mScheduledId);
		op.mRowId = mDb.insert(DATABASE_OPERATIONS_TABLE, null, initialValues);
		if (op.mRowId > -1) {
			return checkNeedUpdateProjection(op, accountId);
		}
		Log.e("Radis", "error in creating op");
		return false;
	}

	private boolean checkNeedUpdateProjection(Operation op, final long accountId) {
		Cursor c = fetchAccount(accountId);
		initProjectionDate(c);
		c.close();
		final long opDate = op.getDate();
		final long projDate = mProjectionDate;
		boolean res = (opDate <= projDate) || ((mProjectionMode == 0) && (opDate >= projDate))
				|| (projDate == 0);
		// Log.d("Radis", "checkNeedUpdateProjection : " + res);
		return res;
	}

	public boolean deleteOp(long rowId, final long accountId) {
		Cursor c = fetchOneOp(rowId, accountId);
		Operation op = new Operation(c);
		c.close();
		if (mDb.delete(DATABASE_OPERATIONS_TABLE, KEY_OP_ROWID + "=" + rowId, null) > 0) {
			return checkNeedUpdateProjection(op, accountId);
		}
		return false;
	}

	public Cursor fetchNLastOps(int nbOps, final long accountId) {
		return mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId), null, null, null, OP_ORDERING,
				Integer.toString(nbOps));
	}

	public Cursor fetchLastOp(final long accountId) {
		Cursor c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops." + KEY_OP_DATE
						+ " = (SELECT max(ops." + KEY_OP_DATE + ") FROM "
						+ DATABASE_OPERATIONS_TABLE + ") ", null, null, null, OP_ORDERING, null);
		return c;
	}

	public Cursor fetchAllOps(final long accountId) {
		Cursor c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId), null, null, null, OP_ORDERING, null);
		if (null != c) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOneOp(final long rowId, final long accountId) {
		Cursor c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops." + KEY_OP_ROWID + " = "
						+ rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpEarlierThan(long date, int nbOps, final long accountId) {
		Cursor c = null;
		String limit = nbOps == 0 ? null : Integer.toString(nbOps);
		c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops." + KEY_OP_DATE + " < "
						+ date, null, null, null, OP_ORDERING, limit);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpOfMonth(final GregorianCalendar date, final long accountId) {
		Cursor c = null;
		GregorianCalendar startDate = new GregorianCalendar();
		GregorianCalendar endDate = new GregorianCalendar();
		Tools.clearTimeOfCalendar(startDate);
		Tools.clearTimeOfCalendar(endDate);

		startDate.set(Calendar.DAY_OF_MONTH, 1);
		endDate.set(Calendar.DAY_OF_MONTH, 1);

		startDate.set(Calendar.MONTH, date.get(Calendar.MONTH));
		endDate.set(Calendar.MONTH, date.get(Calendar.MONTH));
		
		startDate.set(Calendar.YEAR, date.get(Calendar.YEAR));
		endDate.set(Calendar.YEAR, date.get(Calendar.YEAR));

		startDate.set(Calendar.DAY_OF_MONTH, startDate.getActualMinimum(Calendar.DAY_OF_MONTH));
		endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
		c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops." + KEY_OP_DATE + " <= "
						+ endDate.getTimeInMillis() + " AND ops." + KEY_OP_DATE + " >= "
						+ startDate.getTimeInMillis(), null, null, null, OP_ORDERING, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpBetweenDate(final GregorianCalendar today, final GregorianCalendar latest,
			final long accountId) {
		Cursor c = null;
		GregorianCalendar startDate;
		GregorianCalendar endDate;

		if (today.before(latest)) {
			startDate = today;
			endDate = latest;
		} else {
			startDate = latest;
			endDate = today;
		}
		
		c = mDb.query(DATABASE_OP_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, accountId) + " AND ops." + KEY_OP_DATE + " <= "
						+ endDate.getTimeInMillis() + " AND ops." + KEY_OP_DATE + " >= "
						+ startDate.getTimeInMillis(), null, null, null, OP_ORDERING, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	private ContentValues createContentValuesFromOp(final Operation op,
			final boolean updateOccurrences) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME, KEY_OP_THIRD_PARTY,
				mThirdPartiesMap, args);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap, args);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE, mModesMap, args);

		args.put(KEY_OP_SUM, op.mSum);
		args.put(KEY_OP_NOTES, op.mNotes);
		if (!updateOccurrences) {
			args.put(KEY_OP_DATE, op.getDate());
			args.put(KEY_OP_SCHEDULED_ID, op.mScheduledId);
		}
		return args;
	}

	// return if need to update OP_SUM
	public boolean updateOp(final long rowId, final Operation op, final long accountId) {
		ContentValues args = createContentValuesFromOp(op, false);
		if (mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ROWID + "=" + rowId, null) > 0) {
			return checkNeedUpdateProjection(op, accountId);
		}
		return false;
	}

	public void updateOp(final long opId, final long schOpId) {
		ContentValues args = new ContentValues();
		args.put(KEY_OP_SCHEDULED_ID, schOpId);
		mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ROWID + "=" + opId, null);
	}

	// ----------------------
	// SCHEDULED TRANSACTIONS
	// ----------------------
	public Cursor fetchAllScheduledOps() {
		Cursor c = mDb.query(DATABASE_SCHEDULED_TABLE_JOINTURE, SCHEDULED_OP_COLS_QUERY, null,
				null, null, null, SCHEDULED_OP_ORDERING);
		if (null != c) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOneScheduledOp(long rowId) {
		Cursor c = mDb.query(DATABASE_SCHEDULED_TABLE_JOINTURE, SCHEDULED_OP_COLS_QUERY, "sch."
				+ KEY_OP_ROWID + " = " + rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public long createScheduledOp(ScheduledOperation op) {
		ContentValues initialValues = new ContentValues();
		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME, KEY_OP_THIRD_PARTY,
				mThirdPartiesMap, initialValues);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap, initialValues);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE, mModesMap, initialValues);

		initialValues.put(KEY_OP_SUM, op.mSum);
		initialValues.put(KEY_OP_DATE, op.getDate());
		initialValues.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
		initialValues.put(KEY_OP_NOTES, op.mNotes);
		initialValues.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
		initialValues.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
		initialValues.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
		return mDb.insert(DATABASE_SCHEDULED_TABLE, null, initialValues);
	}

	public boolean updateScheduledOp(long rowId, ScheduledOperation op,
			final boolean isUpdatedFromOccurence) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME, KEY_OP_THIRD_PARTY,
				mThirdPartiesMap, args);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap, args);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE, mModesMap, args);

		args.put(KEY_OP_SUM, op.mSum);
		args.put(KEY_OP_NOTES, op.mNotes);
		if (!isUpdatedFromOccurence) { // update from schedule editor
			args.put(KEY_SCHEDULED_END_DATE, op.getEndDate());
			args.put(KEY_SCHEDULED_PERIODICITY, op.mPeriodicity);
			args.put(KEY_SCHEDULED_PERIODICITY_UNIT, op.mPeriodicityUnit);
			args.put(KEY_SCHEDULED_ACCOUNT_ID, op.mAccountId);
			args.put(KEY_OP_DATE, op.getDate());
		}
		return mDb.update(DATABASE_SCHEDULED_TABLE, args, KEY_OP_ROWID + "=" + rowId, null) > 0;
	}

	public boolean deleteScheduledOp(final long schOpId) {
		return mDb.delete(DATABASE_SCHEDULED_TABLE, KEY_SCHEDULED_ROWID + "=" + schOpId, null) > 0;
	}

	public int deleteAllOccurrences(final long accountId, final long schOpId) {
		return mDb.delete(DATABASE_OPERATIONS_TABLE, KEY_OP_ACCOUNT_ID + "=" + accountId + " AND "
				+ KEY_OP_SCHEDULED_ID + "=" + schOpId, null);
	}

	public int updateAllOccurrences(final long accountId, final long schOpId, final Operation op) {
		ContentValues args = createContentValuesFromOp(op, true);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ACCOUNT_ID + "=" + accountId
				+ " AND " + KEY_OP_SCHEDULED_ID + "=" + schOpId, null);
	}

	public int disconnectAllOccurrences(final long accountId, final long schOpId) {
		ContentValues args = new ContentValues();
		args.put(KEY_OP_SCHEDULED_ID, 0);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ACCOUNT_ID + "=" + accountId
				+ " AND " + KEY_OP_SCHEDULED_ID + "=" + schOpId, null);
	}

	// ------------------------------
	// INFOS (third party, tag, mode)
	// ------------------------------
	public boolean updateInfo(String table, long rowId, String value, String oldValue) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		args.put(mColNameNormName.get(mInfoColMap.get(table)), AsciiUtils.convertNonAscii(value)
				.trim().toLowerCase());
		int res = mDb.update(table, args, "_id =" + rowId, null);

		// update cache
		Map<String, Long> m = null;
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			m = mThirdPartiesMap;
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			m = mTagsMap;
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			m = mModesMap;
		}
		m.remove(oldValue);
		m.put(value.toLowerCase(), rowId);
		return res > 0;
	}

	public long createInfo(String table, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		args.put(mColNameNormName.get(mInfoColMap.get(table)), AsciiUtils.convertNonAscii(value)
				.trim().toLowerCase());
		long res = mDb.insert(table, null, args);
		if (res > 0) { // update cache
			Map<String, Long> m = null;
			if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
				m = mThirdPartiesMap;
			} else if (table.equals(DATABASE_TAGS_TABLE)) {
				m = mTagsMap;
			} else if (table.equals(DATABASE_MODES_TABLE)) {
				m = mModesMap;
			}
			m.put(AsciiUtils.convertNonAscii(value).trim().toLowerCase(), res);
		}
		return res;
	}

	public boolean deleteInfo(String table, long rowId) {
		boolean res = mDb.delete(table, "_id =" + rowId, null) > 0;
		mInfoCursorMap.get(table).requery();
		return res;
	}

	public Cursor fetchMatchingInfo(String table, String colName, String constraint) {
		String where;
		String[] params;
		if (null != constraint) {
			where = mColNameNormName.get(colName) + " LIKE ?";
			params = new String[] { constraint.trim() + "%" };
		} else {
			where = null;
			params = null;
		}
		Cursor c = mDb.query(table, new String[] { "_id", colName }, where, params, null, null,
				colName + " asc");
		if (null != c) {
			c.moveToFirst();
		}
		mInfoCursorMap.put(table, c);
		return c;
	}

	// ------------
	// Preferences
	// ------------
	public void setPref(final String key, final String value) {
		ContentValues values = new ContentValues();
		values.put(KEY_PREFS_VALUE, value);
		if (null == getPref(key)) {
			values.put(KEY_PREFS_NAME, key);
			// insert
			mDb.insert(DATABASE_PREFS_TABLE, null, values);
		} else {
			// update
			mDb.update(DATABASE_PREFS_TABLE, values, KEY_PREFS_NAME + "='" + key + "'", null);
		}
	}

	public String getPref(final String key) {
		String res = null;
		Cursor c = mDb.query(DATABASE_PREFS_TABLE, new String[] { KEY_PREFS_VALUE }, KEY_PREFS_NAME
				+ "='" + key + "'", null, null, null, null);
		if (null != c) {
			if (c.moveToFirst()) {
				res = c.getString(0);
			}
			c.close();
		}
		return res;
	}

	public HashMap<String, String> getAllPrefs() {
		HashMap<String, String> res = new HashMap<String, String>();
		Cursor c = mDb.query(DATABASE_PREFS_TABLE,
				new String[] { KEY_PREFS_NAME, KEY_PREFS_VALUE }, null, null, null, null, null);
		if (null != c) {
			if (c.moveToFirst()) {
				do {
					res.put(c.getString(0), c.getString(1));
				} while (c.moveToNext());
			}
			c.close();
		}
		return res;
	}

	public void deletePref(final String key) {
		mDb.delete(DATABASE_PREFS_TABLE, KEY_PREFS_NAME + "='" + key + "'", null);
	}

	// -------------
	// Global tools
	// -------------
	public void trashDatabase() {
		close();
		mCtx.deleteDatabase(DATABASE_NAME);
	}

	public boolean backupDatabase() {
		close();
		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();
			if (sd.canWrite()) {
				String currentDBPath = "data/fr.geobert.radis/databases/radisDb";
				String backupDBDir = "/radis/";
				String backupDBPath = "/radis/radisDb";
				File currentDB = new File(data, currentDBPath);
				File backupDir = new File(sd, backupDBDir);
				backupDir.mkdirs();
				File backupDB = new File(sd, backupDBPath);

				if (currentDB.exists()) {
					FileChannel src = new FileInputStream(currentDB).getChannel();
					FileChannel dst = new FileOutputStream(backupDB).getChannel();

					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean restoreDatabase() {
		close();
		try {
			File sd = Environment.getExternalStorageDirectory();

			String backupDBPath = "/radis/radisDb";
			File currentDB = mCtx.getDatabasePath(DATABASE_NAME);
			File backupDB = new File(sd, backupDBPath);

			if (backupDB.exists()) {
				FileChannel dst = new FileOutputStream(currentDB).getChannel();
				FileChannel src = new FileInputStream(backupDB).getChannel();

				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void consolidateSums(final long accountId) {
		consolidateSums(accountId, null);
	}

	public void consolidateSums(final long accountId, SQLiteDatabase db) {
		if (0 != accountId) {
			if (null != db) {
				mDb = db;
			}
			ContentValues values = new ContentValues();
			Cursor account = fetchAccount(accountId);
			if (account != null) {
				if (account.moveToFirst()) {
					try {
						setCurrentSumAndDate(accountId, values, account.getLong(account
								.getColumnIndex(KEY_ACCOUNT_START_SUM)), account.getInt(account
								.getColumnIndex(KEY_ACCOUNT_PROJECTION_MODE)), account
								.getString(account.getColumnIndex(KEY_ACCOUNT_PROJECTION_DATE)));
						mDb.update(DATABASE_ACCOUNT_TABLE, values, KEY_ACCOUNT_ROWID + "="
								+ accountId, null);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				account.close();
			}
		}
	}

}
