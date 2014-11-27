/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.transformer.input.tika;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;

public class TikaInputTransformerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikaInputTransformerTest.class);

    @Test(expected = CatalogTransformerException.class)
    public void testNullInputStream() throws Exception {
        Metacard metacard = transform(null);
    }

    @Test
    public void testJavaClass() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("CatalogFrameworkImpl.class");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("CatalogFrameworkImpl"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("DEFAULT_RESOURCE_NOT_FOUND_MESSAGE"));
        assertThat(metacard.getContentTypeName(), is("application/java-vm"));
    }

    @Test
    public void testAudioWav() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testWAV.wav");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("16Int"));
        assertThat(metacard.getContentTypeName(), is("audio/x-wav"));
    }

    @Test
    public void testAudioAiff() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testAIFF.aif");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("PCM_SIGNED"));
        assertThat(metacard.getContentTypeName(), is("audio/x-aiff"));
    }

    @Test
    public void testAudioAu() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testAU.au");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("PCM_SIGNED"));
        assertThat(metacard.getContentTypeName(), is("audio/basic"));
    }

    @Test
    public void testAudioMidi() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testMID.mid");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("PPQ"));
        assertThat(metacard.getContentTypeName(), is("audio/midi"));
    }

    @Test
    public void testJavaSource() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testpackage/testJAVA.java");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("HelloWorld"));
        assertThat(metacard.getContentTypeName(), containsString("text/plain"));
    }

    @Test
    public void testCppSource() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testCPP.cpp");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("Hello world example"));
        assertThat(metacard.getContentTypeName(), containsString("text/plain"));
    }

    @Test
    public void testGroovySource() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testGROOVY.groovy");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("this is a comment"));
        assertThat(metacard.getContentTypeName(), containsString("text/plain"));
    }

    @Test
    public void testTiff() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testTIFF.tif");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"tiff:BitsPerSample\" content=\"8\" />"));
        assertThat(metacard.getContentTypeName(), is("image/tiff"));
    }

    @Test
    public void testBmp() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testBMP.bmp");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(
                metacard.getMetadata(),
                containsString("<meta name=\"Compression CompressionTypeName\" content=\"BI_RGB\" />"));
        assertThat(metacard.getContentTypeName(), is("image/x-ms-bmp"));
    }

    @Test
    public void testGif() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testGIF.gif");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Compression CompressionTypeName\" content=\"lzw\" />"));
        assertThat(metacard.getContentTypeName(), is("image/gif"));
    }

    @Test
    public void testGeoTaggedJpeg() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testJPEG_GEO.jpg");
        
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
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Model\" content=\"Canon EOS 40D\" />"));
        assertThat(metacard.getContentTypeName(), is("image/jpeg"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2009-08-11 09:09:45 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2009-10-02 23:02:49 UTC"));
        assertThat((String) metacard.getAttribute(Metacard.GEOGRAPHY).getValue(),
                is("POINT(-54.1234 12.54321)"));
        
        // Reset timezone back to local time zone.
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testCommentedJpeg() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
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
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Keywords\" content=\"bird watching\" />"));
        assertThat(metacard.getContentTypeName(), is("image/jpeg"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2010-07-28 11:02:00 UTC"));
        
        // Reset timezone back to local time zone.
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testPng() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testPNG.png");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Compression Lossless\" content=\"true\" />"));
        assertThat(metacard.getContentTypeName(), is("image/png"));
    }

    @Test
    public void testMp3() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testMP3id3v1_v2.mp3");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Test Title"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"xmpDM:artist\" content=\"Test Artist\" />"));
        assertThat(metacard.getContentTypeName(), is("audio/mpeg"));
    }

    @Test
    public void testMp4() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testMP4.m4a");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Test Title"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2012-01-28 18:39:18 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2012-01-28 18:40:25 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"xmpDM:artist\" content=\"Test Artist\" />"));
        assertThat(metacard.getContentTypeName(), is("audio/mp4"));
    }

    @Test
    public void testPDF() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testPDF.pdf");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Apache Tika - Apache Tika"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2007-09-15 09:02:31 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2007-09-15 09:02:31 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"xmpTPg:NPages\" content=\"1\" />"));
        assertThat(metacard.getContentTypeName(), is("application/pdf"));
    }

    @Test
    public void testXml() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testXML.xml");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Test Document"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2000-12-01 00:00:00 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("John Smith"));
        assertThat(metacard.getContentTypeName(), is("application/xml"));
    }

    @Test
    public void testWordDoc() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testWORD.docx");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Sample Word Document"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2008-12-11 16:04:00 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2010-11-12 16:21:00 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<p>This is a sample Microsoft Word Document.</p>"));
        assertThat(metacard.getContentTypeName(),
                is("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    public void testPpt() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testPPT.ppt");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Sample Powerpoint Slide"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2007-09-14 17:33:12 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2007-09-14 19:16:39 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("Created with Microsoft"));
        assertThat(metacard.getContentTypeName(), is("application/vnd.ms-powerpoint"));
    }

    @Test
    public void testPptx() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testPPT.pptx");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Attachment Test"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2010-05-04 06:43:54 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2010-06-29 06:34:35 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("content as every other file being tested for tika content parsing"));
        assertThat(metacard.getContentTypeName(),
                is("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    }

    @Test
    public void testXls() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testEXCEL.xls");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Simple Excel document"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2007-10-01 16:13:56 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2007-10-01 16:31:43 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("Written and saved in Microsoft Excel X for Mac Service Release 1."));
        assertThat(metacard.getContentTypeName(), is("application/vnd.ms-excel"));
    }

    @Test
    public void testXlsx() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testEXCEL.xlsx");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Simple Excel document"));
        assertThat(convertDate(metacard.getCreatedDate()), is("2007-10-01 16:13:56 UTC"));
        assertThat(convertDate(metacard.getModifiedDate()), is("2008-12-11 16:02:17 UTC"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("Sample Excel Worksheet - Numbers and their Squares"));
        assertThat(metacard.getContentTypeName(),
                is("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    public void testOpenOffice() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testOpenOffice2.odt");

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
        assertThat(metacard.getMetadata(),
                containsString("This is a sample Open Office document, written in NeoOffice 2.2.1"));
        assertThat(metacard.getContentTypeName(), is("application/vnd.oasis.opendocument.text"));

        // Reset timezone back to local time zone.
        TimeZone.setDefault(defaultTimeZone);
    }
    
    private String convertDate(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        LOGGER.debug(df.format(date));
        return df.format(date);
    }

    private Metacard transform(InputStream stream) throws Exception {
        TikaInputTransformer tikaInputTransformer = new TikaInputTransformer(null);
        Metacard metacard = tikaInputTransformer.transform(stream);
        return metacard;
    }
}
