package fr.geobert.radis.db

import android.database.sqlite.SQLiteDatabase
import fr.geobert.radis.data.Statistic
import android.content.{ContentValues, Context}
import java.lang.Long
import fr.geobert.radis.tools.STools
import android.net.Uri

object StatisticTable {
  private val TAG = "StatisticTable"
  val KEY_STAT_ID = "_id"
  val KEY_STAT_NAME = "stat_name"
  val KEY_STAT_ACCOUNT = "account_id"
  val KEY_STAT_FILTER = "filter"
  val KEY_STAT_PERIOD_TYPE = "period_type"
  val KEY_STAT_TYPE = "chart_type"
  val KEY_STAT_X_LAST = "x_last"
  val KEY_STAT_START_DATE = "start_date"
  val KEY_STAT_END_DATE = "end_date"

  val STAT_COLS = Array(KEY_STAT_ID, KEY_STAT_NAME, KEY_STAT_ACCOUNT, KEY_STAT_FILTER, KEY_STAT_PERIOD_TYPE, KEY_STAT_TYPE,
    KEY_STAT_X_LAST, KEY_STAT_START_DATE, KEY_STAT_END_DATE)

  val STAT_TABLE = "statistics"

  private val CREATE_TABLE = s"create table $STAT_TABLE ($KEY_STAT_ID integer primary key autoincrement, " +
    s"$KEY_STAT_NAME string not null, " +
    s"$KEY_STAT_ACCOUNT integer not null, $KEY_STAT_FILTER integer not null, $KEY_STAT_PERIOD_TYPE integer not null, " +
    s"$KEY_STAT_TYPE integer not null, $KEY_STAT_X_LAST integer, $KEY_STAT_START_DATE integer," +
    s"$KEY_STAT_END_DATE integer)"

  def onCreate(db: SQLiteDatabase) = {
    db.execSQL(CREATE_TABLE)
  }

  def upgradeFromV17(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = {
    onCreate(db)
  }

  private def fillContentValues(stat: Statistic) = {
    val args = new ContentValues()
    STAT_COLS.map {
      case KEY_STAT_ID => // ignore this key
      case key => STools.fillContentValues(args, key, stat.getValue(key))
    }
    args
  }

  def createStatistic(ctx: Context, stat: Statistic) = {
    val res = ctx.getContentResolver.insert(DbContentProvider.STATS_URI, fillContentValues(stat))
    Long.parseLong(res.getLastPathSegment)
  }

  def updateStatistic(ctx: Context, stat: Statistic) = {
    ctx.getContentResolver.update(Uri.parse(DbContentProvider.STATS_URI + "/" + stat.id), fillContentValues(stat), null, null)
  }

  def deleteStatistic(ctx: Context, statId: Long) {
    ctx.getContentResolver.delete(Uri.parse(DbContentProvider.STATS_URI + "/" + statId), null, null)
  }
}
