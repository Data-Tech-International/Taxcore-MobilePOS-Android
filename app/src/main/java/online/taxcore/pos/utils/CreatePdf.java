package online.taxcore.pos.utils;

import android.os.Environment;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CreatePdf {
    public static final String FONT_CONSOLA_MONO = "/assets/fonts/ConsolaMono.ttf";
    //write method takes two parameter pdf name and content
    //return true if pdf successfully created
    public static Boolean write(String fileName, String fileContent, byte[] imageByteArray, String pdfFooter) {
        try {
            //Create file path for Pdf
            String fpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName + ".pdf";
            File file = new File(fpath);
            if (!file.exists()) {
                file.createNewFile();
            }

            // To customise the text of the pdf
            // we can use FontFamily
            BaseFont bf = BaseFont.createFont(FONT_CONSOLA_MONO, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font bfBold12 = new Font(bf, 7, Font.NORMAL, new BaseColor(0, 0, 0));

            Image qrCodeImage = Image.getInstance(imageByteArray);
            qrCodeImage.scaleAbsolute(163, 163);

            PdfPTable table = new PdfPTable(1);
            table.setTotalWidth(174);
            table.setLockedWidth(true);
            PdfPCell cell = new PdfPCell();
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            cell.setBorder(Rectangle.NO_BORDER);
            Paragraph p1 = new Paragraph(fileContent, bfBold12);
            p1.setLeading(0, 1);
            cell.addElement(p1);

            cell.addElement(qrCodeImage);

            Paragraph p2 = new Paragraph(pdfFooter, bfBold12);
            p2.setLeading(0, 1);
            cell.addElement(p2);

            table.addCell(cell);

            Rectangle pagesize = new Rectangle(174, 800);
            Document document = new Document(pagesize);
            document.setMargins(5, 5, 0, 0);

            PdfWriter.getInstance(document, new FileOutputStream(file.getAbsoluteFile()));
            document.open();

            document.add(table);

            // close document
            document.close();
            return true;
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
