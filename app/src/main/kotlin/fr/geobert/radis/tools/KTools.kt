package fr.geobert.radis.tools

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.os.Bundle
import android.os.Parcel
import android.support.v7.app.AppCompatActivity
import android.widget.DatePicker
import fr.geobert.radis.R
import fr.geobert.radis.ui.DatePickerDialogFragment
import hirondelle.date4j.DateTime
import java.text.ParseException
import java.util.*

//public val UNIX_DATE: DateTime = DateTime(1970, 1, 1, 0, 0, 0, 0)
public val TIME_ZONE: TimeZone = TimeZone.getDefault()

public fun DateTime.plusMonth(nb: Int): DateTime = this.plus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.minusMonth(nb: Int): DateTime = this.minus(0, nb, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.plusYear(nb: Int): DateTime = this.plus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
public fun DateTime.minusYear(nb: Int): DateTime = this.minus(nb, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)

public fun fillContentValuesWith(args: ContentValues, key: String, value: Any) {
    when (value) {
        is String -> args.put(key, value)
        is Long -> args.put(key, value)
        is Int -> args.put(key, value)
    }
}

fun createBuilder(ctx: Context, titleId: Int, message: String): AlertDialog.Builder {
    val builder = AlertDialog.Builder(ctx)
    builder.setTitle(titleId).setMessage(message)
    return builder
}

fun createBuilder(ctx: Context, titleId: Int, messageId: Int): AlertDialog.Builder {
    val builder = AlertDialog.Builder(ctx)
    builder.setTitle(titleId).setMessage(messageId)
    return builder
}

public fun alert(ctx: Context, titleId: Int, message: String): Unit {
    val builder = createBuilder(ctx, titleId, message)
    builder.create().show()
}

public fun alert(ctx: Context, titleId: Int, messageId: Int): Unit {
    val builder = createBuilder(ctx, titleId, messageId)
    builder.create().show()
}

public fun alert(ctx: Context, titleId: Int, messageId: Int, btnTxtId: Int?,
                 f: ((DialogInterface, Int) -> Unit)? = null) {
    val builder = createBuilder(ctx, titleId, messageId)
    builder.setNeutralButton(btnTxtId ?: android.R.string.ok) { d, i -> if (f != null) f(d, i) }
}

public inline fun Cursor?.forEach(f: (it: Cursor) -> Unit): Unit {
    if (this != null) {
        if (moveToFirst()) {
            do {
                f(this)
            } while (moveToNext())
        }
    }
}

public inline fun <T> Cursor?.map(transform: (it: Cursor) -> T): MutableList<T> {
    return mapTo(LinkedList<T>(), transform)
}

public inline fun <T, C : MutableCollection<T>> Cursor?.mapTo(result: C, transform: (it: Cursor) -> T): C {
    return if (this == null) result else {
        if (moveToFirst())
            do {
                result.add(transform(this))
            } while (moveToNext())
        result
    }
}

public inline fun <T> Cursor?.mapAndClose(create: (it: Cursor) -> T): Collection<T> {
    try {
        return map(create)
    } finally {
        this?.close()
    }
}

public fun <T> Cursor?.getByFilter(filter: (it: Cursor) -> Boolean, create: (it: Cursor) -> T): T? {
    var result: T? = null
    this.forEach {
        if (filter(it)) {
            result = create(it)
            return@forEach
        }
    }
    return result
}

public fun String?.extractSumFromStr(): Long {
    val s = this?.replace('+', ' ')?.trim() as String
    val d: Double
    try {
        d = s.parseSum()
    } catch (e: ParseException) {
        d = 0.0
    }
    return Math.round(d * 100)
}

public fun Parcel.writeBoolean(b: Boolean) {
    this.writeByte(if (b) 1 else 0)
}

public fun Parcel.readBoolean(): Boolean = this.readByte() != 0.toByte()
public fun <T> List<T>.forMutableEach(): Unit {

}

public fun filenameTimetag(): String {
    val today = DateTime.now(TIME_ZONE)
    return today.format("YYYYMMDD|_|ssmmhh")
}

val PLAIN_ASCII = "AaEeIiOoUu" /* grave */ + "AaEeIiOoUuYy"  /* acute */ + "AaEeIiOoUuYy" /* circumflex */ + "AaOoNn" /* tilde */ + "AaEeIiOoUuYy" /* umlaut */ + "Aa" /* ring */ + "Cc" /* cedilla */ + "OoUu" /* double acute */
val UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9" + "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD" + "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" + "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1" + "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5" + "\u00C7\u00E7" + "\u0150\u0151\u0170\u0171"
// remove accentued from a string and replace with ascii equivalent
public fun convertNonAscii(s: String): String {
    val sb = StringBuilder()
    val n = s.length
    for (i in 0..n - 1) {
        val c = s[i]
        val pos = UNICODE.indexOf(c)
        if (pos > -1) {
            sb.append(PLAIN_ASCII[pos])
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

public fun showDatePickerFragment(ctx: AppCompatActivity, listener: (DatePicker?, DateTime) -> Unit,
                                  date: DateTime, minDate: DateTime? = null) {
    val b = Bundle()
    b.putSerializable(DatePickerDialogFragment.DATE, date)
    b.putSerializable(DatePickerDialogFragment.MIN_DATE, minDate)
    b.putInt(DatePickerDialogFragment.TITLE, R.string.op_date)
    val dialog = DatePickerDialogFragment()
    dialog.arguments = b
    dialog.setOnDateSetListener(listener)
    dialog.show(ctx.supportFragmentManager, "quick_add_op_date")
}
