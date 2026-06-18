package uk.hristijan.pitstop.core.format

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

fun formatMoney(minorUnits: Long, locale: Locale = Locale.getDefault()): String {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    val fractionDigits = runCatching { formatter.currency?.defaultFractionDigits ?: 2 }.getOrDefault(2)
    val divisor = if (fractionDigits <= 0) 1.0 else 10.0.pow(fractionDigits.toDouble())
    return formatter.format(minorUnits / divisor)
}

fun formatDistance(kilometres: Long?): String = kilometres?.let { "%,d km".format(it) } ?: "—"

fun formatEfficiency(value: Double?): String = value?.let { "%.1f L/100 km".format(it) } ?: "Not enough data"
