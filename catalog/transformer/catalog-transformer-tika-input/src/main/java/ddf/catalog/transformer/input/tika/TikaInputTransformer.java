/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.input.tika;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi;
import com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi;
import ddf.catalog.content.operation.ContentMetadataExtractor;
import ddf.catalog.content.operation.MetadataExtractor;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.experimental.ExtractedAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Validation;
import ddf.catalog.data.types.constants.core.DataType;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.MetacardCreator;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import ddf.catalog.util.impl.ServiceComparator;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.XMLUtils;
import org.imgscalr.Scalr;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikaInputTransformer implements InputTransformer {

  private int previewMaxLength = 30000;

  private int metadataMaxLength = 30000;

  private static final Logger LOGGER = LoggerFactory.getLogger(TikaInputTransformer.class);

  private static final Map<com.google.common.net.MediaType, String>
      SPECIFIC_MIME_TYPE_DATA_TYPE_MAP;

  private static final Map<com.google.common.net.MediaType, String>
      FALLBACK_MIME_TYPE_DATA_TYPE_MAP;

  private static final String OVERALL_FALLBACK_DATA_TYPE = DataType.DATASET.toString();

  static {
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP = new HashMap<>();
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.MICROSOFT_EXCEL, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.MICROSOFT_POWERPOINT, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.MICROSOFT_WORD, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.OPENDOCUMENT_GRAPHICS, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.OPENDOCUMENT_PRESENTATION, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.OPENDOCUMENT_SPREADSHEET, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.OPENDOCUMENT_TEXT, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.APPLICATION_BINARY, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.PDF, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.APPLICATION_XML_UTF_8, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.JSON_UTF_8, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.KML, DataType.TEXT.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.ZIP, DataType.COLLECTION.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.TAR, DataType.COLLECTION.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.GZIP, DataType.COLLECTION.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.BZIP2, DataType.COLLECTION.toString());
    SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.OCTET_STREAM, OVERALL_FALLBACK_DATA_TYPE);

    FALLBACK_MIME_TYPE_DATA_TYPE_MAP = new HashMap<>();
    FALLBACK_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.ANY_APPLICATION_TYPE, DataType.TEXT.toString());
    FALLBACK_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.ANY_IMAGE_TYPE, DataType.IMAGE.toString());
    FALLBACK_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.ANY_TEXT_TYPE, DataType.TEXT.toString());
    FALLBACK_MIME_TYPE_DATA_TYPE_MAP.put(
        com.google.common.net.MediaType.ANY_AUDIO_TYPE, DataType.SOUND.toString());
  }

  private Templates templates = null;

  private Map<ServiceReference, ContentMetadataExtractor> contentExtractors =
      Collections.synchronizedMap(new TreeMap<>(new ServiceComparator()));

  private Map<ServiceReference, MetadataExtractor> metadataExtractors =
      Collections.synchronizedMap(new TreeMap<>(new ServiceComparator()));

  private MetacardType fallbackExcelMetacardType = null;

  private MetacardType fallbackJpegMetacardType = null;

  private MetacardType fallbackMp4MetacardType = null;

  private MetacardType fallbackMpegMetacardType = null;

  private MetacardType fallbackOfficeDocMetacardType = null;

  private MetacardType fallbackPdfMetacardType = null;

  private MetacardType fallbackPowerpointMetacardType = null;

  // commonTikaMetacardType represents the MetacardType to be used when an ingested product's mime
  // type does not match a mime type that is supported by the mimeTypeToMetacardTypeMap
  private MetacardType commonTikaMetacardType = null;

  private Map<String, MetacardType> mimeTypeToMetacardTypeMap = new HashMap<>();

  private boolean useResourceTitleAsTitle;

  public TikaInputTransformer(BundleContext bundleContext, MetacardType metacardType) {
    this.commonTikaMetacardType = metacardType;
    classLoaderAndBundleContextSetup(bundleContext);
  }

  public int getPreviewMaxLength() {
    return previewMaxLength;
  }

  public void setPreviewMaxLength(int previewMaxLength) {
    this.previewMaxLength = previewMaxLength;
  }

  public void setMetadataMaxLength(int metadataMaxLength) {
    this.metadataMaxLength = metadataMaxLength;
  }

  @SuppressWarnings("unused")
  public void setCommonTikaMetacardType(MetacardType metacardType) {
    this.commonTikaMetacardType = metacardType;
  }

  public void setFallbackExcelMetacardType(MetacardType metacardType) {
    this.fallbackExcelMetacardType = metacardType;
  }

  public void setFallbackJpegMetacardType(MetacardType metacardType) {
    this.fallbackJpegMetacardType = metacardType;
  }

  public void setFallbackMp4MetacardType(MetacardType metacardType) {
    this.fallbackMp4MetacardType = metacardType;
  }

  public void setFallbackMpegMetacardType(MetacardType metacardType) {
    this.fallbackMpegMetacardType = metacardType;
  }

  public void setFallbackOfficeDocMetacardType(MetacardType metacardType) {
    this.fallbackOfficeDocMetacardType = metacardType;
  }

  public void setFallbackPdfMetacardType(MetacardType metacardType) {
    this.fallbackPdfMetacardType = metacardType;
  }

  public void setFallbackPowerpointMetacardType(MetacardType metacardType) {
    this.fallbackPowerpointMetacardType = metacardType;
  }

  /**
   * Populates the mimeTypeToMetacardMap for use in determining the {@link MetacardType} that
   * corresponds to an ingested product's mimeType.
   */
  public void populateMimeTypeMap() {
    // .pptm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-powerpoint.presentation.macroenabled.12",
        fallbackPowerpointMetacardType);
    // .ppt, .ppz, .pps, .pot, .ppa
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MICROSOFT_POWERPOINT.toString(),
        fallbackPowerpointMetacardType);
    // .pptx, .thmx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        fallbackPowerpointMetacardType);
    // .ppsx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        fallbackPowerpointMetacardType);
    // .potx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.presentationml.template",
        fallbackPowerpointMetacardType);
    // .ppam
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-powerpoint.addin.macroenabled.12", fallbackPowerpointMetacardType);
    // .ppsm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-powerpoint.slideshow.macroenabled.12", fallbackPowerpointMetacardType);
    // .sldm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-powerpoint.slide.macroenabled.12", fallbackPowerpointMetacardType);
    // .potm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-powerpoint.template.macroenabled.12", fallbackPowerpointMetacardType);
    // .doc, .dot
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MICROSOFT_WORD.toString(), fallbackOfficeDocMetacardType);
    // .docx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        fallbackOfficeDocMetacardType);
    // .doc, .dot, allias for "application/msword"
    mimeTypeToMetacardTypeMap.put("application/vnd.ms-word", fallbackOfficeDocMetacardType);
    // .docm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-word.document.macroenabled.12", fallbackOfficeDocMetacardType);
    // .dotm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-word.template.macroenabled.12", fallbackOfficeDocMetacardType);
    // .dotx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
        fallbackOfficeDocMetacardType);
    // .pdf
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.PDF.toString(), fallbackPdfMetacardType);
    // .mpeg, .mpg, .mpe, .m1v, .m2v
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MPEG_VIDEO.toString(), fallbackMpegMetacardType);
    // .mpga, .mp2, .mp2a, .mp3, .m2a, .m3a
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MPEG_AUDIO.toString(), fallbackMpegMetacardType);
    // .mp4 is defined for mpeg-4 content but is not directly correlated with this mime type
    mimeTypeToMetacardTypeMap.put("audio/mpeg4-generic", fallbackMpegMetacardType);
    // .mp4 is defined for mpeg-4 content but is not directly correlated with this mime type
    mimeTypeToMetacardTypeMap.put("video/mpeg4-generic", fallbackMpegMetacardType);
    // .mp4s
    mimeTypeToMetacardTypeMap.put("application/mp4", fallbackMp4MetacardType);
    // .mp4a, .m4a,.m4b
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MP4_AUDIO.toString(), fallbackMp4MetacardType);
    // .mp4, .mp4v, .mpg4
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MP4_VIDEO.toString(), fallbackMp4MetacardType);
    // .jpg, .jpeg, .jpe, .jif, .jfif, .jfi
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.JPEG.toString(), fallbackJpegMetacardType);
    // .jpgv is defined for jpeg content but is not directly correlated with this mime type
    mimeTypeToMetacardTypeMap.put("video/jpeg", fallbackJpegMetacardType);
    // .jpgv is defined for jpeg2000 content but is not directly correlated with this mime type
    mimeTypeToMetacardTypeMap.put("video/jpeg2000", fallbackJpegMetacardType);
    // .xls, .xlm, .xla, .xlc, .xlt, .xlw, .xll, .xld
    mimeTypeToMetacardTypeMap.put(
        com.google.common.net.MediaType.MICROSOFT_EXCEL.toString(), fallbackExcelMetacardType);
    // .xlsx
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        fallbackExcelMetacardType);
    // .xlsm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-excel.sheet.macroenabled.12", fallbackExcelMetacardType);
    // .xlsb
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-excel.sheet.binary.macroenabled.12", fallbackExcelMetacardType);
    // .xlam
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-excel.addin.macroenabled.12", fallbackExcelMetacardType);
    // .xltm
    mimeTypeToMetacardTypeMap.put(
        "application/vnd.ms-excel.template.macroenabled.12", fallbackExcelMetacardType);
  }

  /**
   * Determines which {@link MetacardType} should be used to create a metacard for an input file of
   * a given mime type
   *
   * @param mimeType the String representing the mime type of the file
   * @return a {@link Optional} of {@link MetacardType} that should be used to create a {@link
   *     Metacard} for the given mimeType. Returns empty {@link Optional} if no {@link MetacardType}
   *     matched the given mime type.
   */
  public Optional<MetacardType> getMetacardTypeFromMimeType(String mimeType) {
    return Optional.ofNullable(mimeTypeToMetacardTypeMap.get(mimeType));
  }

  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {
    LOGGER.debug("Transforming input stream using Tika.");
    long bytes;
    if (input == null) {
      throw new CatalogTransformerException("Cannot transform null input.");
    }

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {
      try {
        bytes = IOUtils.copyLarge(input, fileBackedOutputStream);
      } catch (IOException e) {
        throw new CatalogTransformerException("Could not copy bytes of content message.", e);
      }

      Metadata metadata;
      String bodyText = null;
      String metadataText;
      Metacard metacard = new MetacardImpl(commonTikaMetacardType);
      String contentType = DataType.DATASET.name();
      TikaMetadataExtractor extractor = null;
      try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource().openStream()) {
        extractor = new TikaMetadataExtractor(inputStreamCopy, previewMaxLength, metadataMaxLength);

      } catch (TikaException | RuntimeException t) {
        LOGGER.debug("Unable to extract tika metadata", t);
      }

      if (extractor != null) {
        metadataText = extractor.getMetadataXml();
        Attribute validationAttribute = null;
        if (metadataText.equals(TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG)) {
          validationAttribute =
              new AttributeImpl(
                  Validation.VALIDATION_WARNINGS, Collections.singletonList(metadataText));
          metadataText = "";
        }
        bodyText = extractor.getBodyText();
        metadata = extractor.getMetadata();
        contentType = metadata.get(Metadata.CONTENT_TYPE);
        MetacardType metacardType = mergeAttributes(getMetacardType(contentType));
        metacard =
            MetacardCreator.createMetacard(
                metadata, id, metadataText, metacardType, useResourceTitleAsTitle);
        if (StringUtils.isNotBlank(bodyText)) {
          metacard.setAttribute(new AttributeImpl(Extracted.EXTRACTED_TEXT, bodyText));
          processContentMetadataExtractors(bodyText, metacard);
        }

        if (StringUtils.isNotBlank(metadataText)) {
          processMetadataExtractors(metadataText, metacard);
        }

        if (validationAttribute != null) {
          metacard.setAttribute(validationAttribute);
        }
      }

      enrichMetacard(fileBackedOutputStream, contentType, bytes, metacard);

      LOGGER.debug("Finished transforming input stream using Tika.");
      return metacard;
    }
  }

  private void processMetadataExtractors(String metadataText, Metacard metacard) {
    for (MetadataExtractor metadataExtractor : metadataExtractors.values()) {
      metadataExtractor.process(metadataText, metacard);
    }
  }

  private void processContentMetadataExtractors(String bodyText, Metacard metacard) {
    if (!contentExtractors.isEmpty()) {
      for (ContentMetadataExtractor contentMetadataExtractor : contentExtractors.values()) {
        contentMetadataExtractor.process(bodyText, metacard);
      }
    }
  }

  public void addContentMetadataExtractor(
      ServiceReference<ContentMetadataExtractor> contentMetadataExtractorRef) {
    Bundle bundle = getBundle();
    if (bundle != null) {
      ContentMetadataExtractor cme =
          bundle.getBundleContext().getService(contentMetadataExtractorRef);
      contentExtractors.put(contentMetadataExtractorRef, cme);
    }
  }

  public void addMetadataExtractor(ServiceReference<MetadataExtractor> metadataExtractorRef) {
    Bundle bundle = getBundle();
    if (bundle != null) {
      MetadataExtractor cme = bundle.getBundleContext().getService(metadataExtractorRef);
      metadataExtractors.put(metadataExtractorRef, cme);
    }
  }

  public void removeContentMetadataExtractor(
      ServiceReference<ContentMetadataExtractor> contentMetadataExtractorRef) {
    contentExtractors.remove(contentMetadataExtractorRef);
  }

  public void removeMetadataExtractor(ServiceReference<MetadataExtractor> metadataExtractorRef) {
    metadataExtractors.remove(metadataExtractorRef);
  }

  /** @param useResourceTitleAsTitle must be non-null */
  public void setUseResourceTitleAsTitle(Boolean useResourceTitleAsTitle) {
    Validate.notNull(useResourceTitleAsTitle, "useResourceTitleAsTitle must be non-null");
    this.useResourceTitleAsTitle = useResourceTitleAsTitle;
  }

  private void classLoaderAndBundleContextSetup(BundleContext bundleContext) {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    try (InputStream stream = TikaMetadataExtractor.class.getResourceAsStream("/metadata.xslt")) {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      templates =
          XMLUtils.getInstance()
              .getSecureXmlTransformerFactory(
                  net.sf.saxon.TransformerFactoryImpl.class.getName(),
                  net.sf.saxon.TransformerFactoryImpl.class.getClassLoader())
              .newTemplates(new StreamSource(stream));
    } catch (TransformerConfigurationException e) {
      LOGGER.debug("Couldn't create XML transformer", e);
    } catch (IOException e) {
      LOGGER.debug("Could not get Tiki metadata XSLT", e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }

    if (bundleContext == null) {
      LOGGER.info(
          "Bundle context is null. Unable to register {} as an osgi service.",
          TikaInputTransformer.class.getSimpleName());
      return;
    }

    registerService(bundleContext);
    IIORegistry.getDefaultInstance().registerServiceProvider(new J2KImageReaderSpi());
    IIORegistry.getDefaultInstance().registerServiceProvider(new TIFFImageReaderSpi());
  }

  private MetacardType getMetacardType(String contentType) {
    return metadataExtractors.values().stream()
        .filter(e -> e.canProcess(contentType))
        .findFirst()
        .map(e -> e.getMetacardType(contentType))
        .orElse(getMetacardTypeFromMimeType(contentType).orElse(commonTikaMetacardType));
  }

  protected MetacardType mergeAttributes(MetacardType metacardType) {
    Set<AttributeDescriptor> additionalAttributes =
        contentExtractors.values().stream()
            .map(ContentMetadataExtractor::getMetacardAttributes)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    // Always add extracted attribute descriptors since the given type might not have them
    // defined. If they are not defined the attributes like extracted text will not be saved.
    additionalAttributes.addAll(new ExtractedAttributes().getAttributeDescriptors());

    return new MetacardTypeImpl(metacardType.getName(), metacardType, additionalAttributes);
  }

  protected void enrichMetacard(
      TemporaryFileBackedOutputStream fileBackedOutputStream,
      String metacardContentType,
      long bytes,
      Metacard metacard)
      throws IOException {

    if (StringUtils.isNotBlank(metacardContentType)) {
      metacard.setAttribute(new AttributeImpl(Core.DATATYPE, getDatatype(metacardContentType)));
    }

    if (StringUtils.startsWith(metacardContentType, "image")) {
      try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource().openStream()) {
        createThumbnail(inputStreamCopy, metacard);
      }
    }

    metacard.setAttribute(new AttributeImpl(Core.RESOURCE_SIZE, String.valueOf(bytes)));
  }

  @Nullable
  private String getDatatype(String mimeType) {
    if (mimeType == null) {
      return null;
    }

    if (mimeType.matches(DataType.DATASET.name())) {
      return OVERALL_FALLBACK_DATA_TYPE;
    }

    com.google.common.net.MediaType mediaType = com.google.common.net.MediaType.parse(mimeType);

    LOGGER.debug("Attempting to map {}", mimeType);
    Optional<Map.Entry<com.google.common.net.MediaType, String>> returnType =
        SPECIFIC_MIME_TYPE_DATA_TYPE_MAP.entrySet().stream()
            .filter(mediaTypeStringEntry -> mediaType.is(mediaTypeStringEntry.getKey()))
            .findFirst();

    if (!returnType.isPresent()) {
      Optional<Map.Entry<com.google.common.net.MediaType, String>> fallback =
          FALLBACK_MIME_TYPE_DATA_TYPE_MAP.entrySet().stream()
              .filter(mediaTypeStringEntry -> mediaType.is(mediaTypeStringEntry.getKey()))
              .findFirst();

      return fallback.map(Map.Entry::getValue).orElse(OVERALL_FALLBACK_DATA_TYPE);
    }

    return returnType.get().getValue();
  }

  /**
   * We programmatically register the Tika Input Transformer so we can programmatically build the
   * list of supported mime types.
   */
  private void registerService(BundleContext bundleContext) {
    LOGGER.debug("Registering {} as an osgi service.", TikaInputTransformer.class.getSimpleName());
    bundleContext.registerService(InputTransformer.class, this, getServiceProperties());
  }

  private Dictionary<String, Object> getServiceProperties() {
    Dictionary<String, Object> properties = new DictionaryMap<>();
    properties.put(ddf.catalog.Constants.SERVICE_ID, "tika");
    properties.put(ddf.catalog.Constants.SERVICE_TITLE, "Tika Input Transformer");
    properties.put(
        ddf.catalog.Constants.SERVICE_DESCRIPTION,
        "The Tika Input Transformer detects and extracts metadata and text content from various documents.");
    properties.put("mime-type", getSupportedMimeTypes());
    // The Tika Input Transformer should be tried last, so we set the service ranking to -1
    properties.put(Constants.SERVICE_RANKING, -1);

    return properties;
  }

  private List<String> getSupportedMimeTypes() {
    MediaTypeRegistry mediaTypeRegistry = MediaTypeRegistry.getDefaultRegistry();

    Set<MediaType> mediaTypes = mediaTypeRegistry.getTypes();
    Set<MediaType> mediaTypeAliases = new HashSet<>();
    List<String> mimeTypes = new ArrayList<>(mediaTypes.size());

    for (MediaType mediaType : mediaTypes) {
      addMediaTypetoMimeTypes(mediaType, mimeTypes);
      mediaTypeAliases.addAll(mediaTypeRegistry.getAliases(mediaType));
    }

    for (MediaType mediaType : mediaTypeAliases) {
      addMediaTypetoMimeTypes(mediaType, mimeTypes);
    }

    mimeTypes.add("image/jp2");
    mimeTypes.add("application/vnd.ms-visio.viewer");

    LOGGER.debug("supported mime types: {}", mimeTypes);
    return mimeTypes;
  }

  private void addMediaTypetoMimeTypes(MediaType mediaType, List<String> mimeTypes) {
    String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
    mimeTypes.add(mimeType);
  }

  private void createThumbnail(InputStream input, Metacard metacard) {
    try {
      Image image = ImageIO.read(new CloseShieldInputStream(input));

      if (null != image) {
        BufferedImage bufferedImage =
            new BufferedImage(
                image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
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
        LOGGER.debug("Unable to read image from input stream to create thumbnail.");
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to read image from input stream to create thumbnail.", e);
    }
  }

  Bundle getBundle() {
    return FrameworkUtil.getBundle(TikaInputTransformer.class);
  }
}
