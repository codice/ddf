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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.FileBackedOutputStream;
import com.google.common.net.MediaType;

import ddf.catalog.content.operation.ContentMetadataExtractor;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import ddf.catalog.util.impl.ServiceComparator;

public class PdfInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfInputTransformer.class);

    private static final int RESOLUTION_DPI = 44;

    private static final float IMAGE_QUALITY = 1.0f;

    private static final float IMAGE_HEIGHTWIDTH = 128;

    private static final String FORMAT_NAME = "jpg";

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(
            "yyyy-MM-dd'T'HH:mm:ssZZ");

    private Map<ServiceReference, ContentMetadataExtractor> contentMetadataExtractors =
            Collections.synchronizedMap(new TreeMap<>(new ServiceComparator()));

    public void addContentMetadataExtractors(
            ServiceReference<ContentMetadataExtractor> contentMetadataExtractorRef) {
        Bundle bundle = getBundle();
        if (bundle != null) {
            ContentMetadataExtractor cme = bundle.getBundleContext()
                    .getService(contentMetadataExtractorRef);
            contentMetadataExtractors.put(contentMetadataExtractorRef, cme);
        }
    }

    Bundle getBundle() {
        return FrameworkUtil.getBundle(PdfInputTransformer.class);
    }

    public void removeContentMetadataExtractor(
            ServiceReference<ContentMetadataExtractor> contentMetadataExtractorRef) {
        contentMetadataExtractors.remove(contentMetadataExtractorRef);
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        if (contentMetadataExtractors.isEmpty()) {
            return transformWithoutExtractors(input, id);
        } else {
            return transformWithExtractors(input, id);
        }
    }

    private Metacard transformWithoutExtractors(InputStream input, String id) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(input)) {
            return transformPdf(id, pdfDocument);
        } catch (InvalidPasswordException e) {
            LOGGER.warn("Cannot transform encrypted pdf", e);
            return initializeMetacard(id);
        }
    }

    private Metacard transformWithExtractors(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(1000000)) {
            try {
                IOUtils.copy(input, fbos);
            } catch (IOException e) {
                throw new CatalogTransformerException("Could not copy bytes of content message.",
                        e);
            }

            String plainText = null;
            try (InputStream isCopy = fbos.asByteSource()
                    .openStream()) {
                Parser parser = new AutoDetectParser();
                ContentHandler contentHandler = new ToTextContentHandler();
                TikaMetadataExtractor tikaMetadataExtractor = new TikaMetadataExtractor(parser,
                        contentHandler);
                tikaMetadataExtractor.parseMetadata(isCopy, new ParseContext());
                plainText = contentHandler.toString();
            } catch (CatalogTransformerException e) {
                LOGGER.warn("Cannot extract metadata from pdf", e);
            }

            try (InputStream isCopy = fbos.asByteSource()
                    .openStream();
                    PDDocument pdfDocument = PDDocument.load(isCopy)) {

                return transformPdf(id, pdfDocument, plainText);
            } catch (InvalidPasswordException e) {
                LOGGER.warn("Cannot transform encrypted pdf", e);
                return initializeMetacard(id);
            }
        }
    }

    private MetacardImpl initializeMetacard(String id) {
        return initializeMetacard(id, null);
    }

    private MetacardImpl initializeMetacard(String id, String contentInput) {
        MetacardImpl metacard;

        if (StringUtils.isNotBlank(contentInput)) {
            Set<AttributeDescriptor> attributes = contentMetadataExtractors.values()
                    .stream()
                    .map(ContentMetadataExtractor::getMetacardAttributes)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            // TODO RAP 14 Jun 16: Might need to add service method to extract
            // name/id type info from the extractor. Those would then be
            // concatted together to form a name for the metacardtype.
            String typeName = contentMetadataExtractors.values()
                    .stream()
                    .map(v -> v.getClass()
                            .getName())
                    .collect(Collectors.joining("_"));

            metacard = new MetacardImpl(new MetacardTypeImpl(typeName,
                    Sets.union(BasicTypes.BASIC_METACARD.getAttributeDescriptors(), attributes)));
            for (ContentMetadataExtractor contentMetadataExtractor : contentMetadataExtractors.values()) {
                contentMetadataExtractor.process(contentInput, metacard);
            }
        } else {
            metacard = new MetacardImpl();
        }

        metacard.setId(id);
        metacard.setContentTypeName(MediaType.PDF.subtype());

        return metacard;
    }

    private Metacard transformPdf(String id, PDDocument pdfDocument) throws IOException {
        return transformPdf(id, pdfDocument, null);
    }

    private Metacard transformPdf(String id, PDDocument pdfDocument, String contentInput)
            throws IOException {
        MetacardImpl metacard = initializeMetacard(id, contentInput);

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

        Calendar creationDate = documentInformation.getCreationDate();
        if (creationDate != null) {
            metacard.setCreatedDate(creationDate.getTime());
            addXmlElement("creationDate", DATE_FORMAT.format(creationDate), metadataField);
            LOGGER.debug("PDF Creation date was: {}", DATE_FORMAT.format(creationDate));
        }

        Calendar modificationDate = documentInformation.getModificationDate();
        if (modificationDate != null) {
            metacard.setModifiedDate(modificationDate.getTime());
            addXmlElement("modificationDate", DATE_FORMAT.format(modificationDate), metadataField);
            LOGGER.debug("PDF Modification date was: {}", DATE_FORMAT.format(modificationDate));
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
            metadata.append(String.format("<%s>%s</%s>", name, HtmlEscapers.htmlEscaper()
                    .escape(value), name));
        }
    }

    private byte[] generatePdfThumbnail(PDDocument pdfDocument) throws IOException {

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

        PDPage page = pdfDocument.getPage(0);

        BufferedImage image = pdfRenderer.renderImageWithDPI(0, RESOLUTION_DPI, ImageType.RGB);

        int largestDimension = Math.max(image.getHeight(), image.getWidth());
        float scalingFactor = IMAGE_HEIGHTWIDTH / largestDimension;
        int scaledHeight = (int) (image.getHeight() * scalingFactor);
        int scaledWidth = (int) (image.getWidth() * scalingFactor);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIOUtil.writeImage(scaledImage, FORMAT_NAME, outputStream, RESOLUTION_DPI,
                    IMAGE_QUALITY);
            return outputStream.toByteArray();
        }
    }

}
