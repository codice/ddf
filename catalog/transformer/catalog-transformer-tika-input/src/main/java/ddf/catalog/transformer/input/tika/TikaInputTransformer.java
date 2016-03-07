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
package ddf.catalog.transformer.input.tika;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.imgscalr.Scalr;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.FileBackedOutputStream;
import com.sun.media.imageioimpl.plugins.jpeg2000.J2KImageReaderSpi;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.MetacardCreator;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;

public class TikaInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikaInputTransformer.class);

    private Templates templates = null;

    public TikaInputTransformer(BundleContext bundleContext) {
        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            templates =
                    TransformerFactory.newInstance(net.sf.saxon.TransformerFactoryImpl.class.getName(),
                            net.sf.saxon.TransformerFactoryImpl.class.getClassLoader())
                            .newTemplates(new StreamSource(TikaMetadataExtractor.class.getResourceAsStream(
                                    "/metadata.xslt")));
        } catch (TransformerConfigurationException e) {
            LOGGER.warn("Couldn't create XML transformer", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        if (bundleContext == null) {
            LOGGER.error("Bundle context is null. Unable to register {} as an osgi service.",
                    TikaInputTransformer.class.getSimpleName());
            return;
        }

        registerService(bundleContext);
        IIORegistry.getDefaultInstance()
                .registerServiceProvider(new J2KImageReaderSpi());
        IIORegistry.getDefaultInstance()
                .registerServiceProvider(new TIFFImageReaderSpi());
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        LOGGER.debug("Transforming input stream using Tika.");

        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        try (FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(1000000)) {
            try {
                IOUtils.copy(input, fileBackedOutputStream);
            } catch (IOException e) {
                throw new CatalogTransformerException("Could not copy bytes of content message.",
                        e);
            }

            Parser parser = new AutoDetectParser();
            ToXMLContentHandler handler = new ToXMLContentHandler();
            TikaMetadataExtractor tikaMetadataExtractor = new TikaMetadataExtractor(parser,
                    handler);

            Metadata metadata;
            try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource()
                    .openStream()) {
                metadata = tikaMetadataExtractor.parseMetadata(inputStreamCopy, new ParseContext());
            }

            String metadataText = handler.toString();
            if (templates != null) {
                metadataText = transformToXml(metadataText);
            }

            Metacard metacard = MetacardCreator.createBasicMetacard(metadata, id, metadataText);

            String metacardContentType = metacard.getContentTypeName();
            if (StringUtils.startsWith(metacardContentType, "image")) {
                try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource()
                        .openStream()) {
                    createThumbnail(inputStreamCopy, metacard);
                }
            }

            LOGGER.debug("Finished transforming input stream using Tika.");
            return metacard;
        }
    }

    /**
     * We programmatically register the Tika Input Transformer so we can programmatically build the
     * list of supported mime types.
     */
    private void registerService(BundleContext bundleContext) {
        LOGGER.debug("Registering {} as an osgi service.",
                TikaInputTransformer.class.getSimpleName());
        bundleContext.registerService(ddf.catalog.transform.InputTransformer.class,
                this,
                getServiceProperties());
    }

    private Hashtable<String, Object> getServiceProperties() {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(ddf.catalog.Constants.SERVICE_ID, "tika");
        properties.put(ddf.catalog.Constants.SERVICE_TITLE, "Tika Input Transformer");
        properties.put(ddf.catalog.Constants.SERVICE_DESCRIPTION,
                "The Tika Input Transformer detects and extracts metadata and text content from various documents.");
        properties.put("mime-type", getSupportedMimeTypes());
        // The Tika Input Transformer should be tried last, so we set the service ranking to -1
        properties.put(Constants.SERVICE_RANKING, -1);

        return properties;
    }

    private List<String> getSupportedMimeTypes() {
        SortedSet<MediaType> mediaTypes = MediaTypeRegistry.getDefaultRegistry()
                .getTypes();
        List<String> mimeTypes = new ArrayList<>(mediaTypes.size());

        for (MediaType mediaType : mediaTypes) {
            String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
            mimeTypes.add(mimeType);
        }
        mimeTypes.add("image/jp2");
        mimeTypes.add("image/bmp");

        LOGGER.debug("supported mime types: {}", mimeTypes);
        return mimeTypes;
    }

    private void createThumbnail(InputStream input, Metacard metacard) {
        try {
            Image image = ImageIO.read(new CloseShieldInputStream(input));

            if (null != image) {
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                        image.getHeight(null),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = bufferedImage.createGraphics();
                graphics.drawImage(image, null, null);
                graphics.dispose();

                BufferedImage thumb = Scalr.resize(bufferedImage, 200);

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    ImageIO.write(thumb, "jpeg", out);

                    byte[] thumbBytes = out.toByteArray();
                    metacard.setAttribute(new AttributeImpl(Metacard.THUMBNAIL, thumbBytes));
                }
            } else {
                LOGGER.warn("Unable to read image from input stream to create thumbnail.");
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to read image from input stream to create thumbnail.", e);
        }
    }

    private String transformToXml(String xhtml) {
        LOGGER.debug("Transforming xhtml to xml.");
        try {
            Writer xml = new StringWriter();
            Transformer transformer = templates.newTransformer();
            transformer.transform(new StreamSource(new StringReader(xhtml)), new StreamResult(xml));
            return xml.toString();
        } catch (TransformerException e) {
            LOGGER.warn("Unable to transform metadata from XHTML to XML.", e);
            return xhtml;
        }
    }
}
