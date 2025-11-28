package online.taxcore.pos.utils

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.saveAll
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.enums.JournalError
import org.json.JSONException
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader

object JsonFileManager {

    fun importJournals(
        context: Context?,
        sourceFile: File,
        onSuccess: (List<Journal>) -> Unit,
        onError: (JournalError) -> Unit
    ) {
        try {
            val buffered = BufferedReader(FileReader(sourceFile))
            val gson = Gson()

            val invoicesList: List<Journal> =
                gson.fromJson(buffered, object : TypeToken<List<Journal>>() {}.type)
            val savedInvoices = Journal().queryAll().toList()

            if (invoicesList.isNotEmpty() && invoicesList.first().type == "Journal" && invoicesList.first().id.isNotEmpty()) {

                val existingInvoiceIds = savedInvoices.map { it.invoiceNumber }
                val importInvoices = invoicesList
                    .distinctBy { it.invoiceNumber }
                    .filter { existingInvoiceIds.contains(it.invoiceNumber).not() }

                importInvoices.saveAll()

                onSuccess(importInvoices)
            } else {
                onError(JournalError.WRONG_JSON_TYPE)
            }
        } catch (e: JSONException) {
            Log.wtf("ERROR", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            onError(JournalError.INVALID_JSON_FORMAT)
        } catch (e: FileNotFoundException) {
            Log.wtf("ERROR", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            onError(JournalError.FILE_NOT_FOUND)
        } catch (e: IllegalStateException) {
            Log.wtf("ERROR", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            onError(JournalError.INVALID_JSON_FORMAT)
        } catch (e: Exception) {
            Log.wtf("ERROR", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            onError(JournalError.UNABLE_TO_IMPORT)
        } finally {
            FileUtils.trimCache(context)
        }
    }

}
