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

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.content.operation.ContentMetadataExtractor;
import ddf.catalog.content.operation.MetadataExtractor;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Validation;
import ddf.catalog.data.types.constants.core.DataType;
import ddf.catalog.data.types.experimental.Extracted;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikaInputTransformerTest {

  static final String TEST_NAME = "TEST_NAME";

  static final String PDF_CONTENT_TYPE = "application/pdf";

  private static final Logger LOGGER = LoggerFactory.getLogger(TikaInputTransformerTest.class);

  private static final String COMMON_METACARDTYPE_NAME = "fallback.common";

  private static final String SOUND = DataType.SOUND.toString();

  private static final String IMAGE = DataType.IMAGE.toString();

  private static final String TEXT = DataType.TEXT.toString();

  private static final String DATASET = DataType.DATASET.toString();

  private static final String COLLECTION = DataType.COLLECTION.toString();

  private static final String EXCEL_METACARDTYPE_NAME = "fallback.excel";

  private static final String JPEG_METACARDTYPE_NAME = "fallback.jpeg";

  private static final String MP4_METACARDTYPE_NAME = "fallback.mp4";

  private static final String MPEG_METACARDTYPE_NAME = "fallback.mpeg";

  private static final String OFFICEDOC_METACARDTYPE_NAME = "fallback.doc";

  private static final String PDF_METACARDTYPE_NAME = "fallback.pdf";

  private static final String POWERPOINT_METACARDTYPE_NAME = "fallback.powerpoint";

  ImmutableSet<AttributeDescriptor> attributeDescriptors =
      ImmutableSet.of(
          new AttributeDescriptorImpl("attr1", false, false, false, false, BasicTypes.OBJECT_TYPE),
          new AttributeDescriptorImpl("attr2", false, false, false, false, BasicTypes.OBJECT_TYPE));

  private Bundle bundleMock = mock(Bundle.class);

  private BundleContext bundleCtx = mock(BundleContext.class);

  private ServiceReference serviceRefCme = mock(ServiceReference.class);

  private ServiceReference serviceRefMe = mock(ServiceReference.class);

  private ContentMetadataExtractor cme = mock(ContentMetadataExtractor.class);

  private MetadataExtractor me = mock(MetadataExtractor.class);

  private MetacardType mockMetacardType = mock(MetacardType.class);

  private TikaInputTransformer tikaInputTransformer;

  private static MetacardType getMetacardType(String typeName) {
    return new MetacardTypeImpl(
        typeName,
        Arrays.asList(
            new ValidationAttributes(),
            new ContactAttributes(),
            new LocationAttributes(),
            new MediaAttributes(),
            new AssociationsAttributes()));
  }

  @Before
  public void setup() {
    tikaInputTransformer =
        new TikaInputTransformer(bundleCtx, getMetacardType(COMMON_METACARDTYPE_NAME)) {
          @Override
          Bundle getBundle() {
            return bundleMock;
          }
        };

    when(me.canProcess(PDF_CONTENT_TYPE)).thenReturn(true);
    when(me.getMetacardType(PDF_CONTENT_TYPE)).thenReturn(mockMetacardType);
    when(mockMetacardType.getName()).thenReturn(TEST_NAME);

    when(bundleMock.getBundleContext()).thenReturn(bundleCtx);
    when(bundleCtx.getService(eq(serviceRefCme))).thenReturn(cme);
    when(bundleCtx.getService(eq(serviceRefMe))).thenReturn(me);

    when(cme.getMetacardAttributes()).thenReturn(attributeDescriptors);

    tikaInputTransformer.setFallbackExcelMetacardType(getMetacardType(EXCEL_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackJpegMetacardType(getMetacardType(JPEG_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackMp4MetacardType(getMetacardType(MP4_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackMpegMetacardType(getMetacardType(MPEG_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackOfficeDocMetacardType(
        getMetacardType(OFFICEDOC_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackPdfMetacardType(getMetacardType(PDF_METACARDTYPE_NAME));
    tikaInputTransformer.setFallbackPowerpointMetacardType(
        getMetacardType(POWERPOINT_METACARDTYPE_NAME));
    tikaInputTransformer.populateMimeTypeMap();
    tikaInputTransformer.setUseResourceTitleAsTitle(true);
  }

  @Test
  public void testRegisterService() {
    BundleContext mockBundleContext = mock(BundleContext.class);
    TikaInputTransformer tikaInputTransformer =
        new TikaInputTransformer(mockBundleContext, getMetacardType(COMMON_METACARDTYPE_NAME));
    verify(mockBundleContext)
        .registerService(
            eq(InputTransformer.class), eq(tikaInputTransformer), any(Dictionary.class));
  }

  @Test
  public void testContentExtractors() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPDF.pdf");
    tikaInputTransformer.addContentMetadataExtractor(serviceRefCme);
    Metacard metacard = tikaInputTransformer.transform(stream);
    verify(cme).process(anyString(), anyObject());
    verify(cme).getMetacardAttributes();
    assertThat(metacard.getMetacardType().getName(), is(PDF_METACARDTYPE_NAME));
    List<String> actualNames =
        metacard
            .getMetacardType()
            .getAttributeDescriptors()
            .stream()
            .map(AttributeDescriptor::getName)
            .collect(toList());
    assertThat(
        "Missing an attribute descriptor",
        actualNames,
        allOf(hasItem(equalTo("attr1")), hasItem(equalTo("attr2"))));
  }

  @Test
  public void testMetadataExtractor() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPDF.pdf");
    tikaInputTransformer.addMetadataExtractor(serviceRefMe);
    Metacard metacard = tikaInputTransformer.transform(stream);
    verify(me).canProcess(PDF_CONTENT_TYPE);
    verify(me).getMetacardType(PDF_CONTENT_TYPE);
    verify(me).process(anyString(), any(Metacard.class));
    assertThat("Wrong metacardname", metacard.getMetacardType().getName(), is(TEST_NAME));
  }

  @Test
  public void testRemoveExtractors() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPDF.pdf");
    tikaInputTransformer.addContentMetadataExtractor(serviceRefCme);
    tikaInputTransformer.addMetadataExtractor(serviceRefMe);
    tikaInputTransformer.removeContentMetadataExtractor(serviceRefCme);
    tikaInputTransformer.removeMetadataExtractor(serviceRefMe);
    tikaInputTransformer.transform(stream);
    verify(cme, never()).process(anyString(), anyObject());
    verify(me, never()).process(anyString(), anyObject());
    verify(cme, never()).getMetacardAttributes();
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullInputStream() throws Exception {
    transform(null);
  }

  @Test
  public void testJavaClass() throws Exception {
    InputStream stream =
        TikaInputTransformerTest.class
            .getClassLoader()
            .getResourceAsStream("CatalogFrameworkImpl.class");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("CatalogFrameworkImpl"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("DEFAULT_RESOURCE_NOT_FOUND_MESSAGE"));
    assertThat(metacard.getContentTypeName(), is("application/java-vm"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testAudioWav() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testWAV.wav");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(metacard.getMetadata(), containsString("16Int"));
    assertThat(metacard.getContentTypeName(), is("audio/vnd.wave"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testAudioAiff() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testAIFF.aif");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(metacard.getMetadata(), containsString("PCM_SIGNED"));
    assertThat(metacard.getContentTypeName(), is("audio/x-aiff"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testAudioAu() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testAU.au");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(metacard.getMetadata(), containsString("PCM_SIGNED"));
    assertThat(metacard.getContentTypeName(), is("audio/basic"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testAudioMidi() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testMID.mid");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(metacard.getMetadata(), containsString("PPQ"));
    assertThat(metacard.getContentTypeName(), is("audio/midi"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testJavaSource() throws Exception {
    InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("testpackage/testJAVA.java");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("HelloWorld"));
    assertThat(metacard.getContentTypeName(), containsString("text/plain"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testCppSource() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testCPP.cpp");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("Hello world example"));
    assertThat(metacard.getContentTypeName(), containsString("text/x-csrc"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testGroovySource() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testGROOVY.groovy");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("this is a comment"));
    assertThat(metacard.getContentTypeName(), containsString("text/plain"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testTiff() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testTIFF.tif");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"tiff:BitsPerSample\" content=\"8\" />"));
    assertThat(metacard.getContentTypeName(), is("image/tiff"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
  }

  @Test
  public void testBmp() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testBMP.bmp");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Compression CompressionTypeName\" content=\"BI_RGB\" />"));
    assertThat(metacard.getContentTypeName(), is("image/bmp"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
  }

  @Test
  public void testGif() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testGIF.gif");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Compression CompressionTypeName\" content=\"lzw\" />"));
    assertThat(metacard.getContentTypeName(), is("image/gif"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
  }

  @Test
  public void testGeoTaggedJpeg() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testJPEG_GEO.jpg");

    /*
     * The dates in testJPED_GEO.jpg do not contain timezones. If no timezone is specified,
     * the Tika input transformer assumes the local time zone.  Set the system timezone to UTC
     * so we can do assertions.
     */
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Model\" content=\"Canon EOS 40D\" />"));
    assertThat(metacard.getContentTypeName(), is("image/jpeg"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2009-08-11 09:09:45 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2009-10-02 23:02:49 UTC"));
    assertThat(
        metacard.getAttribute(Metacard.GEOGRAPHY).getValue(), is("POINT(-54.1234 12.54321)"));

    // Reset timezone back to local time zone.
    TimeZone.setDefault(defaultTimeZone);

    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
    assertThat(metacard.getContentTypeName(), is("image/jpeg"));
  }

  @Test
  public void testCommentedJpeg() throws Exception {
    InputStream stream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("testJPEG_commented.jpg");

    /*
     * The dates in testJPEG_commented.jpg do not contain timezones. If no timezone is specified,
     * the Tika input transformer assumes the local time zone.  Set the system timezone to UTC
     * so we can do assertions.
     */
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Tosteberga \u00C4ngar"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Keywords\" content=\"grazelands\" />"));
    assertThat(metacard.getContentTypeName(), is("image/jpeg"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2010-07-28 11:02:00 UTC"));

    // Reset timezone back to local time zone.
    TimeZone.setDefault(defaultTimeZone);
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
    assertThat(metacard.getContentTypeName(), is("image/jpeg"));
  }

  @Test
  public void testPng() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPNG.png");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Compression Lossless\" content=\"true\" />"));
    assertThat(metacard.getContentTypeName(), is("image/png"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(IMAGE));
  }

  @Test
  public void testMp3() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testMP3id3v1_v2.mp3");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Test Title"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"xmpDM:artist\" content=\"Test Artist\" />"));
    assertThat(metacard.getContentTypeName(), is("audio/mpeg"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testMp4() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testMP4.m4a");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Test Title"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2012-01-28 18:39:18 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2012-01-28 18:40:25 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"xmpDM:artist\" content=\"Test Artist\" />"));
    assertThat(metacard.getContentTypeName(), is("audio/mp4"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(SOUND));
  }

  @Test
  public void testPDF() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPDF.pdf");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Apache Tika - Apache Tika"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2007-09-15 09:02:31 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2007-09-15 09:02:31 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(), containsString("<meta name=\"xmpTPg:NPages\" content=\"1\" />"));
    assertThat(metacard.getContentTypeName(), is("application/pdf"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testXml() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testXML.xml");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Test Document"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2000-12-01 00:00:00 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("John Smith"));
    assertThat(metacard.getContentTypeName(), is("application/xml"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testTxt() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("test.txt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("119917165"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testWordDoc() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testWORD.docx");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Sample Word Document"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2008-12-11 16:04:00 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2010-11-12 16:21:00 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("This is a sample Microsoft Word Document."));
    assertThat(
        metacard.getContentTypeName(),
        is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testPpt() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPPT.ppt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Sample Powerpoint Slide"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2007-09-14 17:33:12 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2007-09-14 19:16:39 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("Created with Microsoft"));
    assertThat(metacard.getContentTypeName(), is("application/vnd.ms-powerpoint"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testPptx() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testPPT.pptx");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Attachment Test"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2010-05-04 06:43:54 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2010-06-29 06:34:35 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("content as every other file being tested for tika content parsing"));
    assertThat(
        metacard.getContentTypeName(),
        is("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testBadPpt() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testBadPPT.ppt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNull(metacard.getMetadata());
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is("Dataset"));
  }

  @Test
  public void testXls() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testEXCEL.xls");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Simple Excel document"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2007-10-01 16:13:56 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2007-10-01 16:31:43 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("Written and saved in Microsoft Excel X for Mac Service Release 1."));
    assertThat(metacard.getContentTypeName(), is("application/vnd.ms-excel"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testXlsx() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testEXCEL.xlsx");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Simple Excel document"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2007-10-01 16:13:56 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2008-12-11 16:02:17 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("Sample Excel Worksheet - Numbers and their Squares"));
    assertThat(
        metacard.getContentTypeName(),
        is("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testZip() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testZIP.zip");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), nullValue());
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Content-Type\" content=\"application/zip\" />"));
    assertThat(metacard.getContentTypeName(), is("application/zip"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(COLLECTION));
  }

  @Test
  public void testEmail() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testEmail.eml");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Welcome"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getMetadata(),
        containsString("<meta name=\"Content-Type\" content=\"message/rfc822\" />"));
    assertThat(metacard.getContentTypeName(), is("message/rfc822"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(DATASET));
  }

  @Test
  public void testOpenOffice() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testOpenOffice2.odt");

    /*
     * The dates in testOpenOffice2.odt do not contain timezones. If no timezone is specified,
     * the Tika input transformer assumes the local time zone.  Set the system timezone to UTC
     * so we can do assertions.
     */
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertThat(metacard.getTitle(), is("Test OpenOffice2 Document"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2007-09-14 11:06:08 UTC"));
    assertThat(convertDate(metacard.getModifiedDate()), is("2013-02-13 06:52:10 UTC"));
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        containsString("This is a sample Open Office document, written in NeoOffice 2.2.1"));
    assertThat(metacard.getContentTypeName(), is("application/vnd.oasis.opendocument.text"));

    // Reset timezone back to local time zone.
    TimeZone.setDefault(defaultTimeZone);
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testVisio() throws Exception {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("testVisio.vsdx");
    Metacard metacard = transform(stream);
    assertThat(convertDate(metacard.getModifiedDate()), is("2015-08-16 23:37:46 UTC"));
    assertThat(convertDate(metacard.getCreatedDate()), is("2015-08-16 23:13:05 UTC"));
    assertThat(metacard.getMetadata(), notNullValue());
    assertThat(
        metacard.getMetadata(),
        containsString(
            "<meta name=\"Content-Type\" content=\"application/vnd.ms-visio.drawing\" />"));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testTitleConfiguration() throws Exception {
    ByteArrayInputStream stream = new ByteArrayInputStream("".getBytes());
    Metacard metacard = transform(stream);
    assertThat(metacard, notNullValue());
    assertThat(metacard.getTitle(), nullValue());
  }

  @Test
  public void testMaxPreviewLength() throws Exception {
    this.tikaInputTransformer.setPreviewMaxLength(10);
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("test.txt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNotNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        not(containsString("119917165")));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testMaxMetadataLength() throws Exception {
    this.tikaInputTransformer.setMetadataMaxLength(1);
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("test.txt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Validation.VALIDATION_WARNINGS).getValue().toString(),
        equalTo(TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testMaxPreviewAndMetadataLength() throws Exception {
    this.tikaInputTransformer.setMetadataMaxLength(1);
    this.tikaInputTransformer.setPreviewMaxLength(10);
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("test.txt");
    Metacard metacard = transform(stream);
    assertNotNull(metacard);
    assertNull(metacard.getMetadata());
    assertThat(
        metacard.getAttribute(Validation.VALIDATION_WARNINGS).getValue().toString(),
        equalTo(TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG));
    assertThat(
        metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue().toString(),
        not(containsString("119917165")));
    assertThat(metacard.getAttribute(Core.DATATYPE).getValue(), is(TEXT));
  }

  @Test
  public void testMetadataExtractorReceivesXml() throws Exception {
    MetadataExtractor metadataExtractor = mock(MetadataExtractor.class);
    when(metadataExtractor.canProcess(any())).thenReturn(true);
    addMetadataExtractor(metadataExtractor);
    transform(new ByteArrayInputStream("something".getBytes()));

    ArgumentCaptor<String> metadataText = ArgumentCaptor.forClass(String.class);
    verify(metadataExtractor).process(metadataText.capture(), any());

    assertThat(
        metadataText.getValue(), containsString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
  }

  @Test
  public void testMetadataExtractorExtractedText() throws Exception {
    MetacardType metacardType = mock(MetacardType.class);
    MetadataExtractor metacardExtractor = mock(MetadataExtractor.class);
    when(metacardExtractor.canProcess(any())).thenReturn(true);
    when(metacardExtractor.getMetacardType(any())).thenReturn(metacardType);
    addMetadataExtractor(metacardExtractor);

    Metacard metacard = transform(new ByteArrayInputStream("something".getBytes()));
    assertThat(
        metacard.getMetacardType().getAttributeDescriptor(Extracted.EXTRACTED_TEXT),
        notNullValue());
    assertThat(metacard.getAttribute(Extracted.EXTRACTED_TEXT).getValue(), is("something\n"));
  }

  @Test
  public void testEmptyStringMetadataExtractor() throws Exception {
    MetadataExtractor metadataExtractor = mock(MetadataExtractor.class);
    when(metadataExtractor.canProcess(any())).thenReturn(true);
    addMetadataExtractor(metadataExtractor);
    tikaInputTransformer.setMetadataMaxLength(0);
    transform(new ByteArrayInputStream("something".getBytes()));
    verify(metadataExtractor, times(0)).process(any(), any());
  }

  private String convertDate(Date date) {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    LOGGER.debug(df.format(date));
    return df.format(date);
  }

  private Metacard transform(InputStream stream) throws Exception {
    return tikaInputTransformer.transform(stream);
  }

  private void addMetadataExtractor(MetadataExtractor metadataExtractor) {
    ServiceReference<MetadataExtractor> serviceReference = mock(ServiceReference.class);
    when(bundleCtx.getService(serviceReference)).thenReturn(metadataExtractor);
    tikaInputTransformer.addMetadataExtractor(serviceReference);
  }
}
