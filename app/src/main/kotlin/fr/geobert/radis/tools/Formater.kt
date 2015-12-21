package fr.geobert.radis.tools

import android.content.Context
import fr.geobert.radis.R
import hirondelle.date4j.DateTime
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

val DATE_FORMAT: DateFormat by lazy { DateFormat.getDateInstance(DateFormat.SHORT) }

val SUM_FORMAT: DecimalFormat by lazy {
    val d = DecimalFormat()
    d.maximumFractionDigits = 2
    d.minimumFractionDigits = 2
    d
}

var SHORT_DATE_FORMAT: SimpleDateFormat by Delegates.notNull()

public fun getSumSeparator(): Char = SUM_FORMAT.decimalFormatSymbols.decimalSeparator
public fun getGroupSeparator(): Char = SUM_FORMAT.decimalFormatSymbols.groupingSeparator
public fun Double.formatSum(): String = SUM_FORMAT.format(this)
public fun String.parseSum(): Double = SUM_FORMAT.parse(this).toDouble()
public fun Date.formatDate(): String = DATE_FORMAT.format(this)

public fun DateTime.formatDate(): String = this.format("DD/MM")
public fun DateTime.formatDateLong(): String = this.format("DD/MM/YYYY")

public fun Long.formatDate(): String = DATE_FORMAT.format(this)
public fun String.parseDate(): Date = DATE_FORMAT.parse(this)
public fun Date.formatShortDate(): String = SHORT_DATE_FORMAT.format(this)
public fun String.parseShortDate(): Date = SHORT_DATE_FORMAT.parse(this)
public fun initShortDate(c: Context) {
    SHORT_DATE_FORMAT = SimpleDateFormat(c.resources.getString(R.string.short_date_format))
}
