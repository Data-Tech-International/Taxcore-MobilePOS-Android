package online.taxcore.pos.data.services

import UnzipUtils
import android.os.Environment
import android.util.Base64
import okhttp3.ResponseBody
import online.taxcore.pos.data.api.APIClient
import online.taxcore.pos.data.local.CertManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileNotFoundException
import java.util.*

enum class ErrorType {
    REQUEST_INVALID,
    REQUEST_FAILED,
    RESPONSE_INVALID,
    NO_CERT_FILE_FOUND,
    INVALID_OR_USED_LINK
}

object DownloadService {
    fun downloadCert(
        endpoint: String,
        onStart: () -> Unit,
        onSuccess: (encodedPfx: String, p12FileName: String) -> Unit,
        onError: (type: ErrorType, message: String?) -> Unit,
        onEnd: () -> Unit
    ) {

        onStart()

        val client = APIClient.core(endpoint)

        val downloadUrl = endpoint.trim().removeSuffix("/")
        val downloadRequest = client?.download(downloadUrl)

        if (downloadRequest == null) {
            onError(ErrorType.REQUEST_INVALID, null)
            return onEnd()
        }

        downloadRequest.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(ErrorType.REQUEST_FAILED, null)
                onEnd()
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                if (response.errorBody() != null) {
                    onError(ErrorType.RESPONSE_INVALID, null)
                    return onEnd()
                }

                val body = response.body() ?: return onEnd()

                if (body.contentType().toString().contains("application/zip").not()) {
                    onError(ErrorType.INVALID_OR_USED_LINK, null)
                    return onEnd()
                }

                val documentsFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

                val fileNameBase = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1).ifBlank {
                    UUID.randomUUID().toString()
                }

                val zipFileName = "$fileNameBase.zip"
                val zipFilePath = documentsFolder.absolutePath + File.separator + zipFileName

                File(zipFilePath).writeBytes(body.bytes())

                val zipFile = File(zipFilePath)
                val extractDirPath = documentsFolder.absolutePath + File.separator + fileNameBase

                try {
                    UnzipUtils.unzip(
                        zipFile,
                        extractDirPath
                    )
                } catch (ex: FileNotFoundException) {
                    onError(ErrorType.NO_CERT_FILE_FOUND, null)
                    return onEnd()
                }

                val extractDir = File(extractDirPath)
                val fileList = extractDir.list().orEmpty()

                if (fileList.isEmpty()) {
                    onError(ErrorType.NO_CERT_FILE_FOUND, null)
                    return onEnd()
                }

                val certFileSuffix = ".nochain.p12"
                // Find certificate file from the extracted files
                val p12FileName = fileList.find { it.endsWith(certFileSuffix) }


                if (p12FileName.isNullOrEmpty()) {
                    // No need for extract dir and
                    extractDir.deleteRecursively()
                    zipFile.delete()
                    onError(ErrorType.NO_CERT_FILE_FOUND, null)
                    return onEnd()
                }

                val p12File = File(extractDirPath + File.separator + p12FileName)
                val encodedPfx = Base64.encodeToString(p12File.readBytes(), Base64.DEFAULT)

                // Save certificate for latter use
                CertManager.saveCert(fileNameBase, p12FileName, encodedPfx)

                // No need for extract dir and files
                extractDir.deleteRecursively()
                zipFile.delete()

                onSuccess(encodedPfx, p12FileName)
                onEnd()
            }

        })
    }

}
