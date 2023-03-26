package online.taxcore.pos.helpers

import android.text.InputFilter
import android.text.Spanned

internal class DecimalDigitsInputFilter(
    private val digitsBeforeZero: Int,
    private val digitsAfterZero: Int
) :
    InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {


        if (dest.contains(".")) {
            val (beforeZero, afterZero) = dest.split(".")

            if (afterZero.length < digitsAfterZero) {
                return null
            }

            if (beforeZero.length == digitsBeforeZero) {
                return ""
            }

            return ""
        }

        if (dest.length == digitsBeforeZero) {
            return if (source == ".") return null else ""
        }

        return null
    }
}
