package online.taxcore.pos.utils

import android.os.Environment
import java.io.File
import java.util.*

object Helpers {
    fun createDocumentsFilePath(fileName: String = UUID.randomUUID().toString()): String {
        val documentsFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

        return documentsFolder.absolutePath + File.separator + fileName
    }
}
