package online.taxcore.pos.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.pawegio.kandroid.runAsync
import online.taxcore.pos.data.local.CatalogManager
import online.taxcore.pos.data.realm.Item
import online.taxcore.pos.data.realm.Taxes
import online.taxcore.pos.extensions.sizeInKb
import java.io.*
import java.util.*

object CatalogFileManager {

    private const val CSV_DATA_DELIMITER = ","
    private val CSV_HEADER = arrayListOf(
        "EAN",
        "Name",
        "UnitPrice",
        "TaxLabels",
        "isFavorite"
    )

    fun importCsvCatalog(
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
            } catch (e: java.lang.IndexOutOfBoundsException) {
                Log.wtf("ERROR", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                onError("Invalid file content.")
            } catch (e: IllegalStateException) {
                Log.wtf("ERROR", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                onError("Error occurred")
            } finally {
                FileUtils.trimCache(context)
            }
        }
    }

    fun exportCsvCatalog(
        context: Context?,
        csvFilePath: String,
        items: MutableList<Item>,
        header: List<String> = CSV_HEADER,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        runAsync {
            var outStream: FileOutputStream? = null
            try {
                val outputFile = File(csvFilePath)
                outStream = FileOutputStream(outputFile)

                // Write file encoding
                val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                outStream.write(bom)

                val fileContent = generateCsvFileContent(header, items)
                outStream.write(fileContent.toByteArray(Charsets.UTF_8))

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
                outStream?.flush()
                outStream?.close()
                FileUtils.trimCache(context)
            }
        }
    }

    fun generateCsvFileContent(
        header: List<String> = CSV_HEADER,
        items: MutableList<Item>
    ): String {
        val output = StringBuilder()
        val csvHeader = header.joinToString(CSV_DATA_DELIMITER)
        output.appendLine(csvHeader)

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
            output.appendLine(rowItem)
        }

        return output.toString()
    }

    fun generateJsonFileContent(
        catalogItems: MutableList<Item>
    ): String {
        return GsonBuilder().setPrettyPrinting().create().toJson(catalogItems)
    }

    fun exportJsonCatalog(
        context: Context?,
        destinationFilePath: String,
        items: MutableList<Item>,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        runAsync {
            try {
                val outputFile = File(destinationFilePath)
                val json = generateJsonFileContent(items)

                outputFile.writeText(json, Charsets.UTF_8)

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
                FileUtils.trimCache(context)
            }
        }
    }

    fun importJsonCatalog(
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
                FirebaseCrashlytics.getInstance().recordException(e)
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

}
