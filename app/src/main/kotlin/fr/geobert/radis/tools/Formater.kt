package fr.geobert.radis.tools

import android.content.Context
import fr.geobert.radis.R
import hirondelle.date4j.DateTime
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates

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
public fun String.parseSum(): Double = SUM_FORMAT.parse(this).toDouble()
public fun Date.formatDate(): String = DATE_FORMAT.format(this)

public fun DateTime.formatDate(): String = this.format("DD/MM")
public fun DateTime.formatDateLong(): String = this.format("DD/MM/YY")

public fun Long.formatDate(): String = DATE_FORMAT.format(this)
public fun String.parseDate(): Date = DATE_FORMAT.parse(this)
public fun Date.formatShortDate(): String = SHORT_DATE_FORMAT.format(this)
public fun String.parseShortDate(): Date = SHORT_DATE_FORMAT.parse(this)
public fun initShortDate(c: Context) {
    SHORT_DATE_FORMAT = SimpleDateFormat(c.getResources().getString(R.string.short_date_format))
}
