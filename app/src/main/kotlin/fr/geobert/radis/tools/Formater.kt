package fr.geobert.radis.tools

import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.properties.Delegates
import java.util.Date
import android.content.Context
import fr.geobert.radis.R
import java.text.ParseException

private val DATE_FORMAT: DateFormat by Delegates.lazy { DateFormat.getDateInstance(DateFormat.SHORT) }

private val SUM_FORMAT: DecimalFormat by Delegates.lazy {
    val d = DecimalFormat()
    d.setMaximumFractionDigits(2)
    d.setMinimumFractionDigits(2)
    d
}

private var SHORT_DATE_FORMAT: SimpleDateFormat by Delegates.notNull()

public fun getSumSeparator(): Char = SUM_FORMAT.getDecimalFormatSymbols().getDecimalSeparator()
public fun Double.formatSum(): String = SUM_FORMAT.format(this)
[throws(javaClass<ParseException>())] public fun String.parseSum(): Double = SUM_FORMAT.parse(this).toDouble()
public fun Date.formatDate(): String = DATE_FORMAT.format(this)
public fun Long.formatDate(): String = DATE_FORMAT.format(this)
[throws(javaClass<ParseException>())] public fun String.parseDate(): Date = DATE_FORMAT.parse(this)
public fun Date.formatShortDate(): String = SHORT_DATE_FORMAT.format(this)
[throws(javaClass<ParseException>())] public fun String.parseShortDate(): Date = SHORT_DATE_FORMAT.parse(this)
public fun initShortDate(c: Context) {
    SHORT_DATE_FORMAT = SimpleDateFormat(c.getResources().getString(R.string.short_date_format))
}