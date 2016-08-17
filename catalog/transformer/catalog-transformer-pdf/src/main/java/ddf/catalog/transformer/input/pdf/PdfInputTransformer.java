/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.input.pdf;

import static org.apache.commons.lang.Validate.notNull;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.MediaType;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class PdfInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfInputTransformer.class);

    private static final int RESOLUTION_DPI = 44;

    private static final float IMAGE_QUALITY = 1.0f;

    private static final float IMAGE_HEIGHTWIDTH = 128;

    private static final String FORMAT_NAME = "jpg";

    private static final GeoPdfParser GEO_PDF_PARSER = new GeoPdfParser();

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(
            "yyyy-MM-dd'T'HH:mm:ssZZ");

    private final CheckedFunction<InputStream, PDDocument> inputStreamPDDocumentFunction;

    private final CheckedFunction<PDDocument, String> geoParserFunction;

    private final CheckedFunction<PDDocument, byte[]> thumbnailFunction;

    private MetacardType metacardType;

    private boolean usePdfTitleAsTitle;

    /**
     * @param metacardType       must be non-null
     * @param usePdfTitleAsTitle must be non-null
     */
    public PdfInputTransformer(MetacardType metacardType, Boolean usePdfTitleAsTitle) {
        this(metacardType,
                usePdfTitleAsTitle,
                PDDocument::load,
                GEO_PDF_PARSER::getWktFromPDF,
                PdfInputTransformer::generatePdfThumbnail);
    }

    /**
     * Note: direct use of this constructor is meant for unit testing only
     *
     * @param metacardType                  must be non-null
     * @param usePdfTitleAsTitle            must be non-null
     * @param inputStreamPDDocumentFunction must be non-null
     * @param geoParserFunction             must be non-null
     * @param thumbnailFunction             must be non-null
     */
    PdfInputTransformer(MetacardType metacardType, Boolean usePdfTitleAsTitle,
            CheckedFunction<InputStream, PDDocument> inputStreamPDDocumentFunction,
            CheckedFunction<PDDocument, String> geoParserFunction,
            CheckedFunction<PDDocument, byte[]> thumbnailFunction) {
        notNull(metacardType, "metacardType must be non-null");
        notNull(usePdfTitleAsTitle, "usePdfTitleAsTitle must be non-null");
        notNull(inputStreamPDDocumentFunction, "inputStreamPDDocumentFunction must be non-null");
        notNull(geoParserFunction, "geoParserFunction must be non-null");
        notNull(thumbnailFunction, "thumbnailFunction must be non-null");

        this.metacardType = metacardType;
        this.usePdfTitleAsTitle = usePdfTitleAsTitle;
        this.inputStreamPDDocumentFunction = inputStreamPDDocumentFunction;
        this.geoParserFunction = geoParserFunction;
        this.thumbnailFunction = thumbnailFunction;
    }

    private static byte[] generatePdfThumbnail(PDDocument pdfDocument) throws IOException {

        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

        if (pdfDocument.getNumberOfPages() < 1) {
            /*
             * Can there be a PDF with zero pages??? Should we throw an error or what? The
             * original implementation assumed that a PDF would always have at least one
             * page. That's what I've implemented here, but I don't like make those
             * kinds of assumptions :-( But I also don't want to change the original
             * behavior without knowing how it will impact the system.
             */
        }

        BufferedImage image = pdfRenderer.renderImageWithDPI(0, RESOLUTION_DPI, ImageType.RGB);

        int largestDimension = Math.max(image.getHeight(), image.getWidth());
        float scalingFactor = IMAGE_HEIGHTWIDTH / largestDimension;
        int scaledHeight = (int) (image.getHeight() * scalingFactor);
        int scaledWidth = (int) (image.getWidth() * scalingFactor);

        BufferedImage scaledImage = new BufferedImage(scaledWidth,
                scaledHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIOUtil.writeImage(scaledImage,
                    FORMAT_NAME,
                    outputStream,
                    RESOLUTION_DPI,
                    IMAGE_QUALITY);
            return outputStream.toByteArray();
        }
    }

    public boolean isUsePdfTitleAsTitle() {
        return usePdfTitleAsTitle;
    }

    /**
     * @param usePdfTitleAsTitle must be non-null
     */
    public void setUsePdfTitleAsTitle(Boolean usePdfTitleAsTitle) {
        notNull(usePdfTitleAsTitle, "usePdfTitleAsTitle must be non-null");
        this.usePdfTitleAsTitle = usePdfTitleAsTitle;
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        return transformWithoutExtractors(input, id);
    }

    private Metacard transformWithoutExtractors(InputStream input, String id) throws IOException {
        try (PDDocument pdfDocument = inputStreamPDDocumentFunction.apply(input)) {
            return transformPdf(id, pdfDocument);
        } catch (InvalidPasswordException e) {
            LOGGER.debug("Cannot transform encrypted pdf", e);
            return initializeMetacard(id);
        }
    }

    private MetacardImpl initializeMetacard(String id) {
        MetacardImpl metacard;

        metacard = new MetacardImpl(metacardType);

        metacard.setId(id);
        metacard.setContentTypeName(MediaType.PDF.toString());
        metacard.setAttribute(Media.TYPE, MediaType.PDF.toString());
        metacard.setAttribute(Core.DATATYPE, "Document");

        return metacard;
    }

    private Metacard transformPdf(String id, PDDocument pdfDocument) throws IOException {
        MetacardImpl metacard = initializeMetacard(id);

        if (pdfDocument.isEncrypted()) {
            LOGGER.debug("Cannot transform encrypted pdf");
            return metacard;
        }

        extractPdfMetadata(pdfDocument, metacard);

        metacard.setThumbnail(thumbnailFunction.apply(pdfDocument));

        Optional.ofNullable(geoParserFunction.apply(pdfDocument))
                .ifPresent(metacard::setLocation);

        return metacard;
    }

    /**
     * @param pdfDocument PDF document
     * @param metacard    A mutable metacard to add the extracted data to
     */
    private void extractPdfMetadata(PDDocument pdfDocument, MetacardImpl metacard) {

        PDDocumentInformation documentInformation = pdfDocument.getDocumentInformation();

        Calendar creationDate = documentInformation.getCreationDate();
        if (creationDate != null) {
            metacard.setCreatedDate(creationDate.getTime());
            LOGGER.debug("PDF Creation date was: {}", DATE_FORMAT.format(creationDate));
        }

        Calendar modificationDate = documentInformation.getModificationDate();
        if (modificationDate != null) {
            metacard.setModifiedDate(modificationDate.getTime());
            LOGGER.debug("PDF Modification date was: {}", DATE_FORMAT.format(modificationDate));
        }

        if (usePdfTitleAsTitle) {
            String title = documentInformation.getTitle();
            if (StringUtils.isNotBlank(title)) {
                metacard.setTitle(title);
            }
        }

        String author = documentInformation.getAuthor();
        if (StringUtils.isNotBlank(author)) {
            metacard.setAttribute(Contact.CREATOR_NAME, author);
        }

        String subject = documentInformation.getSubject();
        if (StringUtils.isNotBlank(subject)) {
            metacard.setDescription(subject);
        }

        String keywords = documentInformation.getKeywords();
        if (StringUtils.isNotBlank(keywords)) {
            metacard.setAttribute(Topic.KEYWORD, keywords);
        }

    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }

}
