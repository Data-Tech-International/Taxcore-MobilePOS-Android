package online.taxcore.pos.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.pawegio.kandroid.runAsync
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.Taxes
import online.taxcore.pos.extensions.sizeInKb
import java.io.*
import java.nio.charset.StandardCharsets
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
                    onError("File not found")
                } catch (e: NumberFormatException) {
                    Log.wtf("ERROR", e)
                    onError("Wrong data type.")
                } catch (e: IllegalStateException) {
                    Log.wtf("ERROR", e)
                    onError("Error occurred")
                } finally {
                    FileUtils.trimCache(context)
                }
            }
        }

        private val CSV_HEADER = arrayListOf(
            "EAN",
            "Name",
            "UnitPrice",
            "TaxLabels",
            "isFavorite"
        )

        fun exportCatalog(
            context: Context?,
            csvFilePath: String,
            items: MutableList<Item>,
            header: List<String> = CSV_HEADER,
            onSuccess: (Boolean) -> Unit,
            onError: (String) -> Unit
        ) {
            runAsync {
                var writer: BufferedWriter? = null
                var outStream: FileOutputStream? = null
                try {
                    val outputFile = File(csvFilePath)
                    outStream = FileOutputStream(outputFile)

                    // Write file encoding
                    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                    outStream.write(bom)
                    writer = BufferedWriter(OutputStreamWriter(outStream, StandardCharsets.UTF_8))

                    val csvHeader = header.joinToString(CSV_DATA_DELIMITER)
                    writer.appendLine(csvHeader)

                    for (item in items) {
                        val taxes = item.tax.joinToString(",", "\"", "\"") { it.code }
                        val rowItem = StringBuilder()
                            .append(item.barcode)
                            .append(CSV_DATA_DELIMITER)
                            .append(item.name)
                            .append(CSV_DATA_DELIMITER)
                            .append(item.price.toString())
                            .append(CSV_DATA_DELIMITER)
                            .append(taxes)
                            .append(CSV_DATA_DELIMITER)
                            .append(
                                item.isFavorite.toString().toUpperCase(Locale.ROOT)
                            )
                        writer.appendLine(rowItem)
                    }

                    val availableSize = FileUtils.getAvailableSpaceInKB()

                    if (outputFile.sizeInKb > availableSize) {
                        onError("Not enough space to export catalog.")
                        return@runAsync
                    }

                    writer.close()

                    onSuccess(true)
                } catch (e: FileNotFoundException) {
                    Log.wtf("ERROR", e)
                    onError("Unable to export catalog. File not found.")
                } catch (e: RuntimeException) {
                    Log.wtf("ERROR", e)
                    onError("Unable to export catalog.")
                } finally {
                    writer?.close()
                    outStream?.flush()
                    outStream?.close()

                    FileUtils.trimCache(context)
                }
            }
        }
    }
}
