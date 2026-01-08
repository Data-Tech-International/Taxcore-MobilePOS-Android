package online.taxcore.pos.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Toast extensions
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.toast(message, duration)
}

fun Fragment.longToast(message: String) {
    context?.longToast(message)
}

fun Activity.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Activity.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// Activity extensions
fun Activity.runOnUiThread(action: () -> Unit) {
    if (!isFinishing) {
        runOnUiThread(Runnable(action))
    }
}

fun Fragment.runOnUiThread(action: () -> Unit) {
    activity?.runOnUiThread(action)
}

// SearchView extensions
fun SearchView.onQueryChange(action: (String) -> Unit) {
    this.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            action(newText ?: "")
            return true
        }
    })
}

// Coroutine extensions for async operations
fun runAsync(action: suspend () -> Unit) {
    GlobalScope.launch(Dispatchers.IO) {
        action()
    }
}