package online.taxcore.pos.utils

import android.content.Context
import android.net.ConnectivityManager

fun Context.isOffline(): Boolean {
    val connMgr = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connMgr.activeNetworkInfo
    return !(networkInfo != null && networkInfo.isConnected)
}
