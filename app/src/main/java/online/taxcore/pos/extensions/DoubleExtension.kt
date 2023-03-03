package online.taxcore.pos.extensions

import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

fun Double.roundLocalized(places: Int = 2) =
    String.format("%.${places}f", this)

fun Double.roundToDecimal(places: Int = 2) =
    BigDecimal(this).setScale(places, RoundingMode.HALF_UP).toString()

fun Double.roundingToDecimal(places: Int) =
    BigDecimal(this).setScale(places, RoundingMode.HALF_UP).toDouble()

val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024
