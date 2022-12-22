package online.taxcore.pos.extensions

import android.content.Intent
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.addFragment(fragment: Fragment, id: Int) {
    val fragmentTransaction = supportFragmentManager.beginTransaction()
    fragmentTransaction.replace(id, fragment)
    fragmentTransaction.commit()
}

fun Fragment.baseActivity() = activity as? AppCompatActivity

fun Fragment.replaceFragment(@IdRes fragmentId: Int, fragment: Fragment, addToBackStack: Boolean = false) {
    val activity = activity as? AppCompatActivity

    val transaction = activity?.supportFragmentManager?.beginTransaction()?.apply {
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back

        replace(fragmentId, fragment)
        if (addToBackStack) {
            addToBackStack(fragment.javaClass.simpleName)
        }
    }

    // Commit the transaction
    transaction?.commit()
}

fun <T : AppCompatActivity> Fragment.showActivity(clazz: Class<T>) = baseActivity()?.showActivity(clazz)

fun <T : AppCompatActivity> AppCompatActivity.showActivity(clazz: Class<T>) {
    startActivity(Intent(this, clazz)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK))

}
