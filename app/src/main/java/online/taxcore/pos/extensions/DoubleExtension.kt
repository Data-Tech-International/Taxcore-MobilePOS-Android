package online.taxcore.pos.extensions

import java.io.File
import java.math.BigDecimal

fun Double.roundTo2DecimalPlaces() =
    BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toString()

fun Double.roundToDecimalPlaces(scale: Int) =
    BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toString()

fun Double.roundingToDecimal(places: Int) =
    BigDecimal(this).setScale(places, BigDecimal.ROUND_HALF_UP).toFloat()

val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024
