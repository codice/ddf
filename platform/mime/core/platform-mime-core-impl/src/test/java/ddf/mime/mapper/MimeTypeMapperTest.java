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
package ddf.mime.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.tika.TikaMimeTypeResolver;

public class MimeTypeMapperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeMapperTest.class);

    private static final String CSW_RECORD_FILE = "src/test/resources/csw_record.xml";

    private static final String CSW_RECORD_FILE_NO_EXTENSION =
            "src/test/resources/csw_record_no_extension";

    private static final String XML_METACARD_FILE = "src/test/resources/metacard_sample.xml";

    private static final String XML_METACARD_FILE_NO_EXTENSION =
            "src/test/resources/metacard_sample_no_extension";

    private static final String NO_NAMESPACE_MATCHES_XML_FILE =
            "src/test/resources/no_namespace_matches.xml";

    private static final String NO_NAMESPACES_XML_FILE = "src/test/resources/no_namespaces.xml";

    private static final String NITF_FILE = "src/test/resources/sampleNitf.nitf";

    private static final TikaMimeTypeResolver TIKA_MIME_TYPE_RESOLVER;

    static {
        TIKA_MIME_TYPE_RESOLVER = new TikaMimeTypeResolver();
        TIKA_MIME_TYPE_RESOLVER.setPriority(-1);
    }

    private static final List<MimeTypeResolver> MOCK_MIME_TYPE_RESOLVERS =
            ImmutableList.of(new MockMimeTypeResolver("NitfResolver",
                            10,
                            new String[] {"nitf=image/nitf", "ntf=image/nitf"},
                            null),
                    new MockMimeTypeResolver("XmlMetacardResolver",
                            10,
                            new String[] {"xml=text/xml"},
                            "urn:catalog:metacard"),
                    new MockMimeTypeResolver("CswResolver",
                            5,
                            new String[] {"xml=text/xml;id=csw"},
                            "http://www.opengis.net/cat/csw/2.0.2"),
                    TIKA_MIME_TYPE_RESOLVER);

    @Test
    public void testNoResolvers() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(Collections.emptyList());
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertNull(fileExtension);
    }

    @Test
    public void testSingleResolver() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(MOCK_MIME_TYPE_RESOLVERS);
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testMultipleResolvers() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(MOCK_MIME_TYPE_RESOLVERS);
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testGuessMimeTypeForXmlIngest() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(MOCK_MIME_TYPE_RESOLVERS);

        InputStream is = FileUtils.openInputStream(new File(CSW_RECORD_FILE));
        String mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml;id=csw", mimeType);

        is = FileUtils.openInputStream(new File(XML_METACARD_FILE));
        mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml", mimeType);

        is = FileUtils.openInputStream(new File(NITF_FILE));
        mimeType = mapper.guessMimeType(is, "nitf");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("image/nitf", mimeType);

        // Verify an XML file with a root element namespace, e.g., a pom.xml file, that does not match any
        // MimeTypeResolver returns a null mime type
        is = FileUtils.openInputStream(new File(NO_NAMESPACE_MATCHES_XML_FILE));
        mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertNull(mimeType);

        // Verify an XML file with no namespaces that does not match any
        // MimeTypeResolver returns a null mime type
        is = FileUtils.openInputStream(new File(NO_NAMESPACES_XML_FILE));
        mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertNull(mimeType);
    }

    @Test
    public void testGuessMimeTypeForXmlIngestNoExtension() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(MOCK_MIME_TYPE_RESOLVERS);

        InputStream is = FileUtils.openInputStream(new File(CSW_RECORD_FILE_NO_EXTENSION));
        String mimeType = mapper.guessMimeType(is, "");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml;id=csw", mimeType);

        is = FileUtils.openInputStream(new File(XML_METACARD_FILE_NO_EXTENSION));
        mimeType = mapper.guessMimeType(is, "");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml", mimeType);
    }

    @Test
    public void testGuessMimeTypeForFileExtension() throws Exception {
        MimeTypeMapper mapper = new MimeTypeMapperImpl(MOCK_MIME_TYPE_RESOLVERS);

        String mimeType = mapper.getMimeTypeForFileExtension("xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml", mimeType);

        mimeType = mapper.getMimeTypeForFileExtension(".nitf");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("image/nitf", mimeType);
    }

}
