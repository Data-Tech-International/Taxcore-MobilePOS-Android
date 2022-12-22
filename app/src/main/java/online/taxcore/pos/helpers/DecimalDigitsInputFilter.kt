package online.taxcore.pos.helpers

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern


internal class DecimalDigitsInputFilter(digitsBeforeZero: Int, digitsAfterZero: Int) : InputFilter {
    private val mPattern: Pattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?")

    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val matcher = mPattern.matcher(dest)
        return if (!matcher.matches()) "" else null
    }
}
