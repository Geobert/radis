package fr.geobert.radis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

public class CommonDbAdapter {
	private static final String TAG = "CommonDbAdapter";
	protected static final String DATABASE_NAME = "radisDb";
	protected static final int DATABASE_VERSION = 6;

	protected static final String DATABASE_ACCOUNT_TABLE = "accounts";
	protected static final String DATABASE_MODES_TABLE = "modes";
	protected static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
	protected static final String DATABASE_TAGS_TABLE = "tags";
	protected static final String DATABASE_OPERATIONS_TABLE = "operations";
	protected static final String DATABASE_SCHEDULED_TABLE = "scheduled_ops";

	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ACCOUNT_DESC = "account_desc";
	public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
	public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum";
	public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
	public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
	public static final String KEY_ACCOUNT_ROWID = "_id";
	public static final String KEY_ACCOUNT_CUR_SUM_DATE = "account_current_sum_date";

	private static final String DATABASE_ACCOUNT_CREATE = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
			+ KEY_ACCOUNT_START_SUM + " real not null, " + KEY_ACCOUNT_OP_SUM
			+ " real not null, " + KEY_ACCOUNT_CUR_SUM + " real not null, "
			+ KEY_ACCOUNT_CUR_SUM_DATE + " integer not null, "
			+ KEY_ACCOUNT_CURRENCY + " text not null);";

	public static final String KEY_THIRD_PARTY_ROWID = "_id";
	public static final String KEY_THIRD_PARTY_NAME = "third_party_name";

	private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME
			+ " text not null);";

	public static final String KEY_TAG_ROWID = "_id";
	public static final String KEY_TAG_NAME = "tag_name";

	private static final String DATABASE_TAGS_CREATE = "create table "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ " integer primary key autoincrement, " + KEY_TAG_NAME
			+ " text not null);";

	public static final String KEY_MODE_ROWID = "_id";
	public static final String KEY_MODE_NAME = "mode_name";

	private static final String DATABASE_MODES_CREATE = "create table "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID
			+ " integer primary key autoincrement, " + KEY_MODE_NAME
			+ " text not null);";

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
			+ " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY
			+ " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM
			+ " real not null, " + KEY_SCHEDULED_ACCOUNT_ID
			+ " integer not null, " + KEY_OP_MODE + " integer, " + KEY_OP_DATE
			+ " integer not null, " + KEY_SCHEDULED_END_DATE + " integer, "
			+ KEY_SCHEDULED_PERIODICITY + " integer, "
			+ KEY_SCHEDULED_PERIODICITY_UNIT + " integer not null, "
			+ KEY_OP_NOTES + " text, FOREIGN KEY (" + KEY_OP_THIRD_PARTY
			+ ") REFERENCES " + DATABASE_THIRD_PARTIES_TABLE + "("
			+ KEY_THIRD_PARTY_ROWID + "), FOREIGN KEY (" + KEY_OP_TAG
			+ ") REFERENCES " + DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_MODE + ") REFERENCES "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID + "));";

	protected static final String DATABASE_OP_CREATE = "create table "
			+ DATABASE_OPERATIONS_TABLE + "(" + KEY_OP_ROWID
			+ " integer primary key autoincrement, " + KEY_OP_THIRD_PARTY
			+ " integer, " + KEY_OP_TAG + " integer, " + KEY_OP_SUM
			+ " real not null, " + KEY_OP_ACCOUNT_ID + " integer not null, "
			+ KEY_OP_MODE + " integer, " + KEY_OP_DATE + " integer not null, "
			+ KEY_OP_NOTES + " text, " + KEY_OP_SCHEDULED_ID
			+ " integer, FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_TAG + ") REFERENCES "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID + "), FOREIGN KEY ("
			+ KEY_OP_MODE + ") REFERENCES " + DATABASE_MODES_TABLE + "("
			+ KEY_MODE_ROWID + "), FOREIGN KEY (" + KEY_OP_SCHEDULED_ID
			+ ") REFERENCES " + DATABASE_SCHEDULED_TABLE + "("
			+ KEY_SCHEDULED_ROWID + "));";

	protected static final String DATABASE_OP_DROP = "drop table if exists "
			+ DATABASE_OPERATIONS_TABLE + ";";

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
			+ KEY_OP_THIRD_PARTY + " = old." + KEY_THIRD_PARTY_ROWID + "; END";
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
			+ KEY_OP_MODE
			+ " = old."
			+ KEY_MODE_ROWID
			+ "; END";
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
			+ " = old."
			+ KEY_TAG_ROWID
			+ "; END";
	protected static final String ADD_NOTES_COLUNM = "ALTER TABLE "
			+ DATABASE_OPERATIONS_TABLE + " ADD COLUMN " + KEY_OP_NOTES
			+ " text";
	protected static final String ADD_CUR_DATE_COLUNM = "ALTER TABLE "
			+ DATABASE_ACCOUNT_TABLE + " ADD COLUMN "
			+ KEY_ACCOUNT_CUR_SUM_DATE + " integer not null DEFAULT 0";
	protected DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;
	protected final Context mCtx;
	protected Cursor mCurAccount;

	public CommonDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	protected static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", converting to new database");
			switch (oldVersion) {
			case 1:
				db.execSQL(DATABASE_OP_CREATE);
				Cursor allAccounts = db.query(DATABASE_ACCOUNT_TABLE,
						new String[] {}, null, null, null, null, null);
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

			default:
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
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long createAccount(String name, String desc, double start_sum,
			String currency) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ACCOUNT_NAME, name);
		initialValues.put(KEY_ACCOUNT_DESC, desc);
		initialValues.put(KEY_ACCOUNT_START_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_OP_SUM, 0);
		initialValues.put(KEY_ACCOUNT_CUR_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_CURRENCY, currency);
		initialValues.put(KEY_ACCOUNT_CUR_SUM_DATE, 0);
		return mDb.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
	}

	public boolean deleteAccount(long rowId) {
		mDb.execSQL(String.format(DATABASE_OP_DROP, rowId));
		return mDb.delete(DATABASE_ACCOUNT_TABLE, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public Cursor fetchAllAccounts() {
		Cursor c = mDb.query(DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM,
				KEY_ACCOUNT_CURRENCY, KEY_ACCOUNT_CUR_SUM_DATE }, null, null,
				null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAccount(long rowId) throws SQLException {
		mCurAccount = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_DESC,
				KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_OP_SUM,
				KEY_ACCOUNT_CURRENCY }, KEY_ACCOUNT_ROWID + "=" + rowId, null,
				null, null, null, null);
		Cursor c = mCurAccount;
		if (c != null) {
			c.moveToFirst();
		}
		return c;

	}

	public boolean updateAccount(long rowId, String name, String desc,
			double start_sum, String currency) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public boolean updateOpSum(long rowId, double newSum) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_OP_SUM, newSum);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public double updateCurrentSum(long rowId, Cursor op) {
		Cursor account = getCurAccountIfDiff(rowId);
		double start = account.getDouble(account
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_START_SUM));
		double opSum = account.getDouble(account
				.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
		ContentValues args = new ContentValues();
		double curSum = start + opSum;
		args.put(KEY_ACCOUNT_CUR_SUM, curSum);
		if (null != op) {
			args.put(KEY_ACCOUNT_CUR_SUM_DATE, op.getLong(op
					.getColumnIndex(OperationsDbAdapter.KEY_OP_DATE)));
		}
		mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null);
		return curSum;
	}

	private Cursor getCurAccountIfDiff(long rowId) {
		Cursor account = mCurAccount;
		if (null == account
				|| !account.isFirst()
				|| account
						.getLong(account
								.getColumnIndexOrThrow(CommonDbAdapter.KEY_ACCOUNT_ROWID)) != rowId) {
			account = fetchAccount(rowId);
		} else {
			account.requery();
			account.moveToFirst();
		}
		return account;
	}

	public void trashDatabase() {
		close();
		mDb.close();
		mCtx.deleteDatabase(DATABASE_NAME);
		Tools.restartApp();
	}

	public boolean backupDatabase() {
		close();
		mDb.close();
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
					FileChannel src = new FileInputStream(currentDB)
							.getChannel();
					FileChannel dst = new FileOutputStream(backupDB)
							.getChannel();

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
		mDb.close();
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
}
