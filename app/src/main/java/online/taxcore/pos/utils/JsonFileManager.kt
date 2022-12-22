package online.taxcore.pos.utils


import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.pawegio.kandroid.longToast
import com.pawegio.kandroid.runAsync
import com.vicpin.krealmextensions.queryAll
import com.vicpin.krealmextensions.saveAll
import okhttp3.internal.toImmutableList
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.Journal
import online.taxcore.pos.extensions.sizeInKb
import java.io.*
import java.nio.charset.StandardCharsets

class JsonFileManager {

    companion object {

        fun exportCatalog(
            context: Context?,
            destinationFilePath: String,
            items: MutableList<Item>,
            onSuccess: (Boolean) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                var writer: Writer? = null
                try {
                    val outputFile = File(destinationFilePath)
                    writer =
                        OutputStreamWriter(FileOutputStream(outputFile), StandardCharsets.UTF_8)

                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val json = gson.toJson(items)

                    outputFile.writeText(json, Charsets.UTF_8)

                    val availableSize = FileUtils.getAvailableSpaceInKB()

                    if (outputFile.sizeInKb > availableSize) {
                        onError("Not enough space to export catalog.")
                        return@runAsync
                    }

                    onSuccess(true)
                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)
                    onError("Unable to export catalog. File not found.")
                } catch (e: RuntimeException) {
                    Log.wtf("ERROR", e)

                    onError("Unable to export catalog.")
                } finally {
                    writer?.flush()
                    writer?.close()

                    FileUtils.trimCache(context)
                }
            }
        }

        fun importCatalog(
            sourceFile: File,
            context: Context?,
            onSuccess: (List<Item>) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                try {
                    val buffered = BufferedReader(FileReader(sourceFile))
                    val catalogList = Gson().fromJson<List<Item>>(
                        buffered,
                        object : TypeToken<List<Item>>() {}.type
                    )

                    if (catalogList.isNotEmpty() && catalogList.first().type == "Catalog") {
                        CatalogManager.replaceCatalog(catalogList)
                        onSuccess(catalogList)
                    } else {
                        onError("Wrong JSON type")
                    }
                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)
                } catch (e: IllegalStateException) {
                    Log.wtf("ERROR", e)
                } catch (e: JsonSyntaxException) {
                    Log.wtf("ERROR", e)
                    onError("Wrong JSON type")
                } finally {
                    FileUtils.trimCache(context)
                }
            }
        }

        fun importJournals(activity: Activity?, sourceFile: File): List<Journal> {
            try {
                val buffered = BufferedReader(FileReader(sourceFile))
                val gson = Gson()

                val invoicesList: List<Journal> =
                    gson.fromJson(buffered, object : TypeToken<List<Journal>>() {}.type)
                val savedInvoices = Journal().queryAll().toImmutableList()

                if (invoicesList.isNotEmpty() && invoicesList.first().type == "Journal" && invoicesList.first().id.isNotEmpty()) {

                    val existingInvoiceIds = savedInvoices.map { it.invoiceNumber }
                    val importInvoices = invoicesList
                        .distinctBy { it.invoiceNumber }
                        .filter { existingInvoiceIds.contains(it.invoiceNumber).not() }

                    importInvoices.saveAll()

                    return importInvoices
                }

                activity?.longToast("Wrong JSON type")
            } catch (e: FileNotFoundException) {
                Log.wtf("ERROR", e)
            } catch (e: IllegalStateException) {
                Log.wtf("ERROR", e)

            } finally {
                FileUtils.trimCache(activity)
            }

            return arrayListOf()
        }

        fun exportJournal(
            context: Context?,
            destinationFilePath: String,
            items: MutableList<Journal>,
            onSuccess: (Boolean) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                var outStream: FileOutputStream? = null
                try {
                    val outputFile = File(destinationFilePath)

                    outStream = FileOutputStream(outputFile)
                    val bufferedWriter = BufferedWriter(OutputStreamWriter(outStream, "UTF-8"))

                    val gson = GsonBuilder().setPrettyPrinting().create()
                    gson.toJson(items, bufferedWriter)

                    bufferedWriter.close()

                    onSuccess(true)
                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)

                    onError("Unable to export journals. File not found.")
                } catch (e: RuntimeException) {
                    Log.wtf("ERROR", e)
                    onError("Unable to export journals.")
                } finally {
                    outStream?.flush()
                    outStream?.close()

                    FileUtils.trimCache(context)
                }
            }
        }
    }
}
