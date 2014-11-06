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
package ddf.mime.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.tika.TikaMimeTypeResolver;

public class MimeTypeMapperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeMapperTest.class);

    private static final String CSW_RECORD_FILE = "src/test/resources/csw_record.xml";
    
    private static final String XML_METACARD_FILE = "src/test/resources/sample_metacard.xml";
    
    private static final String NO_NAMESPACE_MATCHES_XML_FILE = "src/test/resources/no_namespace_matches.xml";
    
    private static final String NO_NAMESPACES_XML_FILE = "src/test/resources/no_namespaces.xml";
    
    
    @Test
    public void testNoResolvers() throws Exception {
        List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();

        MimeTypeMapper mapper = new MimeTypeMapperImpl(resolvers);
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertNull(fileExtension);
    }

    @Test
    public void testSingleResolver() throws Exception {
        List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();
        resolvers.add(new MockMimeTypeResolver("Resolver_1", 10));

        MimeTypeMapper mapper = new MimeTypeMapperImpl(resolvers);
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testMultipleResolvers() throws Exception {
        List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();
        resolvers.add(new MockMimeTypeResolver("Resolver_1", 10));
        resolvers.add(new MockMimeTypeResolver("Resolver_2", -1));
        resolvers.add(new MockMimeTypeResolver("Resolver_3", 100));

        MimeTypeMapper mapper = new MimeTypeMapperImpl(resolvers);
        String fileExtension = mapper.getFileExtensionForMimeType("image/nitf");
        LOGGER.debug("fileExtension = {}", fileExtension);
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testGuessMimeTypeForXmlIngest() throws Exception {
        
        List<MimeTypeResolver> resolvers = new ArrayList<MimeTypeResolver>();
        resolvers.add(new MockMimeTypeResolver("NitfResolver", 10, 
                new String[] {"nitf=image/nitf", "ntf=image/nitf"}, null));
        resolvers.add(new MockMimeTypeResolver("XmlMetacardResolver", 10, 
                new String[] {"xml=text/xml"}, "urn:catalog:metacard"));
        resolvers.add(new MockMimeTypeResolver("CswResolver", 10, 
                new String[] {"xml=text/xml;id=csw"}, "http://www.opengis.net/cat/csw/2.0.2"));
        TikaMimeTypeResolver tikaMimeTypeResolver = new TikaMimeTypeResolver();
        tikaMimeTypeResolver.setPriority(-1);
        resolvers.add(tikaMimeTypeResolver);

        MimeTypeMapper mapper = new MimeTypeMapperImpl(resolvers);
        
        InputStream is = FileUtils.openInputStream(new File(CSW_RECORD_FILE));
        String mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml;id=csw", mimeType);
        
        is = FileUtils.openInputStream(new File(XML_METACARD_FILE));
        mimeType = mapper.guessMimeType(is, "xml");
        LOGGER.debug("mimeType = {}", mimeType);
        assertEquals("text/xml", mimeType);
        
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

}
