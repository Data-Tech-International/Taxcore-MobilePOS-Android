// TODO: Migrate to iText7 - This file needs to be completely rewritten for iText7
// Commenting out for now to avoid build errors
/*
package online.taxcore.pos.utils

import com.itextpdf.kernel.colors.Color
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.VerticalAlignment
import com.itextpdf.io.image.ImageDataFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object CreatePdf {
    private const val FONT_CONSOLA_MONO = "/assets/fonts/ConsolaMono.ttf"

    //write method takes two parameter pdf name and content
    //return true if pdf successfully created
    fun write(
        filePath: String?,
        invoiceJournal: String,
        imageByteArray: ByteArray?
    ): Boolean {
        return try {
            val file = File(filePath.toString())
            if (!file.exists()) {
                file.createNewFile()
            }

            val delimiter = "========================================\r\n"
            val invoiceHeader = invoiceJournal.split(delimiter).first()

            val headerElements = invoiceHeader.split("\r\n".toRegex())

            val headerStart = headerElements.first()
            val companyHeader = headerElements.subList(1, 6).joinToString("\r\n")
            val headerInfo = headerElements.subList(6, headerElements.size - 1).joinToString("\r\n")

            val invoiceFooter = invoiceJournal.split(delimiter).last()
            val invoiceMain = invoiceJournal
                .replace(invoiceHeader, "")
                .replace(invoiceFooter, "")

            val font = PdfFontFactory.createFont(FONT_CONSOLA_MONO, "Identity-H", PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
            val qrCodeImage = Image(ImageDataFactory.create(imageByteArray))
            qrCodeImage.scaleAbsolute(163f, 163f)

            val pHeaderStart = Paragraph(headerStart).setFont(font).setFontSize(7f)
            pHeaderStart.setLeading(0f, 1f)

            val pCompanyText = Paragraph(companyHeader).setFont(font).setFontSize(7f)
            pCompanyText.setLeading(0f, 1f)
            pCompanyText.alignment = Element.ALIGN_CENTER

            val pHeaderInfo = Paragraph(headerInfo, bfBold12)
            pHeaderInfo.setLeading(0f, 1f)

            val pInvoiceMain = Paragraph(invoiceMain, bfBold12)
            pInvoiceMain.setLeading(0f, 1f)

            val pFooterText = Paragraph(invoiceFooter, bfBold12)
            pFooterText.setLeading(0f, 1f)

            val cell = PdfPCell()
            cell.border = Rectangle.NO_BORDER

            cell.addElement(pHeaderStart)
            cell.addElement(pCompanyText)
            cell.addElement(pHeaderInfo)
            cell.addElement(pInvoiceMain)
            cell.addElement(qrCodeImage)
            cell.addElement(pFooterText)

            val table = PdfPTable(1)
            table.totalWidth = 174f
            table.isLockedWidth = true
            table.addCell(cell)

            val rows = invoiceJournal.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray().size
            val pageSize = Rectangle(174f, (163 + rows * 7.7).toFloat())

            val document = Document(pageSize)
            document.setMargins(5f, 5f, 0f, 0f)

            PdfWriter.getInstance(document, FileOutputStream(file.absoluteFile))

            document.open()
            document.add(table)

            // close document
            document.close()

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
*/
