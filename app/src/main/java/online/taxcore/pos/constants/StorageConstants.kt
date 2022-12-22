package online.taxcore.pos.constants

import android.os.Environment

class StorageConstants {
    companion object {
        val DOWNLOAD_STORAGE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
    }
}
