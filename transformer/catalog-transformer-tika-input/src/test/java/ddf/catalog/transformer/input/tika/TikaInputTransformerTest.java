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
        assertThat(metacard.getContentTypeName(), is("text/plain; charset=ISO-8859-1"));
    }

    @Test
    public void testCppSource() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testCPP.cpp");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("Hello world example"));
        assertThat(metacard.getContentTypeName(), is("text/plain; charset=ISO-8859-1"));
    }

    @Test
    public void testGroovySource() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testGROOVY.groovy");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(), containsString("this is a comment"));
        assertThat(metacard.getContentTypeName(), is("text/plain; charset=windows-1252"));
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
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Model\" content=\"Canon EOS 40D\" />"));
        assertThat(metacard.getContentTypeName(), is("image/jpeg"));
        assertThat(metacard.getCreatedDate().toString(), is("Tue Aug 11 09:09:45 MST 2009"));
        assertThat(metacard.getModifiedDate().toString(), is("Fri Oct 02 23:02:49 MST 2009"));
        assertThat((String) metacard.getAttribute(Metacard.GEOGRAPHY).getValue(),
                is("POINT(-54.1234 12.54321)"));
    }

    @Test
    public void testCommentedJpeg() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testJPEG_commented.jpg");
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Tosteberga \u00C4ngar"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("<meta name=\"Keywords\" content=\"bird watching\" />"));
        assertThat(metacard.getContentTypeName(), is("image/jpeg"));
        assertThat(metacard.getCreatedDate().toString(), is("Wed Jul 28 11:02:00 MST 2010"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Sat Jan 28 11:39:18 MST 2012"));
        assertThat(metacard.getModifiedDate().toString(), is("Sat Jan 28 11:40:25 MST 2012"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Sat Sep 15 02:02:31 MST 2007"));
        assertThat(metacard.getModifiedDate().toString(), is("Sat Sep 15 02:02:31 MST 2007"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Thu Nov 30 17:00:00 MST 2000"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Thu Dec 11 09:04:00 MST 2008"));
        assertThat(metacard.getModifiedDate().toString(), is("Fri Nov 12 09:21:00 MST 2010"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Fri Sep 14 10:33:12 MST 2007"));
        assertThat(metacard.getModifiedDate().toString(), is("Fri Sep 14 12:16:39 MST 2007"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Mon May 03 23:43:54 MST 2010"));
        assertThat(metacard.getModifiedDate().toString(), is("Mon Jun 28 23:34:35 MST 2010"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Mon Oct 01 09:13:56 MST 2007"));
        assertThat(metacard.getModifiedDate().toString(), is("Mon Oct 01 09:31:43 MST 2007"));
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
        assertThat(metacard.getCreatedDate().toString(), is("Mon Oct 01 09:13:56 MST 2007"));
        assertThat(metacard.getModifiedDate().toString(), is("Thu Dec 11 09:02:17 MST 2008"));
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
        Metacard metacard = transform(stream);
        assertNotNull(metacard);
        assertThat(metacard.getTitle(), is("Test OpenOffice2 Document"));
        assertThat(metacard.getCreatedDate().toString(), is("Fri Sep 14 11:06:08 MST 2007"));
        assertThat(metacard.getModifiedDate().toString(), is("Wed Feb 13 06:52:10 MST 2013"));
        assertNotNull(metacard.getMetadata());
        assertThat(metacard.getMetadata(),
                containsString("This is a sample Open Office document, written in NeoOffice 2.2.1"));
        assertThat(metacard.getContentTypeName(), is("application/vnd.oasis.opendocument.text"));
    }

    private Metacard transform(InputStream stream) throws Exception {
        TikaInputTransformer tikaInputTransformer = new TikaInputTransformer();
        Metacard metacard = tikaInputTransformer.transform(stream);
        return metacard;
    }
}
