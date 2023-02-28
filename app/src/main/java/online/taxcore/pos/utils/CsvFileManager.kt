package online.taxcore.pos.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pawegio.kandroid.runAsync
import online.taxcore.pos.data_managers.CatalogManager
import online.taxcore.pos.extensions.sizeInKb
import online.taxcore.pos.models.Item
import online.taxcore.pos.models.Taxes
import java.io.*
import java.nio.charset.Charset
import java.util.*

class CsvFileManager {
    companion object {
        private const val CSV_DATA_DELIMITER = ","

        fun importCatalog(
            sourceFile: File,
            context: Context?,
            onSuccess: (List<Item>) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                try {
                    val catalogList: ArrayList<Item> = arrayListOf()
                    val buffered = BufferedReader(FileReader(sourceFile))

                    var skipFirstLine = true
                    buffered.forEachLine { lineInFile ->
                        if (skipFirstLine) {
                            skipFirstLine = false
                            return@forEachLine
                        }

                        val tokens = lineInFile.split(CSV_DATA_DELIMITER)
                        val itemBarcode = tokens[0].trim()
                        val itemName = tokens[1].trim()
                        val itemPrice = tokens[2].toDouble()
                        val itemTaxes =
                            tokens.subList(3, tokens.size - 1).map { it.replace("\"", "") }
                        val itemIsFavorite = tokens[tokens.size - 1].trim().toBoolean()

                        // Validate import items
                        if (itemName.isBlank() && itemName.length >= 1000) {
                            return@forEachLine
                        }

                        if (itemPrice < 0.01) {
                            return@forEachLine
                        }

                        val isEanInvalid = if (itemBarcode.isBlank()) {
                            false
                        } else {
                            TextUtils.isDigitsOnly(itemBarcode)
                                .not() && (itemBarcode.length in 8..16).not()
                        }

                        if (isEanInvalid) {
                            return@forEachLine
                        }

                        if (itemTaxes.isEmpty()) {
                            return@forEachLine
                        }

                        val item = Item()
                        with(item) {
                            barcode = itemBarcode
                            name = itemName
                            price = itemPrice
                            isFavorite = itemIsFavorite
                        }

                        itemTaxes.forEach { taxName ->
                            if (taxName.isNotBlank()) {
                                val tax = Taxes()
                                tax.code = taxName
                                item.tax.add(tax)
                            }
                        }

                        catalogList.add(item)
                    }

                    if (catalogList.isNotEmpty()) {
                        CatalogManager.replaceCatalog(catalogList)
                        onSuccess(catalogList)
                    } else {
                        onError("Wrong CSV type")
                    }

                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    onError("File not found")
                } catch (e: NumberFormatException) {
                    Log.wtf("ERROR", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    onError("Wrong data type.")
                } catch (e: IllegalStateException) {
                    Log.wtf("ERROR", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    onError("Error occurred")
                } finally {
                    FileUtils.trimCache(context)
                }
            }
        }

        fun exportCatalog(
            context: Context?,
            uriData: Uri,
            items: MutableList<Item>,
            onSuccess: (Boolean) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                var writer: Writer? = null
                try {
                    val outputFile = FileUtils.getFileFromDownloads(context!!, uriData)
                    writer = FileWriter(outputFile)

                    val CSV_HEADER = arrayListOf(
                        "EAN",
                        "Name",
                        "UnitPrice",
                        "TaxLabels",
                        "isFavorite"
                    ).joinToString(CSV_DATA_DELIMITER)
                    outputFile.appendText(CSV_HEADER)
                    outputFile.appendText("\n")

                    for (item in items) {
                        outputFile.appendText(item.barcode)
                        outputFile.appendText(CSV_DATA_DELIMITER)
                        outputFile.appendText(item.name, Charset.defaultCharset())
                        outputFile.appendText(CSV_DATA_DELIMITER)
                        outputFile.appendText(item.price.toString())
                        outputFile.appendText(CSV_DATA_DELIMITER)

                        val taxes = item.tax.joinToString(",", "\"", "\"") { it.code }
                        outputFile.appendText(taxes)
                        outputFile.appendText(CSV_DATA_DELIMITER)
                        outputFile.appendText(item.isFavorite.toString().toUpperCase(Locale.ROOT))
                        outputFile.appendText("\n")
                    }
                    val availableSize = FileUtils.getAvailableSpaceInKB()

                    if (outputFile.sizeInKb > availableSize) {
                        onError("Not enough space to export catalog.")
                        return@runAsync
                    }
                    onSuccess(true)
                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    onError("Unable to export catalog. File not found.")
                } catch (e: RuntimeException) {
                    Log.wtf("ERROR", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    onError("Unable to export catalog.")
                } finally {
                    writer?.flush()
                    writer?.close()

                    FileUtils.trimCache(context)
                }
            }
        }
    }
}
