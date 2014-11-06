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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeResolver;

/**
 * Implementation of the {@link MimeTypeMapper} interface that searches through all of the
 * registered {@link MimeTypeResolver}s to retieve file extension for a given mime type, and vice
 * versa. Once a file extension (or mime type) is resolved, this mapper stops searching through any
 * remaining {@link MimeTypeResolver}s and returns.
 * 
 * @since 2.1.0
 * 
 */
public class MimeTypeMapperImpl implements MimeTypeMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeMapperImpl.class);
    
    private static final String XML_FILE_EXTENSION = "xml";

    private static Comparator<MimeTypeResolver> COMPARATOR = new Comparator<MimeTypeResolver>() {
        public int compare(MimeTypeResolver o1, MimeTypeResolver o2) {
            return o1.getPriority() - o2.getPriority();
        }
    };

    /**
     * The {@link List} of {@link MimeTypeResolver}s configured for this mapper and will be searched
     * on mime type/file extension mapping requests.
     */
    protected List<MimeTypeResolver> mimeTypeResolvers;

    protected MimeTypeResolver mimeTypeResolver;

    /**
     * Constructs the MimeTypeMapper with a list of {@link MimeTypeResolver}s.
     * 
     * @param mimeTypeResolvers
     *            the {@link List} of {@link MimeTypeResolver}s
     */
    public MimeTypeMapperImpl(List<MimeTypeResolver> mimeTypeResolvers) {
        LOGGER.debug("INSIDE: MimeTypeMapperImpl constructor");
        this.mimeTypeResolvers = mimeTypeResolvers;
    }

    @Override
    public String getFileExtensionForMimeType(String mimeType) throws MimeTypeResolutionException {
        LOGGER.trace("ENTERING: getFileExtensionForMimeType()");

        String extension = null;

        LOGGER.debug("Looping through {} MimeTypeResolvers", mimeTypeResolvers.size());

        // Sort the mime type resolvers in descending order of priority. This should
        // insure custom mime type resolvers are called before the (default) Apache Tika
        // mime type resolver.
        List<MimeTypeResolver> sortedResolvers = sortResolvers(mimeTypeResolvers);

        // Loop through all of the configured MimeTypeResolvers. The order of their
        // invocation is determined by their OSGi service ranking. The default
        // TikaMimeTypeResolver should be called last, allowing any configured custom
        // mime type resolvers to be invoked first - this allows custom mime type
        // resolvers that may override mime types supported by Tika to be invoked first.
        // Once a file extension is find for the given mime type, exit the loop.
        for (MimeTypeResolver resolver : sortedResolvers) {
            LOGGER.debug("Calling MimeTypeResolver {}", resolver.getName());
            try {
                extension = resolver.getFileExtensionForMimeType(mimeType);
            } catch (Exception e) {
                LOGGER.warn("Error resolving file extension for mime type: {}", mimeType);
                throw new MimeTypeResolutionException(e);
            }

            if (StringUtils.isNotEmpty(extension)) {
                LOGGER.debug("extension [{}] retrieved from MimeTypeResolver:  {}", extension,
                        resolver.getName());
                break;
            }
        }

        LOGGER.debug("mimeType = {},   file extension = [{}]", mimeType, extension);

        LOGGER.trace("EXITING: getFileExtensionForMimeType()");

        return extension;
    }

    @Override
    public String getMimeTypeForFileExtension(String fileExtension)
        throws MimeTypeResolutionException {
        LOGGER.trace("ENTERING: getMimeTypeForFileExtension()");

        String mimeType = null;

        LOGGER.debug("Looping through {} MimeTypeResolvers", mimeTypeResolvers.size());

        // TODO: This is to force the TikaMimeTypeResolver to be called
        // after the CustomMimeTypeResolvers to prevent Tika default mapping
        // from being used when a CustomMimeTypeResolver may be more appropriate.
        List<MimeTypeResolver> sortedResolvers = sortResolvers(mimeTypeResolvers);

        // Loop through all of the configured MimeTypeResolvers. The order of their
        // invocation is determined by their OSGi service ranking. The default
        // TikaMimeTypeResolver should be called last, allowing any configured custom
        // mime type resolvers to be invoked first - this allows custom mime type
        // resolvers that may override mime types supported by Tika to be invoked first.
        // Once a file extension is find for the given mime type, exit the loop.
        for (MimeTypeResolver resolver : sortedResolvers) {
            LOGGER.debug("Calling MimeTypeResolver {}", resolver.getName());
            try {
                mimeType = resolver.getMimeTypeForFileExtension(fileExtension);
            } catch (Exception e) {
                LOGGER.warn("Error resolving mime type for file extension: " + fileExtension);
                throw new MimeTypeResolutionException(e);
            }

            if (StringUtils.isNotEmpty(mimeType)) {
                LOGGER.debug("mimeType [{}] retrieved from MimeTypeResolver:  ", mimeType,
                        resolver.getName());
                break;
            }
        }

        LOGGER.debug("mimeType = {},   file extension = [{}]", mimeType, fileExtension);

        LOGGER.trace("EXITING: getMimeTypeForFileExtension()");

        return mimeType;
    }

    @Override
    public String guessMimeType(InputStream is, String fileExtension)
        throws MimeTypeResolutionException {
        LOGGER.trace("ENTERING: guessMimeType()");

        String mimeType = null;

        LOGGER.debug("Looping through{} MimeTypeResolvers", mimeTypeResolvers.size());

        // This is to force the TikaMimeTypeResolver to be called
        // after the CustomMimeTypeResolvers to prevent Tika default mapping
        // from being used when a CustomMimeTypeResolver may be more appropriate.
        List<MimeTypeResolver> sortedResolvers = sortResolvers(mimeTypeResolvers);
        
        // If file has XML extension, then read root element namespace once so
        // each MimeTypeResolver does not have to open the stream and read the namespace
        String namespace = null;
        if (fileExtension.equals(XML_FILE_EXTENSION)) {
            namespace = getRootElementNamespace(is);
            LOGGER.debug("namespace = {}", namespace);
        }

        // Loop through all of the configured MimeTypeResolvers. The order of their
        // invocation is determined by their OSGi service ranking. The default
        // TikaMimeTypeResolver should be called last, allowing any configured custom
        // mime type resolvers to be invoked first - this allows custom mime type
        // resolvers that may override mime types supported by Tika to be invoked first.
        // Once a file extension is find for the given mime type, exit the loop.
        for (MimeTypeResolver resolver : sortedResolvers) {
            LOGGER.debug("Calling MimeTypeResolver {}", resolver.getName());
            try {
                // If processing an XML file, then match the namespace extracted from the
                // XML file to the MimeTypeResolver that supports that schema (namespace).
                // If no MimeTypeResolvers support the namespace, then defer to the Tika
                // MimeTypeResolver to process the XML file.
                if (fileExtension.equals(XML_FILE_EXTENSION)) {
                    if (namespace != null && resolver.hasSchema()) {
                        if (namespace.equals(resolver.getSchema())) {
                            mimeType = resolver.getMimeTypeForFileExtension(fileExtension);
                        }
                    }
                } else {
                    mimeType = resolver.getMimeTypeForFileExtension(fileExtension);
                }
            } catch (Exception e) {
                LOGGER.warn("Error resolving mime type for file extension: " + fileExtension);
                throw new MimeTypeResolutionException(e);
            }

            if (StringUtils.isNotEmpty(mimeType)) {
                LOGGER.debug("mimeType [{}] retrieved from MimeTypeResolver:  ", mimeType,
                        resolver.getName());
                break;
            }
        }

        LOGGER.debug("mimeType = {},   file extension = [{}]", mimeType, fileExtension);

        LOGGER.trace("EXITING: guessMimeType()");

        return mimeType;
    }
    
    private String getRootElementNamespace(InputStream is) {
        LOGGER.trace("ENTERING: getRootElementNamespace()");
        
        if (is == null) {
            return null;
        }
        
        String namespace = null;        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();        
            Document document = db.parse(is);
            Node node = document.getDocumentElement();
            namespace = node.getNamespaceURI();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.debug("Unable to get root element namespace");
        }
        
        LOGGER.trace("ENXITING: getRootElementNamespace() - namespace = {}", namespace);
        
        return namespace;
    }


    /**
     * Sort the list of {@link MimeTypeResolver}s by their descending priority, i.e., the lower the
     * priority the later the {@link MimeTypeResolver} is invoked.
     * 
     * @param resolvers
     *            the {@link List} of {@link MimeTypeResolver}s
     * @return the sorted list of {@link MimeTypeResolver}s by descending priority
     */
    private List<MimeTypeResolver> sortResolvers(List<MimeTypeResolver> resolvers) {
        LOGGER.debug("ENTERING: sortResolvers()");

        List<MimeTypeResolver> sortedResolvers = null;

        if (resolvers != null) {
            // Log sorted list of PreIngestServices for debugging
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unsorted services");
                LOGGER.debug("------------------");

                for (MimeTypeResolver resolver : resolvers) {
                    LOGGER.debug("{}   (priority: {})", resolver.getName(), resolver.getPriority());
                }
            }

            // Make copy of input services list because OSGi/Blueprint marks this input list as
            // read-only
            sortedResolvers = new ArrayList<MimeTypeResolver>(resolvers);

            // Inner class Comparator for comparing/sorting
            Comparator<MimeTypeResolver> comparator = new Comparator<MimeTypeResolver>() {
                @Override
                public int compare(MimeTypeResolver arg0, MimeTypeResolver arg1) {
                    LOGGER.debug("INSIDE: Comparator");
                    return (arg0.getPriority() - arg1.getPriority());
                }
            };

            if (sortedResolvers.size() > 1) {
                LOGGER.debug("Sorting resolvers");
                Collections.sort(sortedResolvers, comparator);
                Collections.reverse(sortedResolvers);
            }

            // Log sorted list of PreIngestServices for debugging
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sorted/prioritized services");
                LOGGER.debug("---------------------------");

                for (MimeTypeResolver resolver : sortedResolvers) {
                    LOGGER.debug("{}   (priority: {})", resolver.getName(), resolver.getPriority());
                }
            }
        }

        LOGGER.debug("EXITING: sortResolvers()");

        return sortedResolvers;
    }
}
