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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;

public class MimeTypeMapperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeMapperTest.class);

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

}
