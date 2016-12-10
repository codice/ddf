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

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
<<<<<<< HEAD
=======
import java.util.Optional;
>>>>>>> master
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
<<<<<<< HEAD
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
=======
>>>>>>> master
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;
<<<<<<< HEAD
=======
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
>>>>>>> master
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

<<<<<<< HEAD
import com.google.common.collect.Sets;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.FileBackedOutputStream;
=======
>>>>>>> master
import com.google.common.net.MediaType;

import ddf.catalog.content.operation.ContentMetadataExtractor;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
<<<<<<< HEAD
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
=======
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
>>>>>>> master
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import ddf.catalog.util.impl.ServiceComparator;

public class PdfInputTransformer implements InputTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfInputTransformer.class);

    private final PDDocumentGenerator pdDocumentGenerator;

    private final GeoPdfParser geoParser;

    private final PdfThumbnailGenerator pdfThumbnailGenerator;

    private Map<ServiceReference, ContentMetadataExtractor> contentMetadataExtractors =
            Collections.synchronizedMap(new TreeMap<>(new ServiceComparator()));

    private MetacardType metacardType;

    private boolean usePdfTitleAsTitle;

    /**
     * @param metacardType          must be non-null
     * @param usePdfTitleAsTitle    must be non-null
     * @param pdDocumentGenerator   must be non-null
     * @param geoParser             must be non-null
     * @param pdfThumbnailGenerator must be non-null
     */
    public PdfInputTransformer(MetacardType metacardType, Boolean usePdfTitleAsTitle,
            PDDocumentGenerator pdDocumentGenerator, GeoPdfParser geoParser,
            PdfThumbnailGenerator pdfThumbnailGenerator) {
        notNull(metacardType, "metacardType must be non-null");
        notNull(usePdfTitleAsTitle, "usePdfTitleAsTitle must be non-null");
        notNull(pdDocumentGenerator, "pdDocumentGenerator must be non-null");
        notNull(geoParser, "geoParser must be non-null");
        notNull(pdfThumbnailGenerator, "pdfThumbnailGenerator must be non-null");

        this.metacardType = metacardType;
        this.usePdfTitleAsTitle = usePdfTitleAsTitle;
        this.pdDocumentGenerator = pdDocumentGenerator;
        this.geoParser = geoParser;
        this.pdfThumbnailGenerator = pdfThumbnailGenerator;
    }

    @SuppressWarnings("unused")
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
<<<<<<< HEAD
        try (PDDocument pdfDocument = PDDocument.load(input)) {
=======
        try (PDDocument pdfDocument = pdDocumentGenerator.apply(input)) {
>>>>>>> master
            return transformPdf(id, pdfDocument);
        } catch (InvalidPasswordException e) {
            LOGGER.debug("Cannot transform encrypted pdf", e);
            return initializeMetacard(id);
        }
    }

    private Metacard transformWithExtractors(InputStream input, String id)
            throws IOException, CatalogTransformerException {
<<<<<<< HEAD
        try (FileBackedOutputStream fbos = new FileBackedOutputStream(1000000)) {
=======
        try (TemporaryFileBackedOutputStream fbos = new TemporaryFileBackedOutputStream()) {
>>>>>>> master
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
<<<<<<< HEAD
                    .openStream();
                    PDDocument pdfDocument = PDDocument.load(isCopy)) {

                return transformPdf(id, pdfDocument, plainText);
            } catch (InvalidPasswordException e) {
                LOGGER.warn("Cannot transform encrypted pdf", e);
=======
                    .openStream(); PDDocument pdfDocument = pdDocumentGenerator.apply(isCopy)) {

                return transformPdf(id, pdfDocument, plainText);
            } catch (InvalidPasswordException e) {
                LOGGER.debug("Cannot transform encrypted pdf", e);
>>>>>>> master
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

<<<<<<< HEAD
            metacard = new MetacardImpl(new MetacardTypeImpl(BasicTypes.BASIC_METACARD.getName(),
                    Sets.union(BasicTypes.BASIC_METACARD.getAttributeDescriptors(), attributes)));
=======
            metacard = new MetacardImpl(new MetacardTypeImpl(metacardType.getName(),
                    metacardType,
                    attributes));

>>>>>>> master
            for (ContentMetadataExtractor contentMetadataExtractor : contentMetadataExtractors.values()) {
                contentMetadataExtractor.process(contentInput, metacard);
            }
        } else {
<<<<<<< HEAD
            metacard = new MetacardImpl();
=======
            metacard = new MetacardImpl(metacardType);
>>>>>>> master
        }

        metacard.setId(id);
        metacard.setContentTypeName(MediaType.PDF.toString());
        metacard.setAttribute(Media.TYPE, MediaType.PDF.toString());
        metacard.setAttribute(Core.DATATYPE, "Document");

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

        pdfThumbnailGenerator.apply(pdfDocument)
                .ifPresent(metacard::setThumbnail);

        Optional.ofNullable(geoParser.apply(pdfDocument))
                .ifPresent(metacard::setLocation);

        return metacard;
    }

    /**
     * @param pdfDocument PDF document
     * @param metacard    A mutable metacard to add the extracted data to
     */
    private void extractPdfMetadata(PDDocument pdfDocument, MetacardImpl metacard) {

        PDDocumentInformation documentInformation = pdfDocument.getDocumentInformation();

<<<<<<< HEAD
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
=======
        setDateIfNotNull(documentInformation.getCreationDate(), metacard, Metacard.CREATED);

        setDateIfNotNull(documentInformation.getModificationDate(), metacard, Metacard.MODIFIED);
>>>>>>> master

        if (usePdfTitleAsTitle) {
            setIfNotBlank(documentInformation.getTitle(), metacard, Metacard.TITLE);
        }

        setIfNotBlank(documentInformation.getAuthor(), metacard, Contact.CREATOR_NAME);

        setIfNotBlank(documentInformation.getSubject(), metacard, Metacard.DESCRIPTION);

        setIfNotBlank(documentInformation.getKeywords(), metacard, Topic.KEYWORD);

    }

    private void setDateIfNotNull(Calendar calendar, MetacardImpl metacard, String attributeName) {
        if (calendar != null) {
            metacard.setAttribute(attributeName, calendar.getTime());
        }
    }

    private void setIfNotBlank(String pdfDocumentValue, MetacardImpl metacard,
            String attributeName) {
        if (StringUtils.isNotBlank(pdfDocumentValue)) {
            metacard.setAttribute(attributeName, pdfDocumentValue);
        }
    }

}
