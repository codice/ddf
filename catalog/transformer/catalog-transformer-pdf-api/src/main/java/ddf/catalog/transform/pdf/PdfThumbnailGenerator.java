package ddf.catalog.transform.pdf;

import java.io.IOException;
import java.io.InputStream;

public interface PdfThumbnailGenerator {
    byte[] generatePdfThumbnail(InputStream pdfInputStream) throws IOException;
}
