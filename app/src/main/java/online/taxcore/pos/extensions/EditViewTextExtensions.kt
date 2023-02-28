package online.taxcore.pos.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText

fun EditText.onTextChanged(callback: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            callback.invoke(s.toString())
        }
    })
}

fun AppCompatEditText.stringOrNull() = if (this.text.isNullOrBlank()) null else this.text.toString()

fun checkRequiredFields(vararg editTexts: EditText): Boolean {
    return editTexts.all { it.text.isNotEmpty() }
}
