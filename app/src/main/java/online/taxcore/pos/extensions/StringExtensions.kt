package online.taxcore.pos.extensions

import android.os.Build
import android.text.Html
import android.text.Spanned
import java.math.BigDecimal
import java.security.MessageDigest

val String.md5: String
    get() {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

fun String.seq(): CharSequence = subSequence(0, length)

fun String.contains(str: String, ignoreCase: Boolean = false) =
    contains(str.seq(), ignoreCase)

fun String.stringOrNull() = if (this.isBlank()) null else this

fun String.roundTo2DecimalPlaces() =
    BigDecimal(this).setScale(3, BigDecimal.ROUND_HALF_UP)

fun String.roundToDecimalPlaces(scale: Int) =
    BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP)

fun String.removeBrackets() =
    this.replace("}", "").replace("{", "")

fun String.showErrorMessage() =
    when {
        this.contains("2310") -> {
            this.replace("2310", "Tax labels sent by the POS are not defined")
        }
        this.contains("2801") -> {
            this.replace("2801", "The length of the field value is longer than expected")
        }
        this.contains("2802") -> {
            this.replace("2802", "The length of the field value is shorter than expected")
        }
        this.contains("2803") -> {
            this.replace("2803", "The length of the field value is shorter or longer than expected")
        }
        this.contains("2804") -> {
            this.replace("2804", "The field value out of expected range")
        }
        this.contains("2805") -> {
            this.replace("2805", "The field contains invalid value")
        }
        this.contains("2806") -> {
            this.replace("2806", "The data format is invalid")
        }
        this.contains("2807") -> {
            this.replace("2807", "The list contains less than minimum required elements count")
        }
        this.contains("2808") -> {
            this.replace("2808", "The list exceeds maximum allowed elements count.")
        }
        this.contains("2100") -> {
            this.replace("2100", "PIN code sent by the POS is invalid.")
        }
        this.contains("2210") -> {
            this.replace(
                "2210",
                "Secure Element is locked. No additional invoices can be signed before the audit is completed."
            )
        }
        this.contains("2220") -> {
            this.replace("2220", "E-SDC cannot connect to the Secure Element applet.")
        }
        this.contains("2230") -> {
            this.replace(
                "2230",
                "Secure Element does not support requested protocol version (reserved for later use)."
            )
        }
        this.contains("2400") -> {
            this.replace("2400", "Device is not configured.")
        }
        else -> this
    }

fun String.fromHtml(): Spanned = if (Build.VERSION.SDK_INT >= 24) {
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
} else {
    Html.fromHtml(this) // or for older api
}
