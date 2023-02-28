package online.taxcore.pos.helpers

import android.app.Activity
import android.app.AlertDialog
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import online.taxcore.pos.R

class AlertDialogHelper {

    companion object {

        fun showNotInternetDialog(
            activity: Activity,
            @StringRes titleText: Int = R.string.error_no_internet_title,
            @StringRes messageText: Int = R.string.msg_configure_application,
            @StringRes buttonText: Int = android.R.string.ok,
            onPositive: () -> Unit = {}
        ) {

            MaterialDialog(activity).show {
                title(titleText)
                message(messageText)

                positiveButton(buttonText) {
                    this.dismiss()
                    onPositive()
                }
            }
        }

        fun showSimpleAlertDialog(activity: FragmentActivity?, message: String) {
            val builder = activity?.let {
                AlertDialog.Builder(it)
                    .setTitle("Error message")
                    .setMessage(message)
                    .setPositiveButton("Ok") { dialog, _ ->
                        dialog.cancel()
                    }.create()
            }
            builder?.show()
        }

    }
}
