package online.taxcore.pos.ui.invoice;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MyPrintDocumentAdapter extends PrintDocumentAdapter {

    private final String fileName;
    private final String path;

    public MyPrintDocumentAdapter(String fileName, String path) {
        this.fileName = fileName;
        this.path = path;
    }

    @Override
    public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes1,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback,
                         Bundle bundle) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo pdi = new PrintDocumentInfo.Builder(fileName + ".pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build();

        callback.onLayoutFinished(pdi, true);
    }

    @Override
    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        InputStream input = null;
        OutputStream output = null;

        try {
            String name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() +
                    File.separator + fileName + ".pdf";

            input = new FileInputStream(new File(name));
            output = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

            byte[] buf = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

        } catch (Exception e) {
            //Catch exception
        } finally {
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

