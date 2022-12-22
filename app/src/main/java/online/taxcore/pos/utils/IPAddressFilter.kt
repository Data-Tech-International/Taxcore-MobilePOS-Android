package online.taxcore.pos.utils

import android.text.InputFilter
import android.text.Spanned

class IPAddressFilter constructor(private var min: Int, private var max: Int, private val ipInterface: IPAddressFilterInterface) : InputFilter {
    override fun filter(source:CharSequence, start:Int, end:Int, dest: Spanned, dstart:Int, dend:Int): CharSequence? {
        try
        {
            val input = Integer.parseInt(dest.toString() + source.toString())
            if (isInRange(min, max, input))
                return null
            else
                ipInterface.shouldPassToNextEditText(input.toString())
        }
        catch (nfe:NumberFormatException) {}
        return ""
    }
    private fun isInRange(a:Int, b:Int, c:Int):Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}

interface IPAddressFilterInterface {
    fun shouldPassToNextEditText(symbol: String)
}