/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.input.pdf;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.util.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.html.HtmlEscapers;
import com.google.common.net.MediaType;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class PdfInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfInputTransformer.class);

    private static final int RESOLUTION_DPI = 44;

    private static final float IMAGE_QUALITY = 1.0f;

    private static final float IMAGE_HEIGHTWIDTH = 128;

    private static final String FORMAT_NAME = "jpg";

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(
            "yyyy-MM-dd'T'HH:mm:ssZZ");

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        try (PDDocument pdfDocument = PDDocument.load(input)) {
            return transformPdf(id, pdfDocument);
        }
    }

    private Metacard transformPdf(String id, PDDocument pdfDocument) throws IOException {
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(id);
        metacard.setContentTypeName(MediaType.PDF.subtype());

        if (pdfDocument.isEncrypted()) {
            LOGGER.debug("Cannot transform encrypted pdf");
            return metacard;
        }

        extractPdfMetadata(pdfDocument, metacard);

        metacard.setThumbnail(generatePdfThumbnail(pdfDocument));
        return metacard;
    }

    /**
     * @param pdfDocument PDF document
     * @param metacard    A mutable metacard to add the extracted data to
     */
    private void extractPdfMetadata(PDDocument pdfDocument, MetacardImpl metacard) {
        StringBuilder metadataField = new StringBuilder("<pdf>");

        PDMetadata documentCatalogMetadata = pdfDocument.getDocumentCatalog()
                .getMetadata();
        if (documentCatalogMetadata != null) {
            try (InputStream inputStream = documentCatalogMetadata.createInputStream()) {
                metadataField.append(IOUtils.toString(inputStream));
            } catch (IOException e) {
                LOGGER.warn("Couldn't read the PDF document catalog's metadata", e);
            }
        }

        PDDocumentInformation documentInformation = pdfDocument.getDocumentInformation();

        try {
            Calendar creationDate = documentInformation.getCreationDate();
            if (creationDate != null) {
                metacard.setCreatedDate(creationDate.getTime());
                addXmlElement("creationDate", DATE_FORMAT.format(creationDate), metadataField);
            }
            LOGGER.info("PDF Creation date was: {}", creationDate);
        } catch (IOException e) {
            LOGGER.debug("Could not create date object", e);
        }

        try {
            Calendar modificationDate = documentInformation.getModificationDate();
            if (modificationDate != null) {
                metacard.setModifiedDate(modificationDate.getTime());
                addXmlElement("modificationDate",
                        DATE_FORMAT.format(modificationDate),
                        metadataField);
            }
            LOGGER.info("PDF Modification date was: {}", modificationDate);
        } catch (IOException e) {
            LOGGER.debug("Could not create date object", e);
        }

        String title = documentInformation.getTitle();
        if (StringUtils.isNotBlank(title)) {
            metacard.setTitle(title);
            addXmlElement("title", title, metadataField);
        }

        addXmlElement("author", documentInformation.getAuthor(), metadataField);
        addXmlElement("creator", documentInformation.getCreator(), metadataField);
        addXmlElement("keywords", documentInformation.getKeywords(), metadataField);
        addXmlElement("producer", documentInformation.getProducer(), metadataField);
        addXmlElement("subject", documentInformation.getSubject(), metadataField);
        addXmlElement("pageCount", String.valueOf(pdfDocument.getNumberOfPages()), metadataField);

        metadataField.append("</pdf>");
        metacard.setMetadata(metadataField.toString());
    }

    private void addXmlElement(String name, String value, StringBuilder metadata) {
        if (StringUtils.isNotBlank(value)) {
            metadata.append(String.format("<%s>%s</%s>",
                    name,
                    HtmlEscapers.htmlEscaper()
                            .escape(value),
                    name));
        }
    }

    public byte[] generatePdfThumbnail(InputStream pdfInputStream) throws IOException {
        PDDocument pdfDocument = PDDocument.load(pdfInputStream);
        if (pdfDocument.isEncrypted()) {
            throw new IOException("Unable to read encrypted document");
        }
        return generatePdfThumbnail(pdfDocument);
    }

    private byte[] generatePdfThumbnail(PDDocument pdfDocument) throws IOException {
        PDPage page = (PDPage) pdfDocument.getDocumentCatalog()
                .getAllPages()
                .get(0);

        BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, RESOLUTION_DPI);
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
}
